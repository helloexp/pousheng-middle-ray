package com.pousheng.middle.web.order.sync.yyedi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.hksyc.component.SycHkOrderCancelApi;
import com.pousheng.middle.open.api.constant.ExtraKeyConstant;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentDetail;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.HkPayType;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddlePayType;
import com.pousheng.middle.shop.service.PsShopReadService;
import com.pousheng.middle.warehouse.companent.WarehouseClient;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.constants.WmsShipmentResponseCode;
import com.pousheng.middle.yyedisyc.component.SyncWmsShipmentOrderApi;
import com.pousheng.middle.yyedisyc.dto.WmsResponse;
import com.pousheng.middle.yyedisyc.dto.trade.WmsShipmentInfo;
import com.pousheng.middle.yyedisyc.dto.trade.WmsShipmentItem;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShipmentItem;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 同步WMS发货单逻辑
 * @author tanlongjun
 */
@Slf4j
@Component
public class SyncWmsShipmentLogic {

    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private SyncWmsShipmentOrderApi syncWmsShipmentOrderApi;
    @Autowired
    private SycHkOrderCancelApi sycHkOrderCancelApi;
    @Autowired
    private WarehouseClient warehouseClient;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private PsShopReadService psShopReadService;
    @Autowired
    private OpenShopReadService openShopReadService;

    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 同步发货单到Wms
     *
     * @param shipment 发货单
     * @return 同步成功true, 同步失败false
     */
    public Response<Boolean> syncShipmentToWms(Shipment shipment) {
        try {
            //更新状态为同步中
            OrderOperation orderOperation = MiddleOrderEvent.SYNC_YYEDI.toOrderOperation();
            Response<Boolean> updateStatusRes = shipmentWiteLogic.updateStatusLocking(shipment, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), orderOperation.getText(), updateStatusRes.getError());
                return Response.fail(updateStatusRes.getError());
            }

            Flow flow = flowPicker.pickShipments();
            Integer targetStatus = flow.target(shipment.getStatus(), orderOperation);
            shipment.setStatus(targetStatus);
            WmsShipmentInfo list = this.makeShipmentOrderDto(shipment,shipment.getType());
            WmsResponse response  = JsonMapper.nonEmptyMapper().fromJson(syncWmsShipmentOrderApi.doSyncShipmentOrder(list),WmsResponse.class);
            if (Objects.equals(response.getCode(), WmsShipmentResponseCode.SUCCESS.getCode())){
                //整体成功
                OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
                Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatusLocking(shipment, syncOrderOperation);
                if (!updateSyncStatusRes.isSuccess()) {
                    log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
                    return Response.fail(updateSyncStatusRes.getError());
                }
            }else{
               //整体失败
                OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
                Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatusLocking(shipment, syncOrderOperation);
                if (!updateSyncStatusRes.isSuccess()) {
                    log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
                }
                return Response.fail(response.getReturnJson());
            }
        } catch (Exception e) {
            log.error("sync wms shipment failed,shipmentId is({}) cause by({})", shipment.getId(), Throwables.getStackTraceAsString(e));
            //更新状态为同步失败
            updateShipmetSyncFail(shipment);
            return Response.fail("sync.wms.shipment.fail");
        }
        return Response.ok(Boolean.TRUE);
    }

    /**
     * 发货单同步恒康参数组装
     *
     * @param shipment
     * @param shipmentType 1.正常销售单，2.换货发货，3.补发
     * @return
     */
    public WmsShipmentInfo makeShipmentOrderDto(Shipment shipment, int shipmentType) {
        //获取发货单详情
        ShipmentDetail shipmentDetail = shipmentReadLogic.orderDetail(shipment.getId());
        return getSyncWmsShipmentInfo(shipment,shipmentDetail,shipmentType);
    }

    /**
     * 组装发往订单派发中心的发货单
     *
     * @param shipment
     * @param shipmentDetail
     * @param shipmentType 1.正常销售单，2.换货发货，3.补发
     * @return
     */
    private WmsShipmentInfo getSyncWmsShipmentInfo(Shipment shipment, ShipmentDetail shipmentDetail,int shipmentType) {
        ShopOrder shopOrder = shipmentDetail.getShopOrder();
        ReceiverInfo receiverInfo = shipmentDetail.getReceiverInfo();
        ShipmentExtra shipmentExtra = shipmentDetail.getShipmentExtra();
        Response<WarehouseDTO> rW  = warehouseClient.findById(shipmentExtra.getWarehouseId());
        if (!rW.isSuccess()){
            throw new ServiceException("find.warehouse.failed");
        }
        Map<String,String> shopOrderExtra = shopOrder.getExtra();

        WarehouseDTO warehouse = rW.getResult();
        WmsShipmentInfo shipmentInfo = new WmsShipmentInfo();
        // 手工单号
        shipmentInfo.setManualbillno(shopOrderExtra.get(ExtraKeyConstant.JIT_ORDER_ID));
        //公司内码
        shipmentInfo.setCompanycode(warehouse.getCompanyCode());
        //仓库
        shipmentInfo.setStockcode(warehouse.getOutCode());
        //仓库编码
        shipmentInfo.setInterstockcode(shopOrderExtra.get(ExtraKeyConstant.INTER_STOCK_CODE));
        //仓库名称
        shipmentInfo.setInterstockname(shopOrderExtra.get(ExtraKeyConstant.INTER_STOCK_NAME));
        //承运商
        shipmentInfo.setFreightcompany(shopOrderExtra.get(ExtraKeyConstant.FREIGHT_COMPANY));
        //erp单号，中台发货单代码
        shipmentInfo.setBillno(String.valueOf(shipment.getShipmentCode()));
        //单据类型
        shipmentInfo.setBilltype(TradeConstants.YUN_JU_JIT_BILL_TYPE);

        shipmentInfo.setBilldate(formatter.print(shopOrder.getOutCreatedAt().getTime()));
        //下游单号
        shipmentInfo.setPrefinishbillno(shopOrderExtra.get(ExtraKeyConstant.PRE_FINISH_BILLO));
        //批次号
        shipmentInfo.setBatchno(shopOrderExtra.get(ExtraKeyConstant.BATCH_NO));
        //批次备注
        shipmentInfo.setBatchmark(shopOrderExtra.get(ExtraKeyConstant.BATCH_MARK));
        //发运编码
        shipmentInfo.setTransportmethodcode(shopOrderExtra.get(ExtraKeyConstant.TRANSPORT_METHOD_CODE));
        //发运方式
        shipmentInfo.setTransportmethodname(shopOrderExtra.get(ExtraKeyConstant.TRANSPORT_METHOD_NAME));
        //品牌
        shipmentInfo.setCardremark(shopOrderExtra.get(ExtraKeyConstant.CARD_REMARK));
        //预计出/入库日期
        shipmentInfo.setExpectdate(shopOrderExtra.get(ExtraKeyConstant.EXPECT_DATE));
        //客商编码
        shipmentInfo.setVendcustcode(shopOrderExtra.get(ExtraKeyConstant.VEND_CUST_Code));
        //联系人
        shipmentInfo.setContact(receiverInfo.getReceiveUserName());
        //收件省
        shipmentInfo.setProvince(receiverInfo.getProvince());
        //收件市
        shipmentInfo.setCity(receiverInfo.getCity());
        //收件区
        shipmentInfo.setArea(receiverInfo.getRegion());
        //地址
        shipmentInfo.setAddress(receiverInfo.getDetail());
        //电话
        shipmentInfo.setPhone(receiverInfo.getPhone());
        shipmentInfo.setChannelcode(shopOrderExtra.get(ExtraKeyConstant.CHANNEL_CODE));
        //获取发货单中对应的sku列表
        List<WmsShipmentItem> items = getSyncWmsShipmentItem(shipment, shipmentDetail);
        int quantity = 0;
        for (WmsShipmentItem item:items){
            quantity = quantity +item.getExpectqty();
        }
        ////预期数量
        shipmentInfo.setExpectqty(quantity);
        shipmentInfo.setOrdersizes(items);
        return shipmentInfo;
    }

    /**
     * 组装发往订单派发中心的的发货单商品列表
     *
     * @param shipment
     * @param shipmentDetail
     * @return
     * //TODO log error
     */
    private List<WmsShipmentItem> getSyncWmsShipmentItem(Shipment shipment, ShipmentDetail shipmentDetail) {
        List<ShipmentItem> shipmentItems = shipmentDetail.getShipmentItems();
        ShipmentExtra shipmentExtra = shipmentDetail.getShipmentExtra();
        Response<WarehouseDTO> rW  = warehouseClient.findById(shipmentExtra.getWarehouseId());
        if (!rW.isSuccess()){
            throw new ServiceException("");
        }
        List<WmsShipmentItem> items = Lists.newArrayListWithCapacity(shipmentItems.size());

        boolean isJitOrder=fromJit(shipmentDetail.getShopOrder());
        Map<String,Integer> skuQuantityMap=Maps.newHashMap();
        //如果是jit拣货单 则根据sku-code合并计算数量
        if(isJitOrder) {
            skuQuantityMap = caculateSkuQuantity(shipmentItems);
        }
        //记录已经处理过的sku-code 避免重复
        Map<String,Boolean> existSku=Maps.newHashMap();

        for (ShipmentItem shipmentItem : shipmentItems) {
            //如果是JIT拣货单 且已经处理过了 就直接跳过。
            if (isJitOrder
                && existSku.containsKey(shipmentItem.getSkuCode())
                && existSku.get(shipmentItem.getSkuCode())) {
                continue;
            }
            WmsShipmentItem item = new WmsShipmentItem();
            //中台sku
            item.setSku(shipmentItem.getSkuCode());
            Response<List<SkuTemplate>> rS = skuTemplateReadService.findBySkuCodes(Lists.newArrayList(shipmentItem.getSkuCode()));
            if (!rS.isSuccess()){
                throw new ServiceException("find.sku.template.failed");
            }
            List<SkuTemplate> skuTemplates = rS.getResult();
            Optional<SkuTemplate> skuTemplateOptional = skuTemplates.stream().findAny();
            if (!skuTemplateOptional.isPresent()){
                throw new ServiceException("find.sku.template.failed");
            }
            SkuTemplate skuTemplate = skuTemplateOptional.get();
            Map<String,String> extraMaps = skuTemplate.getExtra();
            String materialCode = extraMaps.get(TradeConstants.HK_MATRIAL_CODE);
            //货号
            item.setMaterialcode(materialCode);
            //尺码名称
            String sizeName = "";
            List<SkuAttribute> skuAttributes = skuTemplate.getAttrs();
            for (SkuAttribute skuAttribute:skuAttributes){
                if (Objects.equals(skuAttribute.getAttrKey(),"尺码")){
                    sizeName = skuAttribute.getAttrVal();
                }
            }
            item.setSizename(sizeName);

            //如果是jit拣货单 则取合并过的数量
            if (isJitOrder) {
                item.setExpectqty(skuQuantityMap.get(shipmentItem.getSkuCode()));
                existSku.put(shipmentItem.getSkuCode(),Boolean.TRUE);
            } else {
                item.setExpectqty(shipmentItem.getQuantity());
            }
            //必须大于0
            if(item.getExpectqty()>0) {
                items.add(item);
            }
        }
        return items;
    }



    /**
     * 中台支付类型映射为订单派发中心支付类型
     *
     * @param shipmentDetail
     * @return
     */
    private HkPayType getYYEdiPayType(ShipmentDetail shipmentDetail) {
        MiddlePayType middlePayType = MiddlePayType.fromInt(shipmentDetail.getShopOrder().getPayType());
        if (Arguments.isNull(middlePayType)) {
            log.error("shipment(id:{})invalid", shipmentDetail.getShipment().getId());
            throw new ServiceException("shoporder.payType.invalid");
        }
        switch (middlePayType) {
            case ONLINE_PAY:
                return HkPayType.HK_ONLINE_PAY;
            case CASH_ON_DELIVERY:
                return HkPayType.HK_CASH_ON_DELIVERY;
            default:
                log.error("shippment(id:{}) invalid", shipmentDetail.getShipment().getId());
                throw new ServiceException("shoporder.payType.invalid");
        }
    }



    /**
     * 更新同步到订单派发中心的发货单状态为失败
     * @param shipment
     */
    private void updateShipmetSyncFail(Shipment shipment){
        //更新发货单的状态
        OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
        Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatusLocking(shipment, syncOrderOperation);
        if (!updateSyncStatusRes.isSuccess()) {
            //这里失败只打印日志即可
            log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
        }
    }


    /**
     * 是否是云聚JIT订单
     * @param order
     * @return
     */
    private boolean fromJit(ShopOrder order){
        return MiddleChannel.YUNJUJIT.getValue().equals(order.getOutFrom());
    }

    /**
     * 根据skucode合并数量
     * @param shipmentItems
     * @return
     */
    private Map<String,Integer> caculateSkuQuantity(List<ShipmentItem> shipmentItems){
        //stream 实现
        return shipmentItems.stream().collect(
            Collectors.groupingBy(ShipmentItem::getSkuCode,Collectors.summingInt(ShipmentItem::getQuantity)));
    }

}
