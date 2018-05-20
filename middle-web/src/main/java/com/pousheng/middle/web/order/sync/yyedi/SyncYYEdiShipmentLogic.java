package com.pousheng.middle.web.order.sync.yyedi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.hksyc.component.SycHkOrderCancelApi;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.ShipmentDetail;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.HkPayType;
import com.pousheng.middle.order.enums.MiddlePayType;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.shop.dto.ShopExpresssCompany;
import com.pousheng.middle.shop.dto.ShopExtraInfo;
import com.pousheng.middle.shop.service.PsShopReadService;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.yyedisyc.component.SycYYEdiOrderCancelApi;
import com.pousheng.middle.yyedisyc.component.SycYYEdiShipmentOrderApi;
import com.pousheng.middle.yyedisyc.dto.YYEdiCancelResponse;
import com.pousheng.middle.yyedisyc.dto.YYEdiResponse;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiCancelInfo;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiShipmentInfo;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiShipmentItem;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 同步恒康发货单逻辑
 * Created by songrenfei on 2017/6/27
 */
@Slf4j
@Component
public class SyncYYEdiShipmentLogic {

    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private SycYYEdiShipmentOrderApi sycYYEdiShipmentOrderApi;
    @Autowired
    private SycYYEdiOrderCancelApi sycYYEdiOrderCancelApi;
    @Autowired
    private SycHkOrderCancelApi sycHkOrderCancelApi;
    @Autowired
    private WarehouseReadService warehouseReadService;
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
     * 同步发货单到YYEDI
     *
     * @param shipment 发货单
     * @return 同步成功true, 同步失败false
     */
    public Response<Boolean> syncShipmentToYYEdi(Shipment shipment) {
        try {
            //更新状态为同步中
            OrderOperation orderOperation = MiddleOrderEvent.SYNC_YYEDI.toOrderOperation();
            Response<Boolean> updateStatusRes = shipmentWiteLogic.updateStatus(shipment, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), orderOperation.getText(), updateStatusRes.getError());
                return Response.fail(updateStatusRes.getError());
            }

            Flow flow = flowPicker.pickShipments();
            Integer targetStatus = flow.target(shipment.getStatus(), orderOperation);
            shipment.setStatus(targetStatus);
            List<YYEdiShipmentInfo> list = this.makeShipmentOrderDtoList(shipment,shipment.getType());
            YYEdiResponse response  = JsonMapper.nonEmptyMapper().fromJson(sycYYEdiShipmentOrderApi.doSyncShipmentOrder(list),YYEdiResponse.class);
            if (Objects.equals(response.getErrorCode(),TradeConstants.YYEDI_RESPONSE_CODE_SUCCESS)){
                //整体成功
                OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_SUCCESS.toOrderOperation();
                Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(shipment, syncOrderOperation);
                if (!updateSyncStatusRes.isSuccess()) {
                    log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
                    return Response.fail(updateSyncStatusRes.getError());
                }
            }else{
               //整体失败
                OrderOperation syncOrderOperation = MiddleOrderEvent.SYNC_FAIL.toOrderOperation();
                Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(shipment, syncOrderOperation);
                if (!updateSyncStatusRes.isSuccess()) {
                    log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
                }
                return Response.fail(response.getFields().get(0).getErrorMsg());
            }
        } catch (Exception e) {
            log.error("sync yyedi shipment failed,shipmentId is({}) cause by({})", shipment.getId(), Throwables.getStackTraceAsString(e));
            //更新状态为同步失败
            updateShipmetSyncFail(shipment);
            return Response.fail("sync.yyedi.shipment.fail");
        }
        return Response.ok(Boolean.TRUE);
    }




    /**
     * 同步发货单取消订单派发中心
     * @param shipment 发货单
     * @return 同步结果, 同步成功true, 同步失败false
     */
    public Response<Boolean> syncShipmentCancelToYYEdi(Shipment shipment) {
        try {
            //更新状态为同步中
            OrderOperation orderOperation = MiddleOrderEvent.CANCEL_HK.toOrderOperation();
            Response<Boolean> updateStatusRes = shipmentWiteLogic.updateStatus(shipment, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), orderOperation.getText(), updateStatusRes.getError());
                return Response.fail(updateStatusRes.getError());
            }

            Flow flow = flowPicker.pickShipments();
            Integer targetStatus = flow.target(shipment.getStatus(), orderOperation);
            shipment.setStatus(targetStatus);//塞入最新的状态*/

            List<YYEdiCancelInfo> reqeustData = new ArrayList<>();
            YYEdiCancelInfo cancelShipmentInfo = new YYEdiCancelInfo();
            cancelShipmentInfo.setBillNo(String.valueOf(shipment.getShipmentCode()));
            reqeustData.add(cancelShipmentInfo);
            String response = sycYYEdiOrderCancelApi.doCancelOrder(reqeustData);
            YYEdiResponse yyEdiResponse = JsonMapper.nonEmptyMapper().fromJson(response,YYEdiResponse.class);
            //如果订单派发中心返回没有该订单，则直接取消成功
            if (Objects.equals(yyEdiResponse.getErrorCode(),TradeConstants.YYEDI_RESPONSE_CODE_SUCCESS)||Objects.equals(yyEdiResponse.getErrorCode(),TradeConstants.YYEDI_RESPONSE_NOT_EXIST_ORDER)) {
                OrderOperation operation = MiddleOrderEvent.SYNC_CANCEL_SUCCESS.toOrderOperation();
                Response<Boolean> updateStatus = shipmentWiteLogic.updateStatus(shipment, operation);
                if (!updateStatus.isSuccess()) {
                    log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), operation.getText(), updateStatus.getError());
                    return Response.fail(updateStatusRes.getError());
                }
            } else {
               //更新状态取消失败
                updateShipmetSyncCancelFail(shipment);
                return Response.fail("订单派发中心返回信息:"+yyEdiResponse.getDescription());
            }
        } catch (ServiceException e1) {
            log.error("sync yyedi shipment failed,shipmentId is({}) cause by({})", shipment.getId(), Throwables.getStackTraceAsString(e1));
            //更新状态取消失败
            updateShipmetSyncCancelFail(shipment);
            return Response.fail(e1.getMessage());
        } catch (Exception e) {
            log.error("sync yyedi shipment failed,shipmentId is({}) cause by({})", shipment.getId(), Throwables.getStackTraceAsString(e));
            //更新状态取消失败
            updateShipmetSyncCancelFail(shipment);
            return Response.fail("sync.yyedi.cancel.shipment.failed");
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
    public List<YYEdiShipmentInfo> makeShipmentOrderDtoList(Shipment shipment,int shipmentType) {
        //获取发货单详情
        ShipmentDetail shipmentDetail = shipmentReadLogic.orderDetail(shipment.getId());
        List<YYEdiShipmentInfo> list = new ArrayList<>();
        list.add(this.getSycYYEdiShipmentInfo(shipment,shipmentDetail,shipmentType));
        return list;
    }

    /**
     * 组装发往订单派发中心的发货单
     *
     * @param shipment
     * @param shipmentDetail
     * @param shipmentType 1.正常销售单，2.换货发货，3.补发
     * @return
     */
    private YYEdiShipmentInfo getSycYYEdiShipmentInfo(Shipment shipment, ShipmentDetail shipmentDetail,int shipmentType) {
        ShopOrder shopOrder = shipmentDetail.getShopOrder();
        ReceiverInfo receiverInfo = shipmentDetail.getReceiverInfo();
        ShipmentExtra shipmentExtra = shipmentDetail.getShipmentExtra();
        Response<Warehouse> rW  = warehouseReadService.findById(shipmentExtra.getWarehouseId());
        if (!rW.isSuccess()){
            throw new ServiceException("find.warehouse.failed");
        }
        Warehouse warehouse = rW.getResult();
        Map<String,String> warehouseExtraMap = warehouse.getExtra();
        YYEdiShipmentInfo shipmentInfo = new YYEdiShipmentInfo();
        //公司内码
        shipmentInfo.setCompanyCode(warehouse.getCompanyCode());
        //仓库
        shipmentInfo.setStockCode(warehouseExtraMap.get("outCode"));
        //erp单号，中台发货单代码
        shipmentInfo.setBillNo(String.valueOf(shipment.getShipmentCode()));
        //单据类型
        shipmentInfo.setBillType(TradeConstants.YYEDI_BILL_TYPE_ON_LINE);
        //来源单号
        if (Objects.equals(shipmentType,1)){
            shipmentInfo.setSourceBillNo("");
        }
        //换货单号
        if (Objects.equals(shipmentType,2)){
            OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
            Refund refund = refundReadLogic.findRefundById(orderShipment.getAfterSaleOrderId());
            RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
            Shipment sourceShipment = shipmentReadLogic.findShipmentByShipmentCode(refundExtra.getShipmentId());
            shipmentInfo.setSourceBillNo(sourceShipment.getShipmentCode());

        }
        //补发类型
        if (Objects.equals(shipmentType,3)){
            //获取该订单号下面的所有的销售发货单
            OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
            List<Shipment> shipments = shipmentReadLogic.findByShopOrderId(orderShipment.getOrderId());
            List<Shipment> shipmentList = shipments.stream().filter(Objects::nonNull)
                    .filter(it->!Objects.equals(it.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()))
                    .filter(it->Objects.equals(it.getType(), ShipmentType.SALES_SHIP.value())).collect(Collectors.toList());

            List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
            List<String> skuCodes = shipmentItems.stream().filter(Objects::nonNull).map(ShipmentItem::getSkuCode).collect(Collectors.toList());
            Set<String> shipmentIds = new HashSet<>();
            for (Shipment s:shipmentList){
                List<ShipmentItem> shipmentItemList = shipmentReadLogic.getShipmentItems(s);
                for (ShipmentItem item:shipmentItemList){
                    if (skuCodes.contains(item.getSkuCode())){
                        shipmentIds.add(String.valueOf(s.getShipmentCode()));
                    }
                }
            }
            StringBuffer stringBuffer = new StringBuffer();
            int count=0;
            for (String str:shipmentIds){
                if(count==(shipmentIds.size()-1)){
                    stringBuffer.append(str);
                }else{
                    stringBuffer.append(str).append(",");
                }
                count++;
            }
            shipmentInfo.setSourceBillNo(stringBuffer.toString());
        }
        //网店交易单号
        shipmentInfo.setShopBillNo(shopOrder.getOutId());
        //恒康店铺码--传外码
        shipmentInfo.setShopCode(shipmentExtra.getErpPerformanceShopOutCode());
        //恒康店铺名称
        shipmentInfo.setShopName(shipmentExtra.getErpPerformanceShopName());
        //出库单类型
        shipmentInfo.setRefundChangeType(shipmentType);
        //付款时间
        shipmentInfo.setPaymentDate(formatter.print(System.currentTimeMillis()));
        //快递方式
        shipmentInfo.setExpressType("Exress");
        //是否开发票
        shipmentInfo.setIsInvoice(0);
        //是否打印发票
        shipmentInfo.setIsPrintInvoice(0);
        //是否货票同行
        shipmentInfo.setIstrave(0);
        //发票抬头
        shipmentInfo.setInvoiceName("");
        //发票类型
        shipmentInfo.setInvoiceType(1);
        //增值税号
        shipmentInfo.setVATNumber("");
        //发票内容
        shipmentInfo.setInvoiceContent("");
        //电子发票邮箱
        shipmentInfo.setInvoiceEmail("");
        //收件人姓名
        shipmentInfo.setConsigneeName(receiverInfo.getReceiveUserName());
        //收件省
        shipmentInfo.setProvince(receiverInfo.getProvince());
        //收件市
        shipmentInfo.setCity(receiverInfo.getCity());
        //收件区
        shipmentInfo.setArea(receiverInfo.getRegion());
        //地址
        shipmentInfo.setAddress(receiverInfo.getDetail());
        //邮编
        shipmentInfo.setZipCode(receiverInfo.getPostcode());
        //收件人电话
        shipmentInfo.setBuyerTel(receiverInfo.getMobile());
        //手机号码
        shipmentInfo.setBuyerMobileTel(receiverInfo.getMobile());
        //寄件人姓名
        shipmentInfo.setSendContact("");
        //寄件人电话
        shipmentInfo.setSendContactTel("");
        //寄件省
        shipmentInfo.setSendProvince("");
        //寄件市
        shipmentInfo.setSendCity("");
        //寄件区
        shipmentInfo.setSendArea("");
        //寄件地址
        shipmentInfo.setSendAddress("");
        //买家用户名
        shipmentInfo.setBCMemberName(shopOrder.getBuyerName());
        //会员等级
        shipmentInfo.setBCMemberCard("");
        //支付类型在中台是1:在线支付,2:货到付款,同步给订单派发中心时时需要变为0:在线支付,1:货到付款
        shipmentInfo.setPaymenttype(this.getYYEdiPayType(shipmentDetail).getValue());
        if (Objects.equals(shipmentInfo.getPaymenttype(),HkPayType.HK_CASH_ON_DELIVERY.getValue())){
            //代收金额:商品总金额+运费
            shipmentInfo.setCollectionAmount(new BigDecimal(shipmentDetail.getShopOrder().getFee()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN));
            //结算金额
            shipmentInfo.setPayAmount(new BigDecimal(shipmentDetail.getShopOrder().getFee()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN));
        }else{
            shipmentInfo.setCollectionAmount(new BigDecimal(0.00));
            //结算金额
            shipmentInfo.setPayAmount(new BigDecimal(shipmentDetail.getShipmentExtra().getShipmentTotalPrice()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN));
        }
        //买家邮费
        shipmentInfo.setExpressAmount(new BigDecimal(shipmentDetail.getShipmentExtra().getShipmentShipFee()-shipmentDetail.getShipmentExtra().getShipmentShipDiscountFee()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN));
        //线上实付金额
        if (Objects.equals(shipmentInfo.getPaymenttype(),HkPayType.HK_CASH_ON_DELIVERY.getValue())) {
            shipmentInfo.setPayAmountBakUp(new BigDecimal(0.00));
        }else{
            shipmentInfo.setPayAmountBakUp(new BigDecimal(shipmentDetail.getShipmentExtra().getShipmentTotalPrice()).divide(new BigDecimal(100), 2, RoundingMode.HALF_DOWN));
        }
        //红包支付金额
        shipmentInfo.setRptAmount(new BigDecimal(0.00));
        //促销优惠金额
        shipmentInfo.setPromZRAmount(new BigDecimal(0.00));
        //运费到付
        if (Objects.equals(shipmentInfo.getPaymenttype(),HkPayType.HK_CASH_ON_DELIVERY.getValue())){
            if (Objects.equals(shipmentInfo.getExpressAmount().toString(),"0.00")){
                //货到付款运费金额为0则运费到付传0
                shipmentInfo.setFreightPay(0);
            }else{
                //货到付款运费金额不为0则运费到付传1
                shipmentInfo.setFreightPay(1);
            }
        }else{
            //在线支付传0
            shipmentInfo.setFreightPay(0);
        }
        shipmentInfo.setFreightPay(this.getYYEdiPayType(shipmentDetail).getValue()==1?1:0);
        //查询店铺相关信息-获取下单店铺公司码
        Response<OpenShop> r = openShopReadService.findById(shipment.getShopId());
        Map<String,String> openShopExtra = r.getResult().getExtra();
        String shopCompanyCode =openShopExtra.get(TradeConstants.HK_COMPANY_CODE);
        shipmentInfo.setShopCompanyCode(shopCompanyCode);
        //渠道
        Map<String,String> shopOrderExtra = shopOrder.getExtra();

        String isAssignShop = shopOrderExtra.get("isAssignShop");
        if (!Objects.isNull(isAssignShop)){
            shipmentInfo.setChannel("mpos");
            String shopOutCode =openShopExtra.get(TradeConstants.HK_PERFORMANCE_SHOP_OUT_CODE);
            String companyCode = openShopExtra.get(TradeConstants.HK_COMPANY_CODE);
            Response<com.google.common.base.Optional<Shop>> shopOptionalResponse =  psShopReadService.findByOuterIdAndBusinessId(shopOutCode,Long.valueOf(companyCode));
            if (!shopOptionalResponse.isSuccess()){
                log.error("find parana shop failed,shopOutCode is {},companyCode is {},caused by {}",shopOutCode,companyCode,shopOptionalResponse.getError());
                //客户供应商快递代码
                shipmentInfo.setCustomerCode(shipmentExtra.getVendCustID());
                //客户供应商快递公司名称
                shipmentInfo.setFreightCompany(shipmentExtra.getVendCustID());
            }else{
                com.google.common.base.Optional<Shop> shopOptional = shopOptionalResponse.getResult();
                if (shopOptional.isPresent()){
                    Shop shop = shopOptional.get();
                    ShopExtraInfo existShopExtraInfo = ShopExtraInfo.fromJson(shop.getExtra());
                    if (existShopExtraInfo != null) {
                        if(!CollectionUtils.isEmpty(existShopExtraInfo.getExpresssCompanyList())){
                            ShopExpresssCompany shopExpresssCompany = existShopExtraInfo.getExpresssCompanyList().get(0);
                            ExpressCode expressCode = orderReadLogic.makeExpressNameByMposCode(shopExpresssCompany.getCode());
                            //客户供应商快递代码
                            shipmentInfo.setCustomerCode(expressCode.getHkCode());
                            //客户供应商快递公司名称
                            shipmentInfo.setFreightCompany(expressCode.getHkCode());
                        }
                    }
                }else{
                    //客户供应商快递代码
                    shipmentInfo.setCustomerCode(shipmentExtra.getVendCustID());
                    //客户供应商快递公司名称
                    shipmentInfo.setFreightCompany(shipmentExtra.getVendCustID());
                }
            }
        }else{
            if (Objects.equals("suning-sale",shopOrder.getOutFrom())){
                shipmentInfo.setChannel("suning");
            }else{
                shipmentInfo.setChannel(shopOrder.getOutFrom());
            }
            //客户供应商快递代码
            shipmentInfo.setCustomerCode(shipmentExtra.getVendCustID());
            //客户供应商快递公司名称
            shipmentInfo.setFreightCompany(shipmentExtra.getVendCustID());
        }
        //获取发货单中对应的sku列表
        //积分
        int integral = 0;
        List<YYEdiShipmentItem> items = this.getSyncYYEdiShipmentItem(shipment, shipmentDetail,integral);
        int quantity = 0;
        for (YYEdiShipmentItem item:items){
            quantity = quantity +item.getExpectQty();
        }
        //会员兑换积分
        shipmentInfo.setExchangeIntegral(new BigDecimal(integral));
        //总行数
        shipmentInfo.setTdq(items.size());
        //预期数量
        shipmentInfo.setExpectQty(quantity);
        shipmentInfo.setItems(items);
        return shipmentInfo;
    }

    /**
     * 组装发往订单派发中心的的发货单商品列表
     *
     * @param shipment
     * @param shipmentDetail
     * @param integral 积分
     * @return
     */
    private List<YYEdiShipmentItem> getSyncYYEdiShipmentItem(Shipment shipment, ShipmentDetail shipmentDetail,int integral) {
        List<ShipmentItem> shipmentItems = shipmentDetail.getShipmentItems();
        ShipmentExtra shipmentExtra = shipmentDetail.getShipmentExtra();
        Response<Warehouse> rW  = warehouseReadService.findById(shipmentExtra.getWarehouseId());
        if (!rW.isSuccess()){
            throw new ServiceException("");
        }
        Warehouse warehouse = rW.getResult();
        List<YYEdiShipmentItem> items = Lists.newArrayListWithCapacity(shipmentItems.size());
        for (ShipmentItem shipmentItem : shipmentItems) {
            YYEdiShipmentItem item = new YYEdiShipmentItem();
            //公司内码
            item.setCompanyCode(warehouse.getCompanyCode());
            //ERP单号
            item.setBillNo(String.valueOf(shipment.getShipmentCode()));
            //中台sku
            item.setSKU(shipmentItem.getSkuCode());
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
            item.setMaterialCode(materialCode);
            //尺码名称
            String sizeName = "";
            List<SkuAttribute> skuAttributes = skuTemplate.getAttrs();
            for (SkuAttribute skuAttribute:skuAttributes){
                if (Objects.equals(skuAttribute.getAttrKey(),"尺码")){
                    sizeName = skuAttribute.getAttrVal();
                }
            }
            item.setSizeName(sizeName);

            //中台尺码id
            String sizeId = extraMaps.get(TradeConstants.HK_SIZE_ID);
            item.setSizeCode(sizeId);

            item.setExpectQty(shipmentItem.getQuantity());
            //销售单价(减去所有的优惠(优惠需要按比例计算))
            item.setBalaPrice(new BigDecimal(shipmentItem.getCleanPrice()==null?0:shipmentItem.getCleanPrice()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN));
            //总价(销售价格*数量)
            item.setPayAmount(new BigDecimal(shipmentItem.getCleanFee()==null?0:shipmentItem.getCleanFee()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN));
            //吊牌价
            Map<String,Integer> extraPrice = skuTemplate.getExtraPrice();
            int originPrice = 0;
            if (extraPrice!=null){
                originPrice = extraPrice.get("originPrice")==null?0:extraPrice.get("originPrice");
            }
            item.setRetailPrice(new BigDecimal(originPrice).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN));
            //积分
            integral = shipmentItem.getIntegral()==null?0:shipmentItem.getIntegral()+integral;
            items.add(item);
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
        Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(shipment, syncOrderOperation);
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
        Response<Boolean> updateSyncStatusRes = shipmentWiteLogic.updateStatus(shipment, syncOrderOperation);
        if (!updateSyncStatusRes.isSuccess()) {
            //这里失败只打印日志即可
            log.error("shipment(id:{}) operation :{} fail,error:{}", shipment.getId(), syncOrderOperation.getText(), updateSyncStatusRes.getError());
        }
    }

}
