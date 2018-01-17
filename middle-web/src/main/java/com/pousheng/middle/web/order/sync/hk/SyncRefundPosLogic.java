package com.pousheng.middle.web.order.sync.hk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.pousheng.middle.hksyc.pos.api.SycHkShipmentPosApi;
import com.pousheng.middle.hksyc.pos.dto.*;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.RefundItem;
import com.pousheng.middle.order.dto.ShipmentDetail;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.order.dto.OpenClientPaymentInfo;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.common.constants.JacksonType;
import io.terminus.parana.order.model.Invoice;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 同步恒康逆向订单逻辑 for 开pos单
 * Created by songrenfei on 2017/6/27
 */
@Slf4j
@Component
public class SyncRefundPosLogic {

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private ShopCacher shopCacher;
    @Autowired
    private SycHkShipmentPosApi sycHkShipmentPosApi;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @RpcConsumer
    private ShopReadService shopReadService;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private RefundReadLogic refundReadLogic;

    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 同步恒康退货单
     *
     * @param refund 退货单
     * @return 同步结果 result 为恒康的退货单编号
     */
    public Response<Boolean> syncRefundPosToHk(Refund refund) {
        try {


            HkShipmentPosRequestData requestData = makeHkShipmentPosRequestData(refund);
            String url ="/common/erp/pos/addnetsalreturnstock";
            String result = sycHkShipmentPosApi.doSyncRefundPos(requestData,url);
            SycShipmentPosResponse response = JsonMapper.nonEmptyMapper().fromJson(result,SycShipmentPosResponse.class);
            if(!Objects.equal(response.getCode(),"00000")){
                log.error("sync shipment pos to hk fail,error:{}",response.getMessage());
                return Response.fail(response.getMessage());
            }

            return Response.ok();
        } catch (Exception e) {
            log.error("sync hk pos refund failed,shipmentId is({}) cause by({})", refund.getId(), e.getMessage());
            return Response.fail("sync.hk.pos.refund.fail");
        }

    }



    private HkShipmentPosRequestData makeHkShipmentPosRequestData(Refund refund){

        HkShipmentPosRequestData requestData = new HkShipmentPosRequestData();
        requestData.setTranReqDate(formatter.print(new Date().getTime()));
        requestData.setSid("PS_ERP_POS_netsalreturnstock");
        HkShipmentPosContent bizContent = makeHkShipmentPosContent(refund);
        requestData.setBizContent(bizContent);
        return requestData;
    }

    private HkShipmentPosContent makeHkShipmentPosContent(Refund refund) {

        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        if(Arguments.isNull(refundExtra.getShipmentId())){
            log.error("refund(id:{}) shipment id invalid");
            throw new ServiceException("refund.shipment.id.invalid");
        }
        //获取发货单详情
        ShipmentDetail shipmentDetail = shipmentReadLogic.orderDetail(refundExtra.getShipmentId());
        HkShipmentPosContent posContent = new HkShipmentPosContent();

        posContent.setChanneltype("b2c");//订单来源类型, 是b2b还是b2c

        //退货仓
        Warehouse warehouse = warehouseCacher.findById(refundExtra.getWarehouseId());
        Map<String, String>  extra = warehouse.getExtra();
        if(CollectionUtils.isEmpty(extra)||!extra.containsKey("outCode")){
            log.error("warehouse(id:{}) out code invalid",warehouse.getId());
            throw new ServiceException("warehouse.out.code.invalid");
        }
        posContent.setNetcompanyid(warehouse.getCompanyId());//线上店铺所属公司id
        posContent.setNetshopcode(extra.get("outCode"));//线上店铺code

        posContent.setNetstockcode("MPOSEDI");//todo 线上店铺所属公司的虚拟仓代码
        posContent.setNetbillno(refund.getId().toString());//端点唯一订单号
        posContent.setSourcebillno("");//订单来源单号
        posContent.setBilldate(formatter.print(refund.getCreatedAt().getTime()));//订单日期
        posContent.setOperator("MPOS_EDI");//线上店铺帐套操作人code
        posContent.setRemark(refund.getBuyerNote());//备注

        HkShipmentPosInfo netsalorder = makeHkShipmentPosInfo(shipmentDetail,refund);
        List<HkShipmentPosItem> ordersizes = makeHkShipmentPosItem(refund);
        posContent.setNetsalorder(netsalorder);
        posContent.setOrdersizes(ordersizes);

        return posContent;
    }

    private List<HkShipmentPosItem> makeHkShipmentPosItem(Refund refund) {

        List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        List<HkShipmentPosItem> posItems = Lists.newArrayListWithCapacity(refundItems.size());
        for (RefundItem refundItem : refundItems){
            HkShipmentPosItem hkShipmentPosItem = new HkShipmentPosItem();
            hkShipmentPosItem.setMatbarcode(refundItem.getSkuCode());
            hkShipmentPosItem.setQty(refundItem.getQuantity());
            hkShipmentPosItem.setBalaprice(new BigDecimal(refundItem.getFee()==null?0:refundItem.getFee()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString());
            posItems.add(hkShipmentPosItem);
        }
        return posItems;
    }

    private HkShipmentPosInfo makeHkShipmentPosInfo(ShipmentDetail shipmentDetail,Refund refund) {

        ShopOrder shopOrder = shipmentDetail.getShopOrder();
        OpenClientPaymentInfo openClientPaymentInfo = orderReadLogic.getOpenClientPaymentInfo(shopOrder);
        ReceiverInfo receiverInfo = shipmentDetail.getReceiverInfo();
        ShipmentExtra shipmentExtra = shipmentDetail.getShipmentExtra();
        List<Invoice> invoices = shipmentDetail.getInvoices();

        HkShipmentPosInfo posInfo = new HkShipmentPosInfo();
        posInfo.setBuyeralipayno(""); //支付宝账号
        posInfo.setAlipaybillno(""); //支付交易号
        posInfo.setSourceremark(""); //订单来源说明
        posInfo.setOrdertype("");  //订单类型
        posInfo.setOrdercustomercode(""); //订单标记code
        posInfo.setAppamtsourceshop(""); //业绩来源店铺
        posInfo.setPaymentdate(formatter.print(openClientPaymentInfo.getPaidAt().getTime())); //付款时间
        posInfo.setCardcode(""); //会员卡号
        posInfo.setBuyercode(shopOrder.getBuyerName());//买家昵称
        posInfo.setBuyermobiletel(shopOrder.getOutBuyerId()); //买家手机
        posInfo.setBuyertel(""); //买家座机
        posInfo.setBuyerremark(shopOrder.getBuyerNote()); //买家留言
        posInfo.setConsigneename(receiverInfo.getReceiveUserName());//收货人姓名
        posInfo.setPayamountbakup(new BigDecimal(refund.getFee()==null?0:refund.getFee()).divide(new BigDecimal(100),2, RoundingMode.HALF_DOWN).toString()); //线上实付金额
        //posInfo.setExpresscost(new BigDecimal(shipmentExtra.getShipmentShipFee()==null?0:shipmentExtra.getShipmentShipFee()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString());//邮费成本
        posInfo.setCodcharges("0");//货到付款服务费
        posInfo.setPreremark(""); //优惠信息
        if(CollectionUtils.isEmpty(invoices)){
            posInfo.setIsinvoice("0"); //是否开票

        }else {
            posInfo.setIsinvoice("1"); //是否开票
            posInfo.setInvoice_name(invoices.get(0).getTitle()); //发票抬头
            posInfo.setTaxno("");//税号
        }

        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        //卖家备注
        String sellerNote =refund.getSellerNote();
        posInfo.setProvince(receiverInfo.getProvince()); //省
        posInfo.setCity(receiverInfo.getCity()); //市
        posInfo.setArea(receiverInfo.getRegion()); //区
        posInfo.setZipcode(receiverInfo.getPostcode());//邮政编码
        posInfo.setAddress(receiverInfo.getDetail()); //详细地址
        posInfo.setSellremark(sellerNote);//卖家备注
        posInfo.setSellcode(shopOrder.getBuyerName()); //卖家昵称
        posInfo.setExpresstype("express");//物流方式
        posInfo.setVendcustcode(refundExtra.getShipmentCorpCode());  //物流公司代码 寄回物流公司代码
        posInfo.setExpressbillno(refundExtra.getShipmentSerialNo()); //物流单号 寄回物流单号
        posInfo.setWms_ordercode(""); //第三方物流单号
        if(Arguments.isNull(shipmentExtra.getShipmentDate())){
            posInfo.setConsignmentdate(formatter.print(new Date().getTime())); //发货时间
        }else {
            posInfo.setConsignmentdate(formatter.print(shipmentExtra.getShipmentDate().getTime())); //发货时间

        }

        return posInfo;
    }

    private String getBillNo(String data) throws Exception{
        Map<String,String> map =objectMapper.readValue(data, JacksonType.MAP_OF_STRING);
        if(!CollectionUtils.isEmpty(map)&&map.containsKey("billNo")){
            return map.get("billNo");
        } else{
            throw new ServiceException("hk.result.bill.no.invalid");
        }
    }

    private Boolean isWarehouseShip(String shipmentWay){
        if(Objects.equal(shipmentWay,"2")){
            return Boolean.TRUE;
        }else {
            return Boolean.FALSE;
        }
    }

}
