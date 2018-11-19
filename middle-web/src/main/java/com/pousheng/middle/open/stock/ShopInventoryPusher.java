package com.pousheng.middle.open.stock;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.warehouse.companent.WarehouseRulesClient;
import com.pousheng.middle.warehouse.companent.WarehouseShopRuleClient;
import com.pousheng.middle.warehouse.model.StockPushLog;
import com.pousheng.middle.web.mq.warehouse.model.InventoryChangeDTO;
import com.pousheng.middle.web.order.component.ShopMaxOrderLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 店铺库存推送
 * @author tanlongjun
 */
@Component
@Slf4j
public class ShopInventoryPusher {

    @Autowired
    private StockPusherLogic stockPushLogic;
    @Autowired
    private WarehouseShopRuleClient warehouseShopRuleClient;
    @Autowired
    private WarehouseRulesClient warehouseRulesClient;
    @Value("${mpos.open.shop.id}")
    private Long mposOpenShopId;
    private static final Integer HUNDRED = 100;
    @Autowired
    private QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;

    @Autowired
    private StockPushCacher stockPushCacher;
    @Setter
    @Value("${stock.push.cache.enable: true}")
    private boolean stockPusherCacheEnable;

    @RpcConsumer
    private MappingReadService mappingReadService;

    @Autowired
    private ShopMaxOrderLogic shopMaxOrderLogic;

    @Autowired
    private ShopStockPusher shopStockPusher;

    /**
     *
     * 推送
     * InventoryChangeDTO数据格式:
     * 1.只有skuCode
     * 2.只有skuCode和warehouseId
     * 3.只有skuCode和shopId
     *
     * PS:
     *  jit店铺无item_mapping。故这里不会处理jit店铺的推送
     * @param changeDTOList
     */
    public void push(List<InventoryChangeDTO> changeDTOList) {

        log.info("INVENTORY-PUSHER-SUBMIT-START param: skuCodes:{},start time:{}", changeDTOList, System.currentTimeMillis());
        log.info("start to push skus: {}", changeDTOList);

        Table<Long, String, Integer> paranaSkuStock = HashBasedTable.create();
        Table<Long, String, Integer> vipSkuStock = HashBasedTable.create();

        //库存推送日志记录
        List<StockPushLog> logs = Lists.newArrayList();
        for (InventoryChangeDTO changeDTO : changeDTOList) {
            String skuCode=changeDTO.getSkuCode();
            try {
                List<Long> shopIds = Lists.newArrayList();
                //若仓库不为空
                if (!Objects.isNull(changeDTO.getWarehouseId())) {
                    //找到对应的店铺id, 这些店铺需要进行库存推送
                    Response<List<Long>> r = warehouseShopRuleClient.findShopIdsByWarehouseId(
                        changeDTO.getWarehouseId());
                    if (!r.isSuccess()) {
                        log.error("failed to find out shops for warehouse(id={}), error code:{}",
                            changeDTO.getWarehouseId(), r.getError());
                        continue;
                    }
                    shopIds = r.getResult();
                } else if (!Objects.isNull(changeDTO.getShopId())) {
                    //若店铺不为空
                    shopIds = Lists.newArrayList(changeDTO.getShopId());
                }
                //计算库存分配并将库存推送到每个外部店铺去
                shopStockPusher.handle(shopIds,skuCode,paranaSkuStock,logs);
            } catch (Exception e) {
                log.error("failed to push stock,sku is {}", skuCode,e);
            }
        }

        //官网批量推送
        if(!paranaSkuStock.isEmpty()) {
            stockPushLogic.sendToParana(paranaSkuStock);
        }

        log.info("INVENTORY-PUSHER-SUBMIT-END param: skuCodes:{},end time:{}", changeDTOList, System.currentTimeMillis());
    }

}