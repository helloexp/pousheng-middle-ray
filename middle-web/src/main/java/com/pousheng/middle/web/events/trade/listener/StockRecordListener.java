package com.pousheng.middle.web.events.trade.listener;

import com.google.common.collect.Lists;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.model.StockRecordLog;
import com.pousheng.middle.order.service.StockRecordLogWriteService;
import com.pousheng.middle.warehouse.companent.WarehouseClient;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.events.warehouse.StockRecordEvent;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.parana.order.enums.ShipmentWay;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.service.ShipmentReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;

/**
 * Created with IntelliJ IDEA
 * User: yujiacheng
 * Date: 2018/7/14
 * Time: 上午11:28
 */
@Slf4j
@Component
public class StockRecordListener {

    @Autowired
    private EventBus eventBus;
    @Autowired
    private WarehouseClient warehouseClient;
    @Autowired
    private OpenShopCacher openShopCacher;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;
    @Autowired
    private StockRecordLogWriteService stockRecordLogWriteService;
    @RpcConsumer
    private ShipmentReadService shipmentReadService;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();


    @PostConstruct
    public void init() {
        eventBus.register(this);
    }

    @Subscribe
    @AllowConcurrentEvents
    public void recordStock(StockRecordEvent event) {
        Long shipmentId = event.getShipmentId();
        String type = event.getType();
        log.info("try to record sku stock count,shipment id is {}, type is {}", shipmentId, type);
        Response<Shipment> shipmentRes = shipmentReadService.findById(shipmentId);
        if (!shipmentRes.isSuccess()) {
            log.error("failed to find shipment by id={}, error code:{}", shipmentId, shipmentRes.getError());
            return;
        }
        Shipment shipment = shipmentRes.getResult();

        Long shopId = shipment.getShopId();
        // 仓库id
        Long warehouseId = shipment.getShipId();
        // 判断仓发还是店发 ship_way :1 店发 需要去获取店铺关联的仓库ID
        if (Objects.equals(shipment.getShipWay(), 1)) {
            OpenShop openShop = openShopCacher.findById(shipment.getShipId());
            String appKey = openShop.getAppKey();
            String outCode = appKey.substring(4);
            String bizId = appKey.substring(0, 3);
            log.info("ship way by shop outCode {}, bizId {}", outCode, bizId);
            Response<WarehouseDTO> warehouseDTORes = warehouseClient.findByOutCodeBizId(outCode, bizId);
            if (!warehouseDTORes.isSuccess()) {
                log.error("failed to find warehouse by outCode {},bizId {}, error code:{}", outCode, bizId, warehouseDTORes.getError());
                return;
            }
            // 店铺关联的仓ID
            warehouseId = warehouseDTORes.getResult().getId();
        }
        List<ShipmentItem> shipmentItemList = shipmentReadLogic.getShipmentItems(shipment);
        for (ShipmentItem shipmentItem : shipmentItemList) {
            recordLog(warehouseId, shipmentItem.getSkuCode(), shopId, shipmentId, type);
        }
    }


    public void recordLog (Long warehouseId, String skuCode, Long shopId, Long shipmentId, String type) {
        List<Long> warehouseIds = Lists.newArrayList();
        warehouseIds.add(warehouseId);
        List<String> skuCodes = Lists.newArrayList();
        skuCodes.add(skuCode);
        List<HkSkuStockInfo> hkSkuStockInfos = queryHkWarhouseOrShopStockApi.doQueryStockInfo(warehouseIds, skuCodes, shopId);
        String context = mapper.toJson(hkSkuStockInfos);
        StockRecordLog stockRecordLog = new StockRecordLog();
        stockRecordLog.setShipmentId(shipmentId);
        stockRecordLog.setWarehouseId(warehouseId);
        stockRecordLog.setShopId(shopId);
        stockRecordLog.setSkuCode(skuCode);
        stockRecordLog.setContext(context);
        stockRecordLog.setType(type);
        Response<Long> res = stockRecordLogWriteService.create(stockRecordLog);
        if (!res.isSuccess()) {
            log.error("stock record create fail, shipment id {}, warehouse id {}, skuCode {}, type {}, error code {}",
                    shipmentId, warehouseId, skuCode, type, res.getError());
        }

    }

}
