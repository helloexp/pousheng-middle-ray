package com.pousheng.middle.web.order.sync.hk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.pousheng.middle.hksyc.pos.api.SycHkShipmentPosApi;
import com.pousheng.middle.hksyc.pos.dto.*;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentDetail;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import com.pousheng.middle.web.user.component.UcUserOperationLogic;
import com.pousheng.middle.web.user.dto.MemberProfile;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.order.dto.OpenClientPaymentInfo;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.common.constants.JacksonType;
import io.terminus.parana.order.model.Invoice;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

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
    private UcUserOperationLogic ucUserOperationLogic;

    @Value("${pos.stock.code}")
    private String posStockCode;

    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();
    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();
    private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    /**
     * 同步发货单到恒康
     *
     * @param shipment 发货单
     * @return 同步成功true, 同步失败false
     */
    public Response<Boolean> syncShipmentPosToHk(Shipment shipment) {
        try {
            //获取发货单详情
            ShipmentDetail shipmentDetail = shipmentReadLogic.orderDetail(shipment.getId());
            ShipmentExtra shipmentExtra = shipmentDetail.getShipmentExtra();
            String shipmentWay = StringUtils.isEmpty(shipmentExtra.getShipmentWay())?"2":shipmentExtra.getShipmentWay();
            if(Strings.isNullOrEmpty(shipmentWay)){
                log.error("shipment(id:{}) shipment way invalid",shipment.getId());
                throw new ServiceException("shipment.way.invalid");
            }

            HkShipmentPosRequestData requestData = makeHkShipmentPosRequestData(shipmentDetail,shipmentWay);
            String url ="/common/erp/pos/addnetsalshop";
            if(isWarehouseShip(shipmentWay)){
                url="/common/erp/pos/addnetsalstock";
            }
            String result = sycHkShipmentPosApi.doSyncShipmentPos(requestData,url);
            log.info("sync shipment pos to hk,response:{}",result);
            SycShipmentPosResponse response = JsonMapper.nonEmptyMapper().fromJson(result,SycShipmentPosResponse.class);
            if(!Objects.equal(response.getCode(),"00000")){
                log.error("sync shipment(code:{}) shipment pos to hk fail,error:{}",shipment.getShipmentCode(),response.getMessage());
                return Response.fail(response.getMessage());
            }

            //网店零售订单号
            String billNo = getBillNo(response.getReturnJson());
            shipmentExtra.setHkResaleOrderId(billNo);
            Map<String,String> extraMap = shipment.getExtra();
            extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO,JSON_MAPPER.toJson(shipmentExtra));
            //更新extra
            shipmentWiteLogic.updateExtra(shipment.getId(),extraMap);


            return Response.ok();
        } catch (Exception e) {
            log.error("sync hk pos shipment failed,shipmentId is({}) cause by({})", shipment.getId(), e.getMessage());
            return Response.fail("sync.hk.pos.shipment.fail");
        }

    }



    /**
     * 同步发货单收货信息到恒康
     *
     * @param shipment 发货单
     * @return 同步成功true, 同步失败false
     */
    public Response<Boolean> syncShipmentDoneToHk(Shipment shipment) {
        try {

            String url ="/common/erp/pos/updatenetsalreceiptdate";
            HkShimentDoneRequestData requestData = new HkShimentDoneRequestData();
            requestData.setTranReqDate(formatter.print(System.currentTimeMillis()));

            HkShimentDoneInfo doneInfo = new HkShimentDoneInfo();
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            if(Strings.isNullOrEmpty(shipmentExtra.getHkResaleOrderId())||Arguments.isNull(shipment.getConfirmAt())){
                log.error("shipment(id:{}) sync hk confirm fail,param invalid",shipment.getId());
                return Response.fail("shipment.confirm.param.invalid");
            }
            doneInfo.setNetbillno(shipment.getShipmentCode());
            doneInfo.setReceiptdate(formatter.print(shipment.getConfirmAt().getTime()));

            requestData.setBizContent(Lists.newArrayList(doneInfo));

            String result = sycHkShipmentPosApi.doSyncShipmentDone(requestData,url);
            SycShipmentPosResponse response = JsonMapper.nonEmptyMapper().fromJson(result,SycShipmentPosResponse.class);
            if(!Objects.equal(response.getCode(),"00000")){
                log.error("sync shipment confirm at to hk fail,error:{}",response.getMessage());
                return Response.fail(response.getMessage());
            }
            return Response.ok();
        } catch (Exception e) {
            log.error("sync hk shipment confirm at failed,shipmentId is({}) cause by({})", shipment.getId(), e.getMessage());
            return Response.fail("sync.hk.shipment.confirm.at.fail");
        }

    }



    private HkShipmentPosRequestData makeHkShipmentPosRequestData(ShipmentDetail shipmentDetail,String shipmentWay){


        HkShipmentPosRequestData requestData = new HkShipmentPosRequestData();
        requestData.setTranReqDate(formatter.print(System.currentTimeMillis()));
        if(isWarehouseShip(shipmentWay)){
            log.info("current shipment(id:{}) is warehouse shipment");
            requestData.setSid("PS_ERP_POS_netsalstock");//仓发
        }else {
            requestData.setSid("PS_ERP_POS_netsalshop");//店发
        }
        HkShipmentPosContent bizContent = makeHkShipmentPosContent(shipmentDetail,shipmentWay);
        requestData.setBizContent(bizContent);
        return requestData;
    }

    private HkShipmentPosContent makeHkShipmentPosContent(ShipmentDetail shipmentDetail,String shipmentWay) {


        Shipment shipment = shipmentDetail.getShipment();
        ShopOrder shopOrder = shipmentDetail.getShopOrder();
        ShipmentExtra shipmentExtra = shipmentDetail.getShipmentExtra();
        HkShipmentPosContent posContent = new HkShipmentPosContent();

        posContent.setChanneltype("b2c");//订单来源类型, 是b2b还是b2c

        if(isWarehouseShip(shipmentWay)){
            Warehouse warehouse = warehouseCacher.findById(shipmentExtra.getWarehouseId());
            Map<String, String>  extra = warehouse.getExtra();
            if(CollectionUtils.isEmpty(extra)||!extra.containsKey("outCode")){
                log.error("warehouse(id:{}) out code invalid",warehouse.getId());
                throw new ServiceException("warehouse.out.code.invalid");
            }
            posContent.setCompanyid(warehouse.getCompanyId());//实际发货账套id
            posContent.setStockcode(extra.get("outCode"));//实际发货店铺code

            //下单店
            OpenShop openShop = orderReadLogic.findOpenShopByShopId(shipment.getShopId());
            Map<String,String> extraMap = openShop.getExtra();
            String companyId = extraMap.get("companyCode");
            String code = extraMap.get("hkPerformanceShopOutCode");

            posContent.setNetcompanyid(companyId);//线上店铺所属公司id
            posContent.setNetshopcode(code);//线上店铺code
        }else {
            Shop receivershop = shopCacher.findShopById(shipmentExtra.getWarehouseId());
            posContent.setCompanyid(receivershop.getBusinessId().toString());//实际发货账套id
            posContent.setShopcode(receivershop.getOuterId());//实际发货店铺code

            //下单店
            OpenShop openShop = orderReadLogic.findOpenShopByShopId(shipment.getShopId());
            Map<String,String> extraMap = openShop.getExtra();
            String companyId = extraMap.get("companyCode");
            String code = extraMap.get("hkPerformanceShopOutCode");

            posContent.setNetcompanyid(companyId);//线上店铺所属公司id
            posContent.setNetshopcode(code);//线上店铺code
        }
        posContent.setVoidstockcode(posStockCode);//todo 实际发货账套的虚拟仓代码


        posContent.setNetstockcode(posStockCode);//todo 线上店铺所属公司的虚拟仓代码
        posContent.setNetbillno(shipment.getShipmentCode());//端点唯一订单号
        Map<String,String> shopOrderExtra = shopOrder.getExtra();
        String isHkPosOrder = shopOrderExtra.get("isHkPosOrder");
        if (!StringUtils.isEmpty(isHkPosOrder)&&Objects.equal(isHkPosOrder,"true")){
            String outOrderId = shopOrderExtra.get("hkOutOrderId");
            posContent.setSourcebillno(outOrderId==null?"":outOrderId);//订单来源单号
        }else{
            posContent.setSourcebillno("");//订单来源单号
        }
        posContent.setBilldate(formatter.print(shopOrder.getOutCreatedAt().getTime()));//订单日期
        posContent.setOperator("MPOS_EDI");//线上店铺帐套操作人code
        posContent.setRemark(shopOrder.getBuyerNote());//备注

        HkShipmentPosInfo netsalorder = makeHkShipmentPosInfo(shipmentDetail,shipmentWay);
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
            hkShipmentPosItem.setBalaprice(new BigDecimal(shipmentItem.getCleanPrice()==null?0:shipmentItem.getCleanPrice()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString());
            posItems.add(hkShipmentPosItem);
        }
        return posItems;
    }

    private HkShipmentPosInfo makeHkShipmentPosInfo(ShipmentDetail shipmentDetail,String shipmentWay) {

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
        if (java.util.Objects.isNull(openClientPaymentInfo)){
            //如果拉取不到付款信息，则使用中台订单创建的时间
            posInfo.setPaymentdate(formatter.print(shopOrder.getCreatedAt().getTime())); //付款时间
        }else{
            posInfo.setPaymentdate(formatter.print(openClientPaymentInfo.getPaidAt().getTime())); //付款时间
        }

        //获取会员卡号
        String cardcode ="";
        if(Arguments.notNull(shopOrder.getBuyerId())&&shopOrder.getBuyerId()>0L&&shopOrder.getShopName().startsWith("官网")){
            Response<MemberProfile>  memberProfileRes = ucUserOperationLogic.findByUserId(shopOrder.getBuyerId());
            if(!memberProfileRes.isSuccess()){
                log.error("find member profile by user id:{} fail,error:{}",shopOrder.getBuyerId(),memberProfileRes.getError());
            }else {
                cardcode = memberProfileRes.getResult().getOuterId();
            }
        }

        posInfo.setCardcode(cardcode); //会员卡号
        posInfo.setBuyercode(shopOrder.getBuyerName());//买家昵称
        posInfo.setBuyermobiletel(shopOrder.getOutBuyerId()); //买家手机
        posInfo.setBuyertel(""); //买家座机
        posInfo.setBuyerremark(shopOrder.getBuyerNote()); //买家留言
        posInfo.setConsigneename(receiverInfo.getReceiveUserName());//收货人姓名
        posInfo.setPayamountbakup(new BigDecimal(shipmentExtra.getShipmentTotalPrice()==null?0:shipmentExtra.getShipmentTotalPrice()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString()); //线上实付金额
        posInfo.setExpresscost(new BigDecimal(shipmentExtra.getShipmentShipFee()==null?0:shipmentExtra.getShipmentShipFee()).divide(new BigDecimal(100),2,RoundingMode.HALF_DOWN).toString());//邮费成本
        posInfo.setCodcharges("0");//货到付款服务费
        posInfo.setPreremark(""); //优惠信息
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
     /*   if(isWarehouseShip(shipmentWay)){
            //仓发
        }else{
            //店铺发货
            try{
                //ExpressCode expressCode = orderReadLogic.makeExpressNameByMposCode(shipmentExtra.getShipmentCorpCode());
                posInfo.setVendcustcode(expressCode.getHkCode());  //物流公司代码
            }catch (Exception e){
                log.error("find express code failed,mposShipmentCorpCode is {}",shipmentExtra.getShipmentCorpCode());
            }

        }*/
        posInfo.setExpressbillno(shipmentExtra.getShipmentSerialNo()); //物流单号
        posInfo.setWms_ordercode(""); //第三方物流单号
        if(Arguments.isNull(shipmentExtra.getShipmentDate())){
            posInfo.setConsignmentdate(formatter.print(new Date().getTime())); //发货时间
        }else {
            posInfo.setConsignmentdate(formatter.print(shipmentExtra.getShipmentDate().getTime())); //发货时间

        }
        //添加重量
        if (java.util.Objects.isNull(shipmentExtra.getWeight())){
            posInfo.setWeight("0.00");
            posInfo.setParcelweight("0.00");
        }else{
            posInfo.setWeight(String.valueOf(shipmentExtra.getWeight()));
            posInfo.setParcelweight(String.valueOf(shipmentExtra.getWeight()));
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
