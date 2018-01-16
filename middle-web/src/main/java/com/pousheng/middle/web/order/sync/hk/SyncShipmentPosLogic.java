package com.pousheng.middle.web.order.sync.hk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.pousheng.middle.hksyc.pos.api.SycHkShipmentPosApi;
import com.pousheng.middle.hksyc.pos.dto.*;
import com.pousheng.middle.order.dto.ShipmentDetail;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.shop.dto.ShopExtraInfo;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.order.dto.OpenClientPaymentInfo;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.order.model.*;
import io.terminus.parana.shop.model.Shop;
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
 * 同步恒康发货单逻辑 for 开pos单
 * Created by songrenfei on 2017/6/27
 */
@Slf4j
@Component
public class SyncShipmentPosLogic {

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private ShopCacher shopCacher;
    @Autowired
    private SycHkShipmentPosApi sycHkShipmentPosApi;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @RpcConsumer
    private ShopReadService shopReadService;

    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    /**
     * 同步发货单到恒康
     *
     * @param shipment 发货单
     * @return 同步成功true, 同步失败false
     */
    public Response<Boolean> syncShipmentPosToHk(Shipment shipment) {
        try {
            HkShipmentPosRequestData requestData = makeHkShipmentPosRequestData(shipment);
            String result = sycHkShipmentPosApi.doSyncShipmentPos(requestData);
            SycShipmentPosResponse response = JsonMapper.nonEmptyMapper().fromJson(result,SycShipmentPosResponse.class);
            if(Objects.equal(response.getCode(),"00000")){
                log.error("sync shipment pos to hk fail,error:{}",response.getMessage());
                return Response.fail(response.getMessage());
            }
            return Response.ok();
        } catch (Exception e) {
            log.error("sync hk pos shipment failed,shipmentId is({}) cause by({})", shipment.getId(), e.getMessage());
            return Response.fail("sync.hk.pos.shipment.fail");
        }

    }


    private HkShipmentPosRequestData makeHkShipmentPosRequestData(Shipment shipment){
        HkShipmentPosRequestData requestData = new HkShipmentPosRequestData();
        requestData.setTranReqDate(formatter.print(new Date().getTime()));
        requestData.setSid("PS_ERP_POS_netsalshop");//店发
        HkShipmentPosContent bizContent = makeHkShipmentPosContent(shipment);
        requestData.setBizContent(bizContent);
        return requestData;
    }

    private HkShipmentPosContent makeHkShipmentPosContent(Shipment shipment) {

        //获取发货单详情
        ShipmentDetail shipmentDetail = shipmentReadLogic.orderDetail(shipment.getId());
        ShopOrder shopOrder = shipmentDetail.getShopOrder();
        ShipmentExtra shipmentExtra = shipmentDetail.getShipmentExtra();
        String shipmentWay = shipmentExtra.getShipmentWay();
        if(Objects.equal(shipmentWay,"2")){
            log.error("current shipment(id:{}) is warehouse shipment,so skip");
            throw new ServiceException("warehouse.shipment");
        }

        HkShipmentPosContent posContent = new HkShipmentPosContent();

        posContent.setChanneltype("b2c");//订单来源类型, 是b2b还是b2c
        Shop receivershop = shopCacher.findShopById(shipmentExtra.getWarehouseId());
        ShopExtraInfo receiverShopExtraInfo = ShopExtraInfo.fromJson(receivershop.getExtra());
        posContent.setCompanyid(receiverShopExtraInfo.getCompanyId().toString());//实际发货账套id
        posContent.setShopcode(receivershop.getOuterId());//实际发货店铺code
            posContent.setVoidstockcode("WH110010");//todo 实际发货账套的虚拟仓代码

        OpenShop openShop = orderReadLogic.findOpenShopByShopId(shipment.getShopId());
        Response<Shop> shopRes = shopReadService.findByOuterId(openShop.getAppKey());
        if(!shopRes.isSuccess()){
            log.error("find shop by outer id:{} fail,error:{}",openShop.getAppKey(),shopRes.getError());
            throw new ServiceException(shopRes.getError());
        }
        Shop shop = shopRes.getResult();
        ShopExtraInfo shopExtraInfo = ShopExtraInfo.fromJson(shop.getExtra());
        posContent.setNetcompanyid(shopExtraInfo.getCompanyId().toString());//线上店铺所属公司id
        posContent.setNetshopcode(shop.getOuterId());//线上店铺code
        posContent.setNetstockcode("WH110011");//todo 线上店铺所属公司的虚拟仓代码
        posContent.setNetbillno(shipment.getId().toString());//端点唯一订单号
        posContent.setSourcebillno("");//订单来源单号
        posContent.setBilldate(formatter.print(shopOrder.getOutCreatedAt().getTime()));//订单日期
        posContent.setOperator("MPOS_EDI");//线上店铺帐套操作人code
        posContent.setRemark(shopOrder.getBuyerNote());//备注


        HkShipmentPosInfo netsalorder = makeHkShipmentPosInfo(shipmentDetail);
        List<HkShipmentPosItem> ordersizes = makeHkShipmentPosItem(shipmentDetail);
        posContent.setNetsalorder(netsalorder);
        posContent.setOrdersizes(ordersizes);

        return posContent;
    }

    private List<HkShipmentPosItem> makeHkShipmentPosItem(ShipmentDetail shipmentDetail) {
        List<ShipmentItem> shipmentItems = shipmentDetail.getShipmentItems();
        List<HkShipmentPosItem> posItems = Lists.newArrayListWithCapacity(shipmentItems.size());
        for (ShipmentItem shipmentItem : shipmentItems){
            HkShipmentPosItem hkShipmentPosItem = new HkShipmentPosItem();
            hkShipmentPosItem.setMatbarcode(shipmentItem.getSkuCode());
            hkShipmentPosItem.setQty(shipmentItem.getQuantity());
            hkShipmentPosItem.setBalaprice(new BigDecimal(shipmentItem.getCleanFee()==null?0:shipmentItem.getCleanFee()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString());
            posItems.add(hkShipmentPosItem);
        }
        return posItems;
    }

    private HkShipmentPosInfo makeHkShipmentPosInfo(ShipmentDetail shipmentDetail) {

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
        posInfo.setPayamountbakup(new BigDecimal(shipmentExtra.getShipmentTotalFee()==null?0:shipmentExtra.getShipmentTotalFee()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString()); //线上实付金额
        posInfo.setExpresscost(new BigDecimal(shipmentExtra.getShipmentShipFee()==null?0:shipmentExtra.getShipmentShipFee()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString());//邮费成本
        posInfo.setCodcharges("0");//货到付款服务费
        posInfo.setPreremark(""); ; //优惠信息
        if(CollectionUtils.isEmpty(invoices)){
            posInfo.setIsinvoice("0"); //是否开票

        }else {
            posInfo.setIsinvoice("1"); //是否开票
            posInfo.setInvoice_name(invoices.get(0).getTitle()); //发票抬头
            posInfo.setTaxno("");//税号
        }

        Map<String,String> shopOrderExtra = shopOrder.getExtra();
        //卖家备注
        String sellerNote ="";
        if(!CollectionUtils.isEmpty(shopOrderExtra)&&shopOrderExtra.containsKey("customerServiceNote")){
            sellerNote = shopOrderExtra.get("customerServiceNote");
        }

        posInfo.setProvince(receiverInfo.getProvince()); //省
        posInfo.setCity(receiverInfo.getCity()); //市
        posInfo.setArea(receiverInfo.getRegion()); //区
        posInfo.setZipcode(receiverInfo.getPostcode());//邮政编码
        posInfo.setAddress(receiverInfo.getDetail()); //详细地址
        posInfo.setSellremark(sellerNote);//卖家备注
        posInfo.setSellcode(shopOrder.getBuyerName()); //卖家昵称
        posInfo.setExpresstype("express");//物流方式
        posInfo.setVendcustcode(shipmentExtra.getShipmentCorpCode());  //物流公司代码
        posInfo.setExpressbillno(shipmentExtra.getShipmentSerialNo()); //物流单号
        posInfo.setWms_ordercode(""); //第三方物流单号
        if(Arguments.isNull(shipmentExtra.getShipmentDate())){
            posInfo.setConsignmentdate(formatter.print(new Date().getTime())); //发货时间
        }else {
            posInfo.setConsignmentdate(formatter.print(shipmentExtra.getShipmentDate().getTime())); //发货时间

        }

        return posInfo;
    }


}
