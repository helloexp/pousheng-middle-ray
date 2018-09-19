package com.pousheng.middle.web.order.component;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.pousheng.middle.constants.SymbolConsts;
import com.pousheng.middle.open.api.constant.ExtraKeyConstant;
import com.pousheng.middle.open.manager.JitOrderManager;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.impl.manager.MiddleOrderManager;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.dto.InventoryTradeDTO;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.Exception.JitUnlockStockTimeoutException;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.constants.ParanaTradeConstants;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.SkuOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * JIT时效订单库存
 * @author tanlongjun
 */
@Slf4j
@Component
public class JitRealtimeOrderStockManager {

    public static final JsonMapper mapper=JsonMapper.JSON_NON_EMPTY_MAPPER;

    @Autowired
    private MiddleOrderReadService middleOrderReadService;

    @Autowired
    private InventoryClient inventoryClient;

    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;

    @Autowired
    private WarehouseCacher warehouseCacher;

    @Autowired
    private MiddleOrderManager middleOrderManager;

    @Autowired
    private JitOrderManager jitOrderManager;
    /**
     * 释放实效订单库存
     * @param order jit订单
     */
    public void releaseRealtimeOrderInventory(ShopOrder order){
        Map<String, String> extra=order.getExtra();
        //查询订单列表
        List<ShopOrder> orderList = queryShopOrders(order);

        //查询仓库信息
        String warehouseCode=extra.get(ParanaTradeConstants.ASSIGN_WAREHOUSE_ID);
        WarehouseDTO warehouseDTO = warehouseCacher.findByCode(warehouseCode);
        if(warehouseDTO==null){
            String msg=MessageFormat.format("warehouse not found.warehouseCode:{0}",warehouseCode);
            throw new BizException(msg);
        }

        //批量查询sku订单
        List<Long> orderIds=orderList.stream().map(ShopOrder::getId).collect(Collectors.toList());
        List<SkuOrder> skuOrderList=querySkuOrders(orderIds);

        //更新状态并释放库存
        handleReleaseLogic(warehouseDTO.getId(),orderIds,skuOrderList);
    }

    /**
     * 更新状态并释放库存
     * @param warehouseId
     * @param shopOrderList
     * @param skuOrderList
     */
    @Transactional
    public void handleReleaseLogic(Long warehouseId,List<Long> shopOrderList,List<SkuOrder> skuOrderList){
        List<Long> skuOrderIds=skuOrderList.stream().map(SkuOrder::getId).collect(Collectors.toList());
        middleOrderManager.updateOrderStatus(shopOrderList,skuOrderIds,
            MiddleOrderStatus.JIT_STOCK_RELEASED.getValue());
        //释放库存
        unlock(warehouseId,skuOrderList);
    }

    /**
     * 查询订单列表
     * @param order
     * @return
     */
    private List<ShopOrder> queryShopOrders(ShopOrder order) {
        Map<String, String> extra=order.getExtra();
        String exceptionMsg;
        String orderIdsStr=extra.get(ExtraKeyConstant.REALTIME_ORDER_IDS);
        if(StringUtils.isBlank(orderIdsStr)){
            exceptionMsg= MessageFormat.format("realtime orderIds is blank,orderInfo:{0}",
                mapper.toJson(order));
            throw new BizException(exceptionMsg);
        }

        List<String> outIds= Splitter.on(SymbolConsts.COMMA).trimResults().splitToList(orderIdsStr);

        Response<List<ShopOrder>> response = middleOrderReadService.findByOutIdsAndOutFrom(
            outIds, order.getOutFrom());
        if (response == null
            || !response.isSuccess()) {
            exceptionMsg= MessageFormat.format("failed.to.query.realtime.orders.param:{0},response:{1}",
                mapper.toJson(order),mapper.toJson(response));
            throw new BizException(exceptionMsg);
        }
        List<ShopOrder> orderList=response.getResult();
        if (CollectionUtils.isEmpty(orderList)) {
            exceptionMsg=MessageFormat.format("realtimeOrderIds.not.exist.param:{0},response:{1}",
                mapper.toJson(order),mapper.toJson(response));
            throw new BizException(exceptionMsg);
        }
        return orderList;
    }

    /**
     * 查询skuOrders
     * @param orderIds
     * @return
     */
    private List<SkuOrder> querySkuOrders(List<Long> orderIds) {
        String exceptionMsg;
        Response<List<SkuOrder>> skuOrderResponse=skuOrderReadService.findByShopOrderIds(orderIds);

        if (skuOrderResponse == null
            || !skuOrderResponse.isSuccess()) {
            exceptionMsg= MessageFormat.format("failed.to.query.sku.orders.shopOrderIds:{0},response:{1}",
                mapper.toJson(orderIds),mapper.toJson(skuOrderResponse));
            throw new BizException(exceptionMsg);
        }
        return skuOrderResponse.getResult();
    }

    /**
     * 释放库存
     * @param warehouseId
     * @param skuOrders
     */
    private void unlock(Long warehouseId,List<SkuOrder> skuOrders){
        List<InventoryTradeDTO> inventoryTradeDTOList = Lists.newArrayList();
        InventoryTradeDTO dto;

        for (SkuOrder skuOrder : skuOrders) {
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
        Response<Boolean> response = inventoryClient.unLock(inventoryTradeDTOList);
        if (!response.isSuccess() || !response.getResult()) {
            if (Objects.equals(response.getError(), "inventory.response.timeout")) {
                JitUnlockStockTimeoutException exception = new JitUnlockStockTimeoutException(response.getError());
                exception.setData(inventoryTradeDTOList);
                throw exception;
            } else {
                String msg = MessageFormat.format("failed to unlock inventory.param:{0},response:{1}",
                    mapper.toJson(inventoryTradeDTOList), mapper.toJson(response));
                throw new BizException(msg);
            }
        }
    }

    /**
     * 取消时效订单并释放库存
     * @param shopOrder
     * @param skuOrders
     * @param warehouseId
     */
    @Transactional
    public void cancelJitOrderAndUnlockStock(ShopOrder shopOrder, List<SkuOrder> skuOrders,Long warehouseId) {
        middleOrderManager.updateOrderStatusAndSkuQuantities(shopOrder, skuOrders, MiddleOrderEvent.AUTO_CANCEL_SUCCESS.toOrderOperation());
        try {
            unlock(warehouseId, skuOrders);
        } catch (JitUnlockStockTimeoutException e) {
            String param=mapper.toJson(e.getData());
            log.warn("failed to unlock stock of order[{}].param:{}],",shopOrder.getId(),param);
            //超时则保存biz补偿
            jitOrderManager.saveUnlockInventoryTask(e.getData());
        }
    }

}
