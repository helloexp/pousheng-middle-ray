package com.pousheng.middle.web.order.sync.hk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.hksyc.pos.api.SycHkShipmentPosApi;
import com.pousheng.middle.hksyc.pos.dto.*;
import com.pousheng.middle.open.api.constant.ExtraKeyConstant;
import com.pousheng.middle.open.component.InvoiceLogic;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentDetail;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
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
import io.terminus.open.client.order.dto.OpenClientOrderInvoice;
import io.terminus.open.client.order.dto.OpenClientPaymentInfo;
import io.terminus.open.client.vip.constant.VipConstant;
import io.terminus.open.client.vip.extra.service.VipInvoiceServerice;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.common.constants.JacksonType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.OrderWriteService;
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
import java.text.DecimalFormat;
import java.util.*;

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

    @Autowired
    private VipInvoiceServerice vipInvoiceServerice;

    @Autowired
    private InvoiceLogic invoiceLogic;

    @Autowired
    private MiddleOrderWriteService middleOrderWriteService;

    @RpcConsumer
    private OrderWriteService orderWriteService;

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
        ShipmentDetail shipmentDetail = shipmentReadLogic.orderDetail(shipment.getId());
        ShopOrder shopOrder = shipmentDetail.getShopOrder();
        // 如果 vip-oxo 发票拉取失败，这里再补偿拉取一次
        rePullIfVipInvoiceLost(shipmentDetail);
        // 云聚的单据根据order上的信息判读是否需要推hk
        try {
            if (shopOrder.getShopName().startsWith("yj")) {
                if (!shopOrder.getExtra().containsKey(ExtraKeyConstant.IS_SYNCHK)
                        || Objects.equal("N", shopOrder.getExtra().get(ExtraKeyConstant.IS_SYNCHK))) {
                    return Response.ok();
                }
            }
            ShipmentExtra shipmentExtra = shipmentDetail.getShipmentExtra();
            String shipmentWay = StringUtils.isEmpty(shipmentExtra.getShipmentWay()) ? "2" : shipmentExtra.getShipmentWay();
            if (Strings.isNullOrEmpty(shipmentWay)) {
                log.error("shipment(id:{}) shipment way invalid", shipment.getId());
                throw new ServiceException("shipment.way.invalid");
            }

            HkShipmentPosRequestData requestData = makeHkShipmentPosRequestData(shipmentDetail, shipmentWay);
            String url = "/common/erp/pos/addnetsalshop";
            if (isWarehouseShip(shipmentWay)) {
                url = "/common/erp/pos/addnetsalstock";
            }
            String result = sycHkShipmentPosApi.doSyncShipmentPos(requestData, url);
            log.info("sync shipment pos to hk,response:{}", result);
            SycShipmentPosResponse response = JsonMapper.nonEmptyMapper().fromJson(result, SycShipmentPosResponse.class);
            if (!Objects.equal(response.getCode(), "00000")) {
                log.error("sync shipment(code:{}) shipment pos to hk fail,error:{}", shipment.getShipmentCode(), response.getMessage());
                return Response.fail(response.getMessage());
            }

            //网店零售订单号
            String billNo = getBillNo(response.getReturnJson());
            shipmentExtra.setHkResaleOrderId(billNo);
            Map<String, String> extraMap = shipment.getExtra();
            extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, JSON_MAPPER.toJson(shipmentExtra));
            //更新extra
            shipmentWiteLogic.updateExtra(shipment.getId(), extraMap);


            return Response.ok();
        } catch (Exception e) {
            log.error("sync hk pos shipment failed,shipmentId is({}) cause by({})", shipment.getId(), Throwables.getStackTraceAsString(e));
            return Response.fail(Throwables.getStackTraceAsString(e));
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

            String url = "/common/erp/pos/updatenetsalreceiptdate";
            HkShimentDoneRequestData requestData = new HkShimentDoneRequestData();
            requestData.setTranReqDate(formatter.print(System.currentTimeMillis()));

            HkShimentDoneInfo doneInfo = new HkShimentDoneInfo();
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            if (Strings.isNullOrEmpty(shipmentExtra.getHkResaleOrderId())) {
                log.error("shipment(id:{}) sync hk confirm fail,param invalid", shipment.getId());
                return Response.fail("shipment.confirm.param.invalid");
            }

            doneInfo.setNetbillno(shipment.getShipmentCode());
            //如果确认收货时间是空的，直接传当前时间
            if (Arguments.isNull(shipment.getConfirmAt())) {
                doneInfo.setReceiptdate(formatter.print(shipment.getUpdatedAt().getTime()));
            } else {
                doneInfo.setReceiptdate(formatter.print(shipment.getConfirmAt().getTime()));
            }

            requestData.setBizContent(Lists.newArrayList(doneInfo));

            String result = sycHkShipmentPosApi.doSyncShipmentDone(requestData, url);
            SycShipmentPosResponse response = JsonMapper.nonEmptyMapper().fromJson(result, SycShipmentPosResponse.class);
            if (!Objects.equal(response.getCode(), "00000")) {
                log.error("sync shipment confirm at to hk fail,error:{}", response.getMessage());
                return Response.fail(response.getMessage());
            }
            return Response.ok();
        } catch (Exception e) {
            log.error("sync hk shipment confirm at failed,shipmentId is({}) cause by({})", shipment.getId(), Throwables.getStackTraceAsString(e));
            return Response.fail("sync.hk.shipment.confirm.at.fail");
        }

    }


    private HkShipmentPosRequestData makeHkShipmentPosRequestData(ShipmentDetail shipmentDetail, String shipmentWay) {


        HkShipmentPosRequestData requestData = new HkShipmentPosRequestData();
        requestData.setTranReqDate(formatter.print(System.currentTimeMillis()));
        if (isWarehouseShip(shipmentWay)) {
            log.info("current shipment(id:{}) is warehouse shipment");
            requestData.setSid("PS_ERP_POS_netsalstock");//仓发
        } else {
            requestData.setSid("PS_ERP_POS_netsalshop");//店发
        }
        HkShipmentPosContent bizContent = makeHkShipmentPosContent(shipmentDetail, shipmentWay);
        requestData.setBizContent(bizContent);
        return requestData;
    }

    private HkShipmentPosContent makeHkShipmentPosContent(ShipmentDetail shipmentDetail, String shipmentWay) {


        Shipment shipment = shipmentDetail.getShipment();
        ShopOrder shopOrder = shipmentDetail.getShopOrder();
        ShipmentExtra shipmentExtra = shipmentDetail.getShipmentExtra();
        HkShipmentPosContent posContent = new HkShipmentPosContent();

        posContent.setChanneltype("b2c");//订单来源类型, 是b2b还是b2c

        if (isWarehouseShip(shipmentWay)) {
            WarehouseDTO warehouse = warehouseCacher.findById(shipmentExtra.getWarehouseId());
            if (StringUtils.isEmpty(warehouse.getOutCode())) {
                log.error("warehouse(id:{}) out code invalid", warehouse.getId());
                throw new ServiceException("warehouse.out.code.invalid");
            }
            posContent.setCompanyid(warehouse.getCompanyId());//实际发货账套id
            posContent.setStockcode(warehouse.getOutCode());//实际发货店铺code

            //下单店
            OpenShop openShop = orderReadLogic.findOpenShopByShopId(shipment.getShopId());
            Map<String, String> extraMap = openShop.getExtra();
            String companyId = extraMap.get("companyCode");
            String code = extraMap.get("hkPerformanceShopOutCode");

            posContent.setNetcompanyid(companyId);//线上店铺所属公司id
            posContent.setNetshopcode(code);//线上店铺code
        } else {
            //非补邮费订单
            if (!isPostageOrder(shipmentExtra)) {
                Shop receivershop = shopCacher.findShopById(shipmentExtra.getWarehouseId());
                posContent.setCompanyid(receivershop.getBusinessId().toString());//实际发货账套id
                posContent.setShopcode(receivershop.getOuterId());//实际发货店铺code
            }

            //下单店
            OpenShop openShop = orderReadLogic.findOpenShopByShopId(shipment.getShopId());
            Map<String, String> extraMap = openShop.getExtra();
            String companyId = extraMap.get("companyCode");
            String code = extraMap.get("hkPerformanceShopOutCode");

            posContent.setNetcompanyid(companyId);//线上店铺所属公司id
            posContent.setNetshopcode(code);//线上店铺code

            //补邮费订单
            if (isPostageOrder(shipmentExtra)) {
                posContent.setCompanyid(companyId);//绩效店铺所属公司id
                posContent.setShopcode(code);//绩效店铺code
            }

            //门店扫码订单归属國瑞城
            Map<String, String> shopOrderExtra = shopOrder.getExtra();
            String isStoreScanCode = shopOrderExtra.get("is_store_scan_code");
            if (!StringUtils.isEmpty(isStoreScanCode) && Objects.equal(isStoreScanCode, "true")) {  //门店扫码订单
                posContent.setNetcompanyid("244");//绩效店铺所属公司id
                posContent.setNetshopcode("SP004481");//绩效店铺code
            }
        }
        posContent.setVoidstockcode(posStockCode);//todo 实际发货账套的虚拟仓代码


        posContent.setNetstockcode(posStockCode);//todo 线上店铺所属公司的虚拟仓代码
        posContent.setNetbillno(shipment.getShipmentCode());//端点唯一订单号
        Map<String, String> shopOrderExtra = shopOrder.getExtra();
        String isHkPosOrder = shopOrderExtra.get("isHkPosOrder");
        if (!StringUtils.isEmpty(isHkPosOrder) && Objects.equal(isHkPosOrder, "true")) {
            String outOrderId = shopOrderExtra.get("hkOutOrderId");
            posContent.setSourcebillno(outOrderId == null ? "" : outOrderId);//订单来源单号
        } else {
            posContent.setSourcebillno("");//订单来源单号
        }
        posContent.setBilldate(formatter.print(shopOrder.getOutCreatedAt().getTime()));//订单日期
        posContent.setOperator("MPOS_EDI");//线上店铺帐套操作人code
        posContent.setRemark(shopOrder.getBuyerNote());//备注

        HkShipmentPosInfo netsalorder = makeHkShipmentPosInfo(shipmentDetail, shipmentWay);
        List<HkShipmentPosItem> ordersizes = makeHkShipmentPosItem(shipmentDetail);

        // 如果是邮费订单则补传子单明细
        if (isPostageOrder(shipmentExtra)) {
            //邮费订单将订单总金额放到邮费里。并重置数量为0
            if (org.apache.commons.collections.CollectionUtils.isNotEmpty(shipmentDetail.getShipmentItems())) {
                ShipmentItem item=shipmentDetail.getShipmentItems().get(0);
                //商品金额 单位从分转换为元
                BigDecimal itemFee=convertToBigDecimal(String.valueOf(item.getSkuPrice()*item.getQuantity()/100));

                //总金额=邮费+商品金额
                BigDecimal orgExpressFee = convertToBigDecimal(netsalorder.getExpresscost());
                BigDecimal totalFee = itemFee.add(orgExpressFee);
                totalFee.setScale(2, RoundingMode.HALF_DOWN);
                //重置实付金额和让利金额
                netsalorder.setExpresscost(totalFee.toString());
                netsalorder.setZramount("0");
                netsalorder.setPayamountbakup("0");
            }
        } else {
            posContent.setOrdersizes(ordersizes);
        }
        posContent.setNetsalorder(netsalorder);
        return posContent;
    }

    private List<HkShipmentPosItem> makeHkShipmentPosItem(ShipmentDetail shipmentDetail) {
        List<ShipmentItem> shipmentItems = shipmentDetail.getShipmentItems();
        List<HkShipmentPosItem> posItems = Lists.newArrayListWithCapacity(shipmentItems.size());
        for (ShipmentItem shipmentItem : shipmentItems) {
            if (shipmentItem.getShipQuantity() != null && shipmentItem.getShipQuantity() == 0) {
                continue;
            }
            HkShipmentPosItem hkShipmentPosItem = new HkShipmentPosItem();
            hkShipmentPosItem.setMatbarcode(shipmentItem.getSkuCode());
            hkShipmentPosItem.setBalaprice(new BigDecimal(shipmentItem.getCleanFee() == null ? 0 : shipmentItem.getCleanFee())
                    .divide(new BigDecimal(shipmentItem.getQuantity()), 6, RoundingMode.HALF_DOWN)
                    .divide(new BigDecimal(100), 6, RoundingMode.HALF_DOWN).toString());
            //平台分摊到发货单上面的优惠金额
            hkShipmentPosItem.setCouponprice(new BigDecimal(shipmentItem.getSharePlatformDiscount() == null ? 0 : shipmentItem.getSharePlatformDiscount())
                    .divide(new BigDecimal(100), 2, RoundingMode.HALF_DOWN).toString());
            if (shipmentItem.getShipQuantity() == null) {
                hkShipmentPosItem.setQty(shipmentItem.getQuantity());
            } else {
                hkShipmentPosItem.setQty(shipmentItem.getShipQuantity());
            }
            Integer integral = shipmentItem.getIntegral() == null ? 0 : shipmentItem.getIntegral();
            Integer balapriceOfIntegralPart = Math.multiplyExact(integral, 2); // 积分商品因恒康月结原因，需要将积分商品的积分换算成金额添加到实付金额中，因此处返回金额需要乘以100，按默认积分兑换比率0.02，所以此处只做乘以2
            hkShipmentPosItem
                    .setBalaprice(new BigDecimal(shipmentItem.getCleanPrice() == null ? 0 + balapriceOfIntegralPart
                            : shipmentItem.getCleanPrice() + balapriceOfIntegralPart)
                                    .divide(new BigDecimal(100), 2, RoundingMode.HALF_DOWN).toString());
            posItems.add(hkShipmentPosItem);
        }
        return posItems;
    }

    private HkShipmentPosInfo makeHkShipmentPosInfo(ShipmentDetail shipmentDetail, String shipmentWay) {

        ShopOrder shopOrder = shipmentDetail.getShopOrder();

        OpenClientPaymentInfo openClientPaymentInfo = orderReadLogic.getOpenClientPaymentInfo(shopOrder);
        ReceiverInfo receiverInfo = shipmentDetail.getReceiverInfo();
        ShipmentExtra shipmentExtra = shipmentDetail.getShipmentExtra();

        // todo 如果 invoices 为空，调用 openclient 再拉一次
        List<Invoice> invoices = shipmentDetail.getInvoices();

        // List<Long> skuOrdersIds = new java.util.ArrayList<Long>();
        Map<Long, List<ShipmentItem>> skuOrderIdsAndShipmentItemInfosMapping = new HashMap<>();
        HkShipmentPosInfo posInfo = new HkShipmentPosInfo();
        posInfo.setManualbillno(shopOrder.getOutId()); //第三方平台单号
        posInfo.setBuyeralipayno(""); //支付宝账号
        posInfo.setAlipaybillno(""); //支付交易号
        posInfo.setSourceremark(getSourceRemark(shopOrder)); //订单来源说明
        posInfo.setOrdertype("");  //订单类型
        posInfo.setOrdercustomercode(""); //订单标记code
        posInfo.setAppamtsourceshop(""); //业绩来源店铺
        //因为有些付款时间拉取不到，统一使用中台订单创建时间
        posInfo.setPaymentdate(formatter.print(shopOrder.getCreatedAt().getTime())); //付款时间

        //获取会员卡号
        String cardcode = "";
        if (Arguments.notNull(shopOrder.getBuyerId()) && shopOrder.getBuyerId() > 0L && shopOrder.getShopName().startsWith("官网")) {
            Response<MemberProfile> memberProfileRes = ucUserOperationLogic.findByUserId(shopOrder.getBuyerId());
            if (!memberProfileRes.isSuccess()) {
                log.error("find member profile by user id:{} fail,error:{}", shopOrder.getBuyerId(), memberProfileRes.getError());
            } else {
                cardcode = memberProfileRes.getResult().getOuterId();
            }
        }

        posInfo.setCardcode(cardcode); //会员卡号
        posInfo.setBuyercode(shopOrder.getBuyerName());//买家昵称
        posInfo.setBuyermobiletel(shopOrder.getOutBuyerId()); //买家手机
        posInfo.setBuyertel(""); //买家座机
        posInfo.setBuyerremark(shopOrder.getBuyerNote()); //买家留言
        posInfo.setConsigneename(receiverInfo.getReceiveUserName());//收货人姓名
        //等待改造
        List<ShipmentItem> shipmentItems = shipmentDetail.getShipmentItems();
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (ShipmentItem shipmentItem : shipmentItems) {
            if (!skuOrderIdsAndShipmentItemInfosMapping.containsKey(shipmentItem.getSkuOrderId())) {
                skuOrderIdsAndShipmentItemInfosMapping.put(shipmentItem.getSkuOrderId(),
                        Lists.newArrayList(shipmentItem));
            } else {
                skuOrderIdsAndShipmentItemInfosMapping.get(shipmentItem.getSkuOrderId()).add(shipmentItem);
            }

            if (shipmentItem.getShipQuantity() != null && !shipmentItem.getQuantity().equals(shipmentItem.getShipQuantity())) {
                totalPrice = totalPrice.add(new BigDecimal(shipmentItem.getCleanFee())
                        .multiply(new BigDecimal(shipmentItem.getShipQuantity())).divide(new BigDecimal(shipmentItem.getQuantity()), 2, RoundingMode.HALF_DOWN));
            } else {
                totalPrice = totalPrice.add(new BigDecimal(shipmentItem.getCleanFee()));
            }
        }
        totalPrice = totalPrice.add(new BigDecimal(shipmentExtra.getShipmentShipFee() == null ? 0 : shipmentExtra.getShipmentShipFee()));
        posInfo.setPayamountbakup(totalPrice.divide(new BigDecimal(100), 2, RoundingMode.HALF_DOWN).toString()); //线上实付金额
        posInfo.setExpresscost(new BigDecimal(shipmentExtra.getShipmentShipFee() == null ? 0 : shipmentExtra.getShipmentShipFee()).divide(new BigDecimal(100), 2, RoundingMode.HALF_DOWN).toString());//邮费成本
        posInfo.setCodcharges("0");//货到付款服务费
        posInfo.setPreremark(""); //优惠信息
        if (CollectionUtils.isEmpty(invoices)) {
            posInfo.setIsinvoice("0"); //是否开票

        } else {
            posInfo.setIsinvoice("1"); //是否开票
            posInfo.setInvoice_name(invoices.get(0).getTitle()); //发票抬头
            posInfo.setTaxno("");//税号
        }

        Map<String, String> shopOrderExtra = shopOrder.getExtra();
        //卖家备注
        String sellerNote = "";
        if (!CollectionUtils.isEmpty(shopOrderExtra) && shopOrderExtra.containsKey("customerServiceNote")) {
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
        if (Arguments.isNull(shipmentExtra.getShipmentDate())) {
            posInfo.setConsignmentdate(formatter.print(new Date().getTime())); //发货时间
        } else {
            posInfo.setConsignmentdate(formatter.print(shipmentExtra.getShipmentDate().getTime())); //发货时间

        }
        //添加重量
        if (java.util.Objects.isNull(shipmentExtra.getWeight())) {
            posInfo.setWeight("0.00");
            posInfo.setParcelweight("0.00");
        } else {
            posInfo.setWeight(String.valueOf(shipmentExtra.getWeight()));
            posInfo.setParcelweight(String.valueOf(shipmentExtra.getWeight()));
        }

        DecimalFormat df = new DecimalFormat("0.##");
        posInfo.setDischargeintegral(String
                .valueOf(shipmentReadLogic.getUsedIntegralPlusSkuIntegral(skuOrderIdsAndShipmentItemInfosMapping)));
        posInfo.setDischargeamount(
                df.format(BigDecimal
                        .valueOf(shipmentReadLogic.getAmountOfMoneyPaidByBothSkuIntegralAndUsedIntegral(
                                skuOrderIdsAndShipmentItemInfosMapping))
                        .divide(new BigDecimal(100), 2, RoundingMode.HALF_DOWN)));

        return posInfo;
    }
    
    private String getSourceRemark(ShopOrder shopOrder){
        String sourceRemark;
        if(Objects.equal(shopOrder.getOutFrom(),"official")){
            if(shopOrder.getShopName().startsWith("mpos")){
                sourceRemark = "mpos";
            }else{
                sourceRemark = shopOrder.getOutFrom();
            }
        }else{
            sourceRemark = shopOrder.getOutFrom();
        }
        return sourceRemark;
    }

    private String getBillNo(String data) throws Exception {
        Map<String, String> map = objectMapper.readValue(data, JacksonType.MAP_OF_STRING);
        if (!CollectionUtils.isEmpty(map) && map.containsKey("billNo")) {
            return map.get("billNo");
        } else {
            throw new ServiceException("hk.result.bill.no.invalid");
        }
    }

    private Boolean isWarehouseShip(String shipmentWay) {
        if (Objects.equal(shipmentWay, "2")) {
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    /**
     * 是否是补邮费订单
     * @param shipmentExtra
     * @return
     */
    private boolean isPostageOrder(ShipmentExtra shipmentExtra) {
        return !java.util.Objects.isNull(shipmentExtra.getIsPostageOrder())
            && shipmentExtra.getIsPostageOrder();
    }

    /**
     * 转换金额类型
     * @param val
     * @return
     */
    private BigDecimal convertToBigDecimal(String val){
        return new BigDecimal(org.apache.commons.lang3.StringUtils.isBlank(val) ? "0" : val);
    }

    /**
     * 根据 shopOrder 的打标字段，判断是否要重新拉取唯品会发票
     * 如果要拉，拉取并更新 shopOrder extra
     * @param shipmentDetail
     */
    private void rePullIfVipInvoiceLost(ShipmentDetail shipmentDetail) {
        ShopOrder shopOrder = shipmentDetail.getShopOrder();
        if (shopOrder.getExtra().containsKey(VipConstant.VIP_OXO_INVOICE_LOST)
                && Boolean.TRUE.toString().equals(shopOrder.getExtra().get(VipConstant.VIP_OXO_INVOICE_LOST))) {
            // 补偿再拉取一次唯品会发票信息
            Map<String, OpenClientOrderInvoice> openClientOrderInvoiceMap = vipInvoiceServerice.getOrderInvoice(
                    shopOrder.getShopId(), Collections.singletonList(shopOrder.getOutId()));
            if (openClientOrderInvoiceMap.isEmpty()) {
                // 还是拉不到就算了
                log.warn("fail again for pull vip-oxo invoice");
                return;
            }
            OpenClientOrderInvoice openClientOrderInvoice = openClientOrderInvoiceMap.get(shopOrder.getOutId());
            if (Arguments.isNull(openClientOrderInvoice.getType())) {
                //生成发票信息(可能生成失败)
                log.warn("re-pull vip invoice fail as invoice type null");
                return;
            }
            String openClientOrderInvoiceJson = JsonMapper.JSON_NON_DEFAULT_MAPPER.toJson(openClientOrderInvoice);
            // 去掉打标信息，防止后面操作一直拉
            shopOrder.getExtra().remove(VipConstant.VIP_OXO_INVOICE_LOST);
            // 填充已查 shopOrder 的 extra 信息（与 DefaultOrderReceiver 258 行保持一致）
            shopOrder.getExtra().put("invoice", openClientOrderInvoiceJson);
            // 补偿插入 invoice
            Long invoiceId = invoiceLogic.addInvoice(openClientOrderInvoice);
            // 补偿插入 orderInvoice
            middleOrderWriteService.createOrderInvoice(makeOrderInvoice(invoiceId, shopOrder.getId()));
            // 补偿更新 shopOrder 的 extra
            Response<Boolean> extraUpdateResp =
                    orderWriteService.updateOrderExtra(shopOrder.getId(), OrderLevel.SHOP, shopOrder.getExtra());
            // 填充已查 shipmentDetail 的 invoices 信息
            shipmentDetail.setInvoices(orderReadLogic.findInvoiceInfo(shopOrder.getId()));
            if (!extraUpdateResp.isSuccess()) {
                log.error("fail to update order invoice, id={}, invoice={}, cause: {}",
                        shopOrder.getId(), openClientOrderInvoiceJson, extraUpdateResp.getError());
            }
        }
    }

    private OrderInvoice makeOrderInvoice(Long invoiceId, Long shopOrderId) {
        OrderInvoice orderInvoice = new OrderInvoice();
        orderInvoice.setOrderType(OrderLevel.SHOP.getValue());
        orderInvoice.setInvoiceId(invoiceId);
        orderInvoice.setOrderId(shopOrderId);
        orderInvoice.setStatus(1);
        return orderInvoice;
    }
}
