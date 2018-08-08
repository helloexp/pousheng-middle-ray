package com.pousheng.middle.web.order.sync.yjerp;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentDetail;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.warehouse.companent.WarehouseClient;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.order.component.MiddleOrderFlowPicker;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import com.pousheng.middle.yyedisyc.component.SycYYEdiOrderCancelApi;
import com.pousheng.middle.yyedisyc.component.SycYYEdiShipmentOrderApi;
import com.pousheng.middle.yyedisyc.dto.YJErpResponse;
import com.pousheng.middle.yyedisyc.dto.trade.YJErpCancelInfo;
import com.pousheng.middle.yyedisyc.dto.trade.YJErpShipmentInfo;
import com.pousheng.middle.yyedisyc.dto.trade.YJErpShipmentProductInfo;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.constant.ExtraKeyConstant;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShipmentWriteService;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sun.misc.BASE64Encoder;
import java.util.*;

/**
 * @Description: 同步云聚ERP发货单逻辑
 * @author: yjc
 * @date: 2018/7/31下午3:50
 */
@Slf4j
@Component
public class SyncYJErpShipmentLogic {

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @Autowired
    private WarehouseClient warehouseClient;
    @Autowired
    private SycYYEdiShipmentOrderApi sycYYEdiShipmentOrderApi;
    @Autowired
    private SycYYEdiOrderCancelApi sycYYEdiOrderCancelApi;
    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;
    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;


    /**
     * 同步发货单到云聚ERP
     *
     * @param shipment 发货单
     * @return 同步成功true, 同步失败false
     */
    public Response<Boolean> syncShipmentToYJErp(Shipment shipment) {
        try {
            String shipmentInfo = JsonMapper.nonEmptyMapper().toJson(shipment);
            log.info("sync shipment info {} to yj erp", shipmentInfo);
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

            List<YJErpShipmentInfo> list = getSycYJErpShipmentInfo(shipment);
            String response = sycYYEdiShipmentOrderApi.doSyncYJErpShipmentOrder(list);
            JSONObject responseObj = JSONObject.parseObject(response);
            if (Objects.equals(responseObj.get("error"), 0)) {
                JSONObject data = JSONObject.parseObject(responseObj.getString("data"));
                // 成功以后保存order_sn
                String order_sn = data.getString("order_sn");
                log.info("yj erp order_sn : {}", order_sn);
                ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
                shipmentExtra.setOutShipmentId(order_sn);

                //封装更新信息
                Shipment update = new Shipment();
                update.setId(shipment.getId());
                Map<String, String> extraMap = shipment.getExtra();
                extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, mapper.toJson(shipmentExtra));
                update.setExtra(extraMap);
                //更新基本信息
                Response<Boolean> updateRes = shipmentWriteService.update(update);
                if (!updateRes.isSuccess()) {
                    log.error("update shipment(id:{}) extraMap to :{} fail,error:{}", shipment.getId(), extraMap, updateRes.getError());
                    return Response.fail(updateRes.getError());
                }

                //整体成功
                OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
                Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatusLocking(shipment, syncOrderOperation);
                if (!updateSyncStatusRes.isSuccess()) {
                    log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
                    return Response.fail(updateSyncStatusRes.getError());
                }
            }
            else {
                //整体失败
                OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
                Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatusLocking(shipment, syncOrderOperation);
                if (!updateSyncStatusRes.isSuccess()) {
                    log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
                }
                // 失败原因
                log.error("订单派发中心返回信息:{}", response);
                return Response.fail(responseObj.getString("error_info"));
            }

        } catch (Exception e) {
            log.error("sync yj erp shipment failed,shipmentId is({}) cause by({})", shipment.getId(), Throwables.getStackTraceAsString(e));
            //更新状态为同步失败
            updateShipmetSyncFail(shipment);
            return Response.fail("sync.yj.erp.shipment.fail");
        }
        return Response.ok(Boolean.TRUE);

    }


    /**
     * 同步发货单取消云聚ERP
     * @param shipment 发货单
     * @return 同步结果, 同步成功true, 同步失败false
     */
    public Response<Boolean> syncShipmentCancelToYJErp(Shipment shipment) {
        try {
            String shipmentInfo = JsonMapper.nonEmptyMapper().toJson(shipment);
            log.info("sync shipment info {} to yj erp with cancel operation", shipmentInfo);
            Flow flow = flowPicker.pickShipments();
            //更新状态为同步中
            OrderOperation orderOperation = MiddleOrderEvent.CANCEL_HK.toOrderOperation();
            Response<Boolean> updateStatusRes = shipmentWiteLogic.updateStatusLocking(shipment, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), orderOperation.getText(), updateStatusRes.getError());
                return Response.fail(updateStatusRes.getError());
            }
            Integer targetStatus = flow.target(shipment.getStatus(), orderOperation);
            shipment.setStatus(targetStatus);
            List<YJErpCancelInfo> list = getSycYJErpCancelInfo(shipment);
            String response = sycYYEdiOrderCancelApi.doYJErpCancelOrder(list);
            JSONObject responseObj = JSONObject.parseObject(response);
            if (Objects.equals(responseObj.get("error"), 0)) {
                OrderOperation operation = MiddleOrderEvent.SYNC_CANCEL_SUCCESS.toOrderOperation();
                Response<Boolean> updateStatus = shipmentWiteLogic.updateStatusLocking(shipment, operation);
                if (!updateStatus.isSuccess()) {
                    log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), operation.getText(), updateStatus.getError());
                    return Response.fail(updateStatusRes.getError());
                }
            }
            else {
                //更新状态取消失败
                updateShipmetSyncCancelFail(shipment);
                // 失败原因
                log.error("订单派发中心返回信息:{}", response);
                return Response.fail("订单派发中心返回信息:" + responseObj.getString("error_info"));
            }

        } catch (ServiceException e1) {
            log.error("sync yj erp shipment failed,shipmentId is({}) cause by({})", shipment.getId(), Throwables.getStackTraceAsString(e1));
            //更新状态取消失败
            updateShipmetSyncCancelFail(shipment);
            return Response.fail(e1.getMessage());
        } catch (Exception e) {
            log.error("sync yj erp shipment failed,shipmentId is({}) cause by({})", shipment.getId(), Throwables.getStackTraceAsString(e));
            //更新状态取消失败
            updateShipmetSyncCancelFail(shipment);
            return Response.fail("sync.yj.erp.cancel.shipment.failed");
        }
        return Response.ok(Boolean.TRUE);
    }


    /**
     * 发货单同步云聚ERP参数组装
     * @param shipment
     * @return
     */
    public List<YJErpShipmentInfo> getSycYJErpShipmentInfo (Shipment shipment) {
        List<YJErpShipmentInfo> list = Lists.newArrayList();
        //获取发货单详情
        ShipmentDetail shipmentDetail = shipmentReadLogic.orderDetail(shipment.getId());
        list.add(convertYJErpShipmentInfo(shipmentDetail));
        return list;
    }

    public YJErpShipmentInfo convertYJErpShipmentInfo(ShipmentDetail shipmentDetail) {
        YJErpShipmentInfo yjErpShipmentInfo = new YJErpShipmentInfo();
        Shipment shipment = shipmentDetail.getShipment();
        ReceiverInfo receiverInfo = shipmentDetail.getReceiverInfo();
        // 外部单号,中台发货单号
        yjErpShipmentInfo.setOther_order_sn(shipment.getShipmentCode());
        // 收货人
        yjErpShipmentInfo.setConsignee(receiverInfo.getReceiveUserName());
        // 省 编码
        yjErpShipmentInfo.setProvince(String.valueOf(receiverInfo.getProvinceId()));
        // 市 编码
        yjErpShipmentInfo.setCity(String.valueOf(receiverInfo.getCityId()));
        // 区 编码
        yjErpShipmentInfo.setArea(String.valueOf(receiverInfo.getRegionId()));
        // 省 名称
        yjErpShipmentInfo.setProvince_name(receiverInfo.getProvince());
        // 市 名称
        yjErpShipmentInfo.setCity_name(receiverInfo.getCity());
        // 区 名称
        yjErpShipmentInfo.setArea_name(receiverInfo.getRegion());
        // 联系地址
        yjErpShipmentInfo.setAddress(receiverInfo.getDetail());
        // 编码
        yjErpShipmentInfo.setZipcode(receiverInfo.getPostcode());
        // 联系电话
        yjErpShipmentInfo.setTelephone(receiverInfo.getPhone());
        // 手机号码
        yjErpShipmentInfo.setMobile(receiverInfo.getMobile());
        // 配送方式
        yjErpShipmentInfo.setDelivery_type_id(2);
        ShopOrder shopOrder = shipmentDetail.getShopOrder();
        if (shopOrder.getExtra().containsKey(TradeConstants.SHOP_ORDER_HK_EXPRESS_CODE)
                && Objects.equals(shopOrder.getExtra().get(TradeConstants.SHOP_ORDER_HK_EXPRESS_CODE), TradeConstants.JD_VEND_CUST_ID)) {
            // 京东配送
            yjErpShipmentInfo.setDelivery_type_id(9);
        }
        if (shipment.getExtra().containsKey(ExtraKeyConstant.SHIPMENT_TYPE) &&
                Objects.equals(shipment.getExtra().get(ExtraKeyConstant.SHIPMENT_TYPE), 3)) {
            // 分销自提
            yjErpShipmentInfo.setDelivery_type_id(7);
        }

        // 订单商品
        List<YJErpShipmentProductInfo> list = getYJErpShipmentProductInfo(shipmentDetail);
        yjErpShipmentInfo.setProduct_list(list);
        return yjErpShipmentInfo;
    }


    public List<YJErpShipmentProductInfo> getYJErpShipmentProductInfo(ShipmentDetail shipmentDetail) {
        Shipment shipment = shipmentDetail.getShipment();
        ShipmentExtra shipmentExtra = shipmentDetail.getShipmentExtra();
        Response<WarehouseDTO> rW  = warehouseClient.findById(shipmentExtra.getWarehouseId());
        if (!rW.isSuccess()){
            throw new ServiceException("find.warehouse.failed");
        }
        WarehouseDTO warehouse = rW.getResult();

        List<YJErpShipmentProductInfo> list = Lists.newArrayList();
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        shipmentItems.forEach(item -> {
            YJErpShipmentProductInfo yjErpShipmentProductInfo = new YJErpShipmentProductInfo();
            Response<List<SkuTemplate>> rS = skuTemplateReadService.findBySkuCodes(Lists.newArrayList(item.getSkuCode()));
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
            // 货号
            yjErpShipmentProductInfo.setGoods_code(materialCode);
            // 尺码
            String sizeName = "";
            List<SkuAttribute> skuAttributes = skuTemplate.getAttrs();
            for (SkuAttribute skuAttribute:skuAttributes){
                if (Objects.equals(skuAttribute.getAttrKey(),"尺码")){
                    sizeName = skuAttribute.getAttrVal();
                }
            }
            yjErpShipmentProductInfo.setSize(sizeName);
            // 库房code
            yjErpShipmentProductInfo.setWarehouse_code(warehouse.getOutCode());
            // 数量
            yjErpShipmentProductInfo.setNum(item.getQuantity());
            // 条码
            yjErpShipmentProductInfo.setBar_code(item.getSkuCode());
            list.add(yjErpShipmentProductInfo);
        });
        return list;
    }


    /**
     * 订单取消同步云聚ERP参数组装
     * @param shipment
     * @return
     */
    public List<YJErpCancelInfo> getSycYJErpCancelInfo (Shipment shipment) {
        List<YJErpCancelInfo> list = Lists.newArrayList();
        //获取发货单详情
        ShipmentDetail shipmentDetail = shipmentReadLogic.orderDetail(shipment.getId());
        list.add(convertYJErpCancelInfo(shipmentDetail));
        return list;
    }

    public YJErpCancelInfo convertYJErpCancelInfo (ShipmentDetail shipmentDetail) {
        Shipment shipment = shipmentDetail.getShipment();
        ShipmentExtra shipmentExtra = shipmentDetail.getShipmentExtra();
        YJErpCancelInfo yjErpCancelInfo = new YJErpCancelInfo();
        // 外部单号,中台发货单号
        yjErpCancelInfo.setOther_order_sn(shipment.getShipmentCode());
        Response<WarehouseDTO> rW  = warehouseClient.findById(shipmentExtra.getWarehouseId());
        if (!rW.isSuccess()){
            throw new ServiceException("find.warehouse.failed");
        }
        WarehouseDTO warehouse = rW.getResult();
        // 库房code
        yjErpCancelInfo.setWarehouse_code(warehouse.getOutCode());
        return yjErpCancelInfo;
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
     * 更新同步取消售后单到订单派发中心的发货单状态为失败
     * @param shipment
     */
    private void updateShipmetSyncCancelFail(Shipment shipment){
        //更新发货单的状态
        OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_CANCEL_FAIL.toOrderOperation();
        Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatusLocking(shipment, syncOrderOperation);
        if (!updateSyncStatusRes.isSuccess()) {
            //这里失败只打印日志即可
            log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
        }
    }

}
