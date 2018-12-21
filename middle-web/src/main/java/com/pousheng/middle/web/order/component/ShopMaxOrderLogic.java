package com.pousheng.middle.web.order.component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.constants.CacheConsts;
import com.pousheng.middle.constants.DateConsts;
import com.pousheng.middle.open.stock.InventoryPusherClient;
import com.pousheng.middle.open.stock.StockPusherLogic;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.shop.dto.ShopBusinessTime;
import com.pousheng.middle.shop.dto.ShopExtraInfo;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.companent.WarehouseClient;
import com.pousheng.middle.warehouse.dto.AvailableInventoryDTO;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.mq.warehouse.InventoryChangeProducer;
import com.pousheng.middle.web.mq.warehouse.model.InventoryChangeDTO;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.common.utils.Arguments;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 店铺最大接单量逻辑
 *
 * @author tanlongjun
 */
@Slf4j
@Component
public class ShopMaxOrderLogic {

    @Autowired
    private ShopCacher shopCacher;

    @RpcConsumer
    private OrderShipmentReadService orderShipmentReadService;

    @Autowired
    private WarehouseClient warehouseClient;
    @Autowired
    private OpenShopCacher openShopCacher;

    @Autowired
    private JedisTemplate jedisTemplate;

    @Autowired
    private InventoryClient inventoryClient;

    @Autowired
    private InventoryPusherClient inventoryPusherClient;

    @Autowired
    private StockPusherLogic stockPusherLogic;

    @Autowired
    private WarehouseCacher warehouseCacher;

    @Autowired
    private MiddleShopCacher middleShopCacher;

    @Autowired
    private InventoryChangeProducer inventoryChangeProducer;

    /**
     * 验证店铺的最大接单量
     *
     * @param shipment
     */
    public void checkMaxOrderAcceptQty(Shipment shipment) {
        log.info("START-TO-CHECK-MAX-ORDER-ACCEPT-QTY.param:{}", shipment.toString());

        try {
            // 判断仓发还是店发 ship_way :1 店发 需要去获取店铺关联的仓库ID
            if (!Objects.equals(shipment.getShipWay(), 1)) {
                log.warn("skip ship way not equals 1.shipWay:{},shipment:{}", shipment.getShipWay(),
                    shipment.toString());
                return;
            }
            //查询店铺信息
            OpenShop openShop = openShopCacher.findById(shipment.getShipId());
            if (Objects.isNull(openShop)) {
                log.error("open shop not found.openShopId:{}", shipment.getShipId());
                return;
            }
            String appKey = openShop.getAppKey();
            String outCode = appKey.substring(4);
            String bizId = appKey.substring(0, 3);
            //查询仓库
            WarehouseDTO warehouseDTO = warehouseCacher.findByOutCodeAndBizId(outCode, bizId);
            if (Objects.isNull(warehouseDTO)) {
                log.error("failed to find warehouse by outCode {},bizId {}", outCode, bizId);
                return;
            }
            // 店铺关联的仓ID
            Long warehouseId = warehouseDTO.getId();
            if (isOverMaxOrderAcceptQty(warehouseId)) {
                log.warn("shop over max order accept.shopId:{}.warehouseId:{}", openShop.getId(), warehouseId);
                return;
            }
            Shop shop = middleShopCacher.findByOuterIdAndBusinessId(outCode, Long.valueOf(bizId));
            if (Objects.isNull(shop)) {
                log.warn("shop.not.found,outCode:{},bizId:{}", outCode, bizId);
                return;
            }
            ShopExtraInfo shopExtraInfo = ShopExtraInfo.fromJson(shop.getExtra());
            if (Arguments.isNull(shopExtraInfo)) {
                log.error("not find shop(id:{}) extra info by shop extra info json:{} ", shop.getId(), shop.getExtra());
                return;
            }
            ShopBusinessTime shopBusinessTime = shopExtraInfo.getShopBusinessTime();

            //未配置店铺最大接单量 直接返回
            if (Arguments.isNull(shopBusinessTime)
                || Arguments.isNull(shopBusinessTime.getOrderAcceptQtyMax())) {
                log.warn("orderAcceptQtyMax not config.skip check.shopId:{}", shop.getId());
                return;
            }
            Response<Integer> countResp = orderShipmentReadService.countByShopId(openShop.getId());
            if (!countResp.isSuccess()) {
                log.error("failed to query shipment count of shop.shopId:{}", shopExtraInfo.getOpenShopId());
                return;
            }
            log.info("ship way by shop outCode {}, bizId {}", outCode, bizId);

            String companyCode = String.valueOf(shopExtraInfo.getCompanyId());

            //达到接单上限
            if (shopBusinessTime.getOrderAcceptQtyMax().compareTo(countResp.getResult()) <= 0) {
                String key = getMaxOrderKey(warehouseId);

                String result = jedisTemplate.execute(jedis -> {
                    return jedis.get(key);
                });
                //若key不存在 则保存当天的标志位 并触发库存同步
                if (StringUtils.isBlank(result)
                    || CacheConsts.NIL.equals(result)) {
                    asyncPushStock(companyCode, warehouseId, warehouseDTO.getWarehouseCode());
                    jedisTemplate.execute(jedis -> {
                        jedis.setex(key, CacheConsts.ExpireSecond.ONE_DAY,
                            shopBusinessTime.getOrderAcceptQtyMax().toString());
                    });
                }
            }
            log.info("END-TO-CHECK-MAX-ORDER-ACCEPT-QTY.param:{}", shipment.toString());
        } catch (Exception e) {
            log.error("failed to check max order accept qty.param:{}", shipment.toString(), e);
        }

    }

    /**
     * 获取最大接单量key
     *
     * @param warehouseId
     * @return
     */
    public String getMaxOrderKey(Long warehouseId) {
        DateTime today = DateTime.now();
        String date = today.toString(DateTimeFormat.forPattern(DateConsts.YYYYMMDD_PATTERN));
        return MessageFormat.format(CacheConsts.SHOP_MAX_ORDER_LIMIT_PATTERN, warehouseId.toString(), date);
    }

    /**
     * 查询店仓最大接单量缓存值
     *
     * @param warehouseId
     * @return
     */
    public String queryMaxOrder(Long warehouseId) {
        String key = getMaxOrderKey(warehouseId);

        String result = jedisTemplate.execute(jedis -> {
            return jedis.get(key);
        });
        return result;
    }

    /**
     * 异步推送库存
     */
    public void asyncPushStock(String companyCode, Long warehouseId, String warehouseCode) {
        log.debug("start to push stock.warehouseId:{},warehouseCode:{}", warehouseId, warehouseCode);
        CompletableFuture.runAsync(() -> {
            //查询店铺的所有sku
            Response<List<AvailableInventoryDTO>> response = inventoryClient.queryAvailableRealQtyByWarehouse(
                warehouseCode);
            if (!response.isSuccess()) {
                log.error("failed to query all skuCode info of warehouse.warehouseCode:{}", warehouseCode);
                return;
            }
            if (CollectionUtils.isEmpty(response.getResult())) {
                log.warn("no skuCode info of warehouse.warehouseCode:{}", warehouseCode);
                return;
            }
            //过滤实际库存大于0的sku;
            List<String> skuCodeList = Lists.newArrayList();
            response.getResult().forEach(availableDTO -> {
                if (availableDTO.getRealAvailableQuantity() > 0) {
                    skuCodeList.add(availableDTO.getSkuCode());
                }
            });
            if (CollectionUtils.isEmpty(skuCodeList)) {
                log.warn("no available skuCode list of warehouse.warehouseCode:{}", warehouseCode);
                return;
            }
            //筛选存在仓库商品分组的sku
            List<String> skuCodes = stockPusherLogic.filterSkuListInWarehouseItemGroup(companyCode, skuCodeList,
                warehouseId);
            if (CollectionUtils.isEmpty(skuCodes)) {
                log.warn("no skuCodes need to push stock.skip to push.warehouseCode:{}", warehouseCode);
                return;
            }
            final List<InventoryChangeDTO> inventoryChangeDTOList = Lists.newArrayListWithExpectedSize(skuCodes.size());
            skuCodes.forEach(skuCode -> {
                inventoryChangeDTOList.add(InventoryChangeDTO.builder()
                    .skuCode(skuCode)
                    .warehouseId(warehouseId)
                    .build());
            });
            inventoryChangeProducer.handleInventoryChange(inventoryChangeDTOList);
        });
    }

    /**
     * 判断是否达到当天最大接单量
     *
     * @param warehouseId
     * @return
     */
    protected boolean isOverMaxOrderAcceptQty(Long warehouseId) {
        String result = queryMaxOrder(warehouseId);
        if (StringUtils.isNotBlank(result)
            && !CacheConsts.NIL.equals(result)) {
            return true;
        }
        return false;
    }

    /**
     * 过滤超过最大接单量的店仓
     *
     * @param warehouseIds
     * @return
     */
    public List<Long> filterWarehouse(List<Long> warehouseIds) {
        if (CollectionUtils.isEmpty(warehouseIds)) {
            return new ArrayList<>();
        }
        List<Long> result = Lists.newArrayListWithExpectedSize(warehouseIds.size());
        // 因推送库存的时候warehouseId可能很多 故此处用pipeline 批量处理 减少网络开销
        boolean broken = false;
        Jedis jedis = null;
        try {
            //批量请求redis
            jedis = jedisTemplate.getJedisPool().getResource();
            HashMap<Long, redis.clients.jedis.Response<String>> requestMap = Maps.newHashMapWithExpectedSize(
                warehouseIds.size());
            Pipeline pipeline = jedis.pipelined();
            warehouseIds.forEach(id -> {
                String key = getMaxOrderKey(id);
                requestMap.put(id, pipeline.get(key));
            });
            pipeline.sync();
            // 处理结果
            requestMap.forEach((k, v) -> {
                String val = v.get();
                if (StringUtils.isNotBlank(val)
                    && !CacheConsts.NIL.equals(val)) {
                    result.add(k);
                }
            });
        } catch (JedisConnectionException jce) {
            log.error("Redis connection lost.", jce);
            broken = true;
            throw jce;
        } finally {
            closeJedisResource(jedis, broken);
        }
        return result;

    }

    /**
     * 修改最大接单量的redis标志位
     */
    public void changeMaxOrderAcceptQtyFlag(Integer orderAcceptQtyMax, Shop shop) {
        try {
            WarehouseDTO warehouse = warehouseCacher.findByOutCodeAndBizId(shop.getOuterId(),
                String.valueOf(shop.getBusinessId()));
            if (Objects.isNull(warehouse)) {
                log.warn("could not find warehouse.skip.shop outId:{},businessId:{}", shop.getOuterId(),
                    shop.getBusinessId());
                return;
            }

            ShopExtraInfo shopExtraInfo = ShopExtraInfo.fromJson(shop.getExtra());
            String companyCode = String.valueOf(shopExtraInfo.getCompanyId());

            String key = getMaxOrderKey(warehouse.getId());
            String result = jedisTemplate.execute(jedis -> {
                return jedis.get(key);
            });

            //若标志位不存在
            if (StringUtils.isBlank(result)
                || CacheConsts.NIL.equals(result)) {
                if (Objects.isNull(orderAcceptQtyMax)) {
                    log.warn("shop max order accept is unlimit.shopId:{} ", shop.getId());
                    return;
                }
                Response<Integer> countResp = orderShipmentReadService.countByShopId(shopExtraInfo.getOpenShopId());
                if (!countResp.isSuccess()) {
                    log.error("failed to query shipment count of shop.shopId:{}", shopExtraInfo.getOpenShopId());
                    return;
                }
                //若当前设置的最大接单量值比当天已经接单量小
                if (orderAcceptQtyMax.compareTo(countResp.getResult()) <= 0) {
                    // 触发同步
                    // 店铺关联的仓ID
                    Long warehouseId = warehouse.getId();
                    asyncPushStock(companyCode, warehouseId, warehouse.getWarehouseCode());
                    addRedisMaxOrderFlag(key, orderAcceptQtyMax);
                } else {
                    log.warn("shop do not over max order accept qty.skip.shop info:{}", shop.toString());
                }
                return;
            }

            //若存在 则对比标志位中的阀值
            Integer flag = Integer.valueOf(result);
            //若相等 则忽略不处理
            if (!Objects.isNull(orderAcceptQtyMax)
                && orderAcceptQtyMax.compareTo(flag) == 0) {
                log.warn("order max accept qty not changed.skip.shop info:{}", shop.toString());
                return;
            }

            Response<Integer> countResp = orderShipmentReadService.countByShopId(shopExtraInfo.getOpenShopId());
            if (!countResp.isSuccess()) {
                log.error("max order flag exist.failed to query shipment count of shop.shopId:{}", shopExtraInfo.getOpenShopId());
                return;
            }
            //若设置的值大于实际接单量则重新触发库存同步
            //由于redis标志位存在的情况下先改小 再改成比实际接单量C小的值会删除标志位 影响业务。
            // 故此处由跟redis值比较改成实际接单量比较
            // orderAcceptQtyMax由已达到上限改成空(不限制) 同样需要触发同步
            if (Objects.isNull(orderAcceptQtyMax)
                || orderAcceptQtyMax.compareTo(countResp.getResult()) > 0) {
                //删除key
                jedisTemplate.execute(jedis -> {
                    jedis.del(key);
                });
                // 触发同步
                // 店铺关联的仓ID
                Long warehouseId = warehouse.getId();
                asyncPushStock(companyCode, warehouseId, warehouse.getWarehouseCode());
            } else {
                addRedisMaxOrderFlag(key, orderAcceptQtyMax);
            }
        } catch (Exception e) {
            log.error("failed to change max order accept qty flag.shopId:{}", shop.getId(), e);
        }
    }

    /**
     * 增加redis 最大接单量标志位
     *
     * @param key
     * @param orderAcceptQtyMax
     */
    protected void addRedisMaxOrderFlag(String key, Integer orderAcceptQtyMax) {
        jedisTemplate.execute(jedis -> {
            jedis.setex(key, CacheConsts.ExpireSecond.ONE_DAY, String.valueOf(orderAcceptQtyMax));
        });
    }


    protected void closeJedisResource(Jedis jedis, boolean connectionBroken) {
        if (jedis != null) {
            if (connectionBroken) {
                jedisTemplate.getJedisPool().returnBrokenResource(jedis);
            } else {
                jedisTemplate.getJedisPool().returnResource(jedis);
            }
        }

    }

}
