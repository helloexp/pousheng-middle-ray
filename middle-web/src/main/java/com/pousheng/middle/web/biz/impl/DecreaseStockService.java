package com.pousheng.middle.web.biz.impl;

import com.google.common.collect.Lists;
import com.pousheng.middle.open.api.constant.ExtraKeyConstant;
import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.order.dispatch.component.MposSkuStockLogic;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.dto.InventoryTradeDTO;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.common.StringUtil;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShipmentReadService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.pousheng.middle.constants.Constants.IS_CARE_STOCK;

/**
 * Created with IntelliJ IDEA
 * User: yujiacheng
 * Date: 2018/7/6
 * Time: 下午2:01
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.STOCK_API_DECREASE_STOCK)
@Service
@Slf4j
public class DecreaseStockService implements CompensateBizService {

    @Autowired
    private ShipmentReadService shipmentReadServices;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private OpenShopCacher openShopCacher;
    @Autowired
    private DispatchComponent dispatchComponent;
    @Autowired
    private InventoryClient inventoryClient;
    @Autowired
    private OrderReadLogic orderReadLogic;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        if (null == poushengCompensateBiz) {
            log.warn("DecreaseStockService.doProcess params is null");
            return;
        }

        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("DecreaseStockService.doProcess context is null");
            throw new BizException("DecreaseStockService.doProcess context is null");
        }
        Long shipmentId = JsonMapper.nonEmptyMapper().fromJson(context, Long.class);

        if (shipmentId == null) {
            log.warn("DecreaseStockService.doProcess unLockStock param shipment is null");
            throw new BizException("DecreaseStockService.doProcess unLockStock param shipment is null");
        }

        try {
            // 获取对应的发货单然后去扣减库存
            Response<Shipment> shipmentResponse =  shipmentReadServices.findById(shipmentId);
            if (!shipmentResponse.isSuccess()) {
                log.error("find shipment by id {} fail", shipmentId);
            }

            Shipment shipment = shipmentResponse.getResult();


            DispatchOrderItemInfo dispatchOrderItemInfo = shipmentReadLogic.getDispatchOrderItem(shipment);
            if(!careStock(dispatchOrderItemInfo.getOrderId())){
                return;
            }

            //仓库发货
            List<WarehouseShipment> actualShipments = dispatchOrderItemInfo.getWarehouseShipments();

            //没有说明不是仓发直接返回
            if(CollectionUtils.isEmpty(actualShipments)){
                return;
            }

            WarehouseShipment actualShipment = actualShipments.get(0);
            InventoryTradeDTO inventoryTradeDTO = dispatchComponent.genInventoryTradeDTO(dispatchOrderItemInfo);

            List<InventoryTradeDTO> tradeList = Lists.newArrayList();
            List<InventoryTradeDTO> releaseList = Lists.newArrayList();
            List<SkuCodeAndQuantity> unlockList = Lists.newArrayList();
            Map<String, Integer> itemMap = shipmentReadLogic.getShipmentItems(shipment).stream().filter(e -> e.getShipQuantity() != null)
                    .collect(Collectors.toMap(ShipmentItem::getSkuCode, ShipmentItem::getShipQuantity));
            for (SkuCodeAndQuantity sq : actualShipment.getSkuCodeAndQuantities()) {
                if (itemMap.get(sq.getSkuCode()) == null) {
                    continue;
                }
                if (itemMap.get(sq.getSkuCode()) < sq.getQuantity()) {
                    unlockList.add(new SkuCodeAndQuantity().skuOrderId(sq.getSkuOrderId()).skuCode(sq.getSkuCode())
                            .quantity(sq.getQuantity() - itemMap.get(sq.getSkuCode())));
                }
                sq.setQuantity(itemMap.get(sq.getSkuCode()));
            }
            tradeList.addAll(genTradeContextList(actualShipment.getWarehouseId(),
                    inventoryTradeDTO, actualShipment.getSkuCodeAndQuantities()));

            //这里增加如果没发全的判断 没发全要做扣减加释放
            if (!ObjectUtils.isEmpty(tradeList)) {
                Response<Boolean> tradeRet = inventoryClient.decrease(tradeList);
                if (!tradeRet.isSuccess() || !tradeRet.getResult()) {
                    log.error("fail to decrease inventory, trade trade dto: {}, shipment:{}, cause:{}", inventoryTradeDTO, actualShipment, tradeRet.getError());
                    throw new ServiceException(tradeRet.getError());

                }
            }
            if (!ObjectUtils.isEmpty(unlockList)) {
                releaseList.addAll(genTradeContextList(actualShipment.getWarehouseId(),
                        inventoryTradeDTO, unlockList));
                Response<Boolean> releaseRet = inventoryClient.unLock(releaseList);
                if (!releaseRet.isSuccess() || !releaseRet.getResult()) {
                    log.error("fail to unlock inventory, trade dto: {}, shipment:{}, cause:{}", inventoryTradeDTO, actualShipment, releaseRet.getError());
                    throw new ServiceException(releaseRet.getError());

                }
            }

        } catch (Exception e){
            throw new BizException("try to decreaseStock param for shipment fail,caused by {}", e);
        }
    }

    /**
     * 当前店铺下的订单是否关心库存
     * @param orderId 订单id
     * @return true 关心 false 不关心
     */
    private Boolean careStock(Long orderId) {
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderId);
        if (shopOrder.getShopName().startsWith("yj")) {
            if (shopOrder.getExtra().containsKey(ExtraKeyConstant.IS_CARESTOCK)
                    && Objects.equals("Y", shopOrder.getExtra().get(ExtraKeyConstant.IS_CARESTOCK))) {
                return Boolean.TRUE;
            }
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    private List<InventoryTradeDTO> genTradeContextList (Long warehouseId, InventoryTradeDTO inventoryTradeDTO, List<SkuCodeAndQuantity> skuCodeAndQuantities) {
        List<InventoryTradeDTO> tradeList = Lists.newArrayList();
        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
            String skuCode = skuCodeAndQuantity.getSkuCode();
            Integer quantity = skuCodeAndQuantity.getQuantity();

            InventoryTradeDTO currTrade = new InventoryTradeDTO();
            currTrade.setWarehouseId(warehouseId);
            currTrade.setQuantity(quantity);
            currTrade.setSkuCode(skuCode);
            currTrade.setBizSrcId(inventoryTradeDTO.getBizSrcId());
            currTrade.setSubBizSrcId(Lists.newArrayList(inventoryTradeDTO.getSubBizSrcId()));
            currTrade.setShopId(inventoryTradeDTO.getShopId());
            currTrade.setUniqueCode(inventoryTradeDTO.getUniqueCode());

            tradeList.add(currTrade);

        }

        return tradeList;
    }
}
