package com.pousheng.middle.open.manager;

import com.google.common.base.Optional;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.constants.SymbolConsts;
import com.pousheng.middle.open.PsOrderReceiver;
import com.pousheng.middle.open.component.OpenOrderConverter;
import com.pousheng.middle.order.dto.fsm.MiddleOrderType;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.dto.AvailableInventoryDTO;
import com.pousheng.middle.warehouse.dto.AvailableInventoryRequest;
import com.pousheng.middle.warehouse.dto.InventoryTradeDTO;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.Exception.JitUnlockStockTimeoutException;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.order.dto.OpenClientFullOrder;
import io.terminus.open.client.order.dto.OpenFullOrder;
import io.terminus.open.client.order.dto.OpenFullOrderInfo;
import io.terminus.open.client.order.dto.OpenFullOrderItem;
import io.terminus.open.client.order.enums.OpenClientStepOrderStatus;
import io.terminus.pampas.openplatform.entity.OPResponse;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.common.constants.JitConsts;
import io.terminus.parana.order.api.AbstractPersistedOrderMaker;
import io.terminus.parana.order.dto.PersistedOrderInfos;
import io.terminus.parana.order.dto.RichOrder;
import io.terminus.parana.order.impl.manager.OrderManager;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.ShopOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JIT订单管理
 *
 * @author tanlongjun
 */
@Slf4j
@Component
public class JitOrderManager extends PsOrderReceiver {

    private final AbstractPersistedOrderMaker orderMaker;
    private final OrderManager orderManager;

    private final RedisLockClient redisLockClient;
    private final OpenShopCacher openShopCacher;

    /**
     * 根据渠道判断订单是否插入中台
     */
    private static final String IS_ORDER_INSERT_MIDDLE = "isOrderInsertMiddle";

    @RpcConsumer
    private OpenShopReadService openShopReadService;

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;
    private final OpenOrderConverter openOrderConverter;

    private final InventoryClient inventoryClient;

    @Autowired
    private WarehouseCacher warehouseCacher;

    @RpcConsumer
    private OrderWriteService orderWriteService;

    @RpcConsumer
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;

    public JitOrderManager(AbstractPersistedOrderMaker orderMaker, OrderManager orderManager,
                           OpenShopCacher openShopCacher, InventoryClient inventoryClient,
                           OpenOrderConverter openOrderConverter, RedisLockClient redisLockClient) {
        this.orderMaker = orderMaker;
        this.orderManager = orderManager;
        this.openShopCacher = openShopCacher;
        this.redisLockClient = redisLockClient;
        this.openOrderConverter = openOrderConverter;
        this.inventoryClient = inventoryClient;
    }

    /**
     * 批量处理时效订单
     *
     * @param orders 订单列表
     * @return
     */
    @Transactional
    public OPResponse<String> batchHandleRealTimeOrder(List<OpenFullOrderInfo> orders) {
        for (OpenFullOrderInfo openFullOrderInfo : orders) {
            OPResponse<String> response = handleRealTimeOrder(openFullOrderInfo);
            if (!response.isSuccess()) {
                return response;
            }
        }

        return OPResponse.ok();
    }

    /**
     * 处理时效订单
     *
     * @param openFullOrderInfo 订单信息
     */
    @Transactional
    public OPResponse<String> handleRealTimeOrder(OpenFullOrderInfo openFullOrderInfo) {
        String shopCode = openFullOrderInfo.getOrder().getCompanyCode() + SymbolConsts.MINUS +
            openFullOrderInfo.getOrder()
                .getShopCode();

        //查询该渠道的店铺信息
        Long openShopId = validateOpenShop(shopCode);
        OpenShop openShop = openShopCacher.findById(openShopId);
        Map<String, Integer> skuItemMap = Maps.newHashMap();
        for (OpenFullOrderItem item : openFullOrderInfo.getItem()) {
            skuItemMap.put(item.getSkuCode(), item.getQuantity());
        }

        //查询仓库编号
        String stockId = openFullOrderInfo.getOrder().getStockId();
        WarehouseDTO warehouseDTO = warehouseCacher.findByCode(stockId);

        //验证库存是否足够
        boolean outstock = validateInventory(openShopId, warehouseDTO.getId(), skuItemMap);
        if (!outstock) {
            return OPResponse.fail("inventory.not.enough");
        }

        //业务参数校验
        OPResponse<String> response = validateBusiParam(openFullOrderInfo);
        if (!response.isSuccess()) {
            return response;
        }
        //组装参数
        OpenClientFullOrder openClientFullOrder = openOrderConverter.transform(openFullOrderInfo);

        //设置为jit时效订单类型
        openClientFullOrder.setType(MiddleOrderType.JIT_REAL_TIME.getValue());
        //保存中台仓库id
        Map<String,String> extraMap=openClientFullOrder.getExtra();
        extraMap.put(JitConsts.WAREHOUSE_ID,String.valueOf(warehouseDTO.getId()));
        //保存订单
        handleReceiveOrder(OpenClientShop.from(openShop), Lists.newArrayList(openClientFullOrder),
            warehouseDTO.getId());

        return OPResponse.ok();
    }

    /**
     * 处理订单业务
     *
     * @param openClientShop
     * @param openClientFullOrders
     * @param warehouseId
     */
    @Transactional
    public void handleReceiveOrder(OpenClientShop openClientShop, List<OpenClientFullOrder> openClientFullOrders,
                                   Long warehouseId) {
        for (OpenClientFullOrder openClientFullOrder : openClientFullOrders) {
            if (Objects.nonNull(openClientFullOrder.getIsStepOrder())
                && Objects.nonNull(openClientFullOrder.getStepStatus()) && openClientFullOrder.getIsStepOrder()
                && Objects.equals(openClientFullOrder.getStepStatus().getValue(),
                OpenClientStepOrderStatus.NOT_PAID.getValue())) {
                continue; //预售未付款订单不会被拉取
            }
            Response<Optional<ShopOrder>> findShopOrder = shopOrderReadService.findByOutIdAndOutFrom(
                openClientFullOrder.getOrderId(), openClientShop.getChannel());
            if (!findShopOrder.isSuccess()) {
                log.error("fail to find shop order by outId={},outFrom={} when receive order,cause:{}",
                    openClientFullOrder.getOrderId(), openClientShop.getChannel(), findShopOrder.getError());
                continue;
            }
            Optional<ShopOrder> shopOrderOptional = findShopOrder.getResult();

            if (shopOrderOptional.isPresent()) {
                updateParanaOrder(shopOrderOptional.get(), openClientFullOrder);
            } else {
                RichOrder richOrder = makeParanaOrder(openClientShop, openClientFullOrder);

                Map<String,String> extraMap=richOrder.getExtra();
                if (extraMap == null) {
                    extraMap = Maps.newHashMap();
                }
                extraMap.put(JitConsts.WAREHOUSE_ID, warehouseId.toString());
                saveOrderAndLockInventory(richOrder, warehouseId);
                // jit 时效订单不创建发货单 故不用post event
            }
        }
    }

    /**
     * 保存订单且占库存
     *
     * @return
     */
    public void saveOrderAndLockInventory(RichOrder richOrder, Long warehouseId) {
        if (richOrder == null) {
            return;
        }
        //save to db
        PersistedOrderInfos persistedOrderInfos = orderMaker.make(richOrder);
        List<Long> shopOrderIds = orderManager.create(persistedOrderInfos);

        lockInventory(warehouseId, persistedOrderInfos.getSkuOrdersByShopId());
    }

    /**
     * 查询外部渠道
     *
     * @param shopCode
     * @return
     */
    private Long validateOpenShop(String shopCode) {

        //查询店铺的信息，如果没有就新建一个
        Response<List<OpenClientShop>> rP = openShopReadService.search(null, null, shopCode);
        if (!rP.isSuccess()) {
            log.error("find open shop failed,shopCode is {},caused by {}", shopCode, rP.getError());
            throw new ServiceException("find.open.shop.failed");
        }
        List<OpenClientShop> openClientShops = rP.getResult();
        if(CollectionUtils.isEmpty(openClientShops)) {
            throw new OPServerException(200,"find.open.shop.fail");
        }
        java.util.Optional<OpenClientShop> openClientShopOptional = openClientShops.stream().findAny();
        OpenClientShop openClientShop = openClientShopOptional.get();
        return openClientShop.getOpenShopId();

    }

    /**
     * 业务参数校验
     *
     * @param openFullOrderInfo
     */
    private OPResponse<String> validateBusiParam(OpenFullOrderInfo openFullOrderInfo) {
        OpenFullOrder openFullOrder = openFullOrderInfo.getOrder();
        if (Objects.isNull(openFullOrder.getOutOrderId())) {
            return OPResponse.fail("outOrderId.is.null");
        }
        if (Objects.isNull(openFullOrder.getChannel())) {
            return OPResponse.fail("channel.is.null");
        }
        String outId = openFullOrder.getOutOrderId();
        String channel = openFullOrder.getChannel();
        Response<Optional<ShopOrder>> rP = shopOrderReadService.findByOutIdAndOutFrom(outId, channel);
        if (!rP.isSuccess()) {
            log.error("find shopOrder failed,outId is {},outFrom is {},caused by {}", outId, channel, rP.getError());
        }
        Optional<ShopOrder> shopOrderOptional = rP.getResult();
        if (shopOrderOptional.isPresent()) {
            return OPResponse.fail("shop.order.is.exist");
        }
        return OPResponse.ok();
    }

    /**
     * 验证库存是否满足
     *
     * @return
     */
    public boolean validateInventory(Long shopId, Long warehouseId, Map<String, Integer> skus) {
        List<AvailableInventoryRequest> requests = Lists.newArrayList();
        if (skus == null
            || skus.isEmpty()) {
            return false;
        }
        //构造请求参数
        AvailableInventoryRequest request;
        for (Map.Entry<String, Integer> entry : skus.entrySet()) {
            request = new AvailableInventoryRequest();
            request.setWarehouseId(warehouseId);
            request.setSkuCode(entry.getKey());
            requests.add(request);
        }

        try {
            //发送http请求查询库存
            Response<List<AvailableInventoryDTO>> response = inventoryClient.getAvailableInventory(requests, shopId);
            log.info("query available inventory.result:{}",JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(response));
            if (!response.isSuccess()) {
                return false;
            }

            List<AvailableInventoryDTO> list = response.getResult();
            if (CollectionUtils.isEmpty(list)) {
                return false;
            }

            for (AvailableInventoryDTO dto : list) {
                Integer quantity = skus.get(dto.getSkuCode());
                if (quantity == null) {
                    continue;
                }
                if (dto.getTotalAvailQuantity() == null) {
                    return false;
                }
                if (quantity.compareTo(dto.getTotalAvailQuantity()) >= 0) {
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("failed to query inventory,shopId:{},AvailableInventoryRequest:{}", shopId, requests);
            return false;
        }
        return true;
    }

    /**
     * 占库存
     *
     * @param warehouseId
     * @param skuOrderMultimap
     */
    protected void lockInventory(Long warehouseId,
                                 ListMultimap<Long, SkuOrder> skuOrderMultimap) {
        List<InventoryTradeDTO> inventoryTradeDTOList = Lists.newArrayList();
        InventoryTradeDTO dto;

        for (SkuOrder skuOrder : skuOrderMultimap.values()) {
            dto = InventoryTradeDTO.builder()
                .bizSrcId(skuOrder.getOrderId().toString())
                .subBizSrcId(Lists.newArrayList(skuOrder.getOrderId().toString()))
                .shopId(skuOrder.getShopId())
                .quantity(skuOrder.getQuantity())
                .skuCode(skuOrder.getSkuCode())
                .warehouseId(warehouseId)
                .build();
            inventoryTradeDTOList.add(dto);
        }
        Response<Boolean> response = inventoryClient.lock(inventoryTradeDTOList);
        if (!response.isSuccess() || !response.getResult()) {
            log.error("failed to lock inventory.param:{}", inventoryTradeDTOList);
            // save unlock inventory task.
            if (Objects.equals(response.getError(), "inventory.response.timeout")) {
                JitUnlockStockTimeoutException exception = new JitUnlockStockTimeoutException();
                exception.setData(inventoryTradeDTOList);
                throw exception;
            } else {
                throw new BizException();
            }
        }

    }

    /**
     * 保存释放库存的补偿任务
     *
     * @param inventoryTradeDTOList
     */
    public void saveUnlockInventoryTask(List<InventoryTradeDTO> inventoryTradeDTOList) {
        String data = JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(inventoryTradeDTOList);

        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.JIT_UNLOCK_STOCK_API.toString());
        biz.setContext(data);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        poushengCompensateBizWriteService.create(biz);
    }

}
