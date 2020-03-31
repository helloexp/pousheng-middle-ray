package com.pousheng.middle.web.express.esp;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Optional;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.order.service.PoushengConfigService;
import com.pousheng.middle.shop.service.PsShopReadService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.express.ExpressConfigType;
import com.pousheng.middle.web.express.esp.bean.*;
import com.pousheng.middle.web.express.esp.client.ExpressClient;
import com.pousheng.middle.web.express.esp.exception.UnsuportExpressCompany;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShipmentItem;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @Desc 中台ESP快递处理
 * @Author GuoFeng
 * @Date 2019/8/27
 */
@Service
@Slf4j
public class ExpressProcessService {

    @Autowired
    private PoushengConfigService poushengConfigService;

    @Autowired
    private ExpressClient expressClient;

    @Autowired
    private OpenShopReadService openShopReadService;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    @Autowired
    private WarehouseCacher warehouseCacher;

    @Autowired
    private SkuTemplateReadService skuTemplateReadService;

    @Autowired
    private PsShopReadService psShopReadService;

    @Autowired
    private MiddleOrderReadService orderReadService;

    @Autowired
    private ConfigCacheService configCacheService;

    @Autowired
    private EventBus eventBus;


    /**
     * 获取快递单号
     *
     * @return
     */
    public MposResponse getExpressNo(ShopOrder shopOrder, Shipment shipment) {
        MposResponse response = new MposResponse();
        ExpressNoRequest expressNoRequest = buildExpressNoRequest(shopOrder, shipment);
        String json = JSONObject.toJSONString(expressNoRequest);
        log.info("获取快递单号参数:{}", json);
        JSONObject expressNo = expressClient.getExpressNo(json);
        log.info("获取快递单号结果:{}", expressNo.toString());
        if ("00000".equals(expressNo.getString("code"))) {
            JSONObject returnJsonObject = expressNo.getJSONObject("returnJson");
            JSONObject data = returnJsonObject.getJSONObject("data");
            String expressbillno = data.getString("expressbillno");
            response.setSuccess(true);
            response.setUseMiddleService(true);
            response.setExpressOrderId(expressbillno);
            response.setProcess(true);

            return response;
        }

        response.setSuccess(true);
        response.setMsg(expressNo.getString("code") + ":" + expressNo.getString("message"));
        return response;

    }


    public boolean checkShopUseESP(String shopUserName) {
        String value = configCacheService.getUnchecked(ExpressConfigType.use_ESP_service);
        if (StringUtils.hasText(value) && ("true".equals(value) || "false".equals(value))) {
            Boolean flag = Boolean.valueOf(value);
            if (flag) {
                //全部门店都使用esp
                return true;
            } else {
                //根据门店列表判断，看该门店是不是需要使用esp
                configCacheService.getUnchecked(ExpressConfigType.store_list_ESP);
                if (configCacheService.storeList.contains(shopUserName)) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }


    /**
     * 回传店发快递单号
     *
     * @return
     */
    public JSONObject sendExpressNo(ShopOrder shopOrder, Shipment shipment, String shipmentCorpCode, String shipmentSerialNo) {
        ExpressNoRequest expressNoRequest = buildSendExpressNoRequest(shopOrder, shipment, shipmentCorpCode, shipmentSerialNo);
        String param = JSONObject.toJSONString(expressNoRequest);
        JSONObject espExpressCodeSendResponse = expressClient.sendStoreExpressCode(param);
        return espExpressCodeSendResponse;
    }

    /**
     * 回传仓发快递单号
     *
     * @return
     */
    public ESPExpressCodeSendResponse sendWarehouseExpressNo(ShopOrder shopOrder, Shipment shipment, String shipmentCorpCode, String shipmentSerialNo) {

        WarehouseDTO warehouseDTO = warehouseCacher.findById(shipment.getShipId());
        String companyCode = null;
        if (warehouseDTO != null) {
            companyCode = warehouseDTO.getCompanyCode();
        }

        ESPExpressCodeSendRequestContent content = new ESPExpressCodeSendRequestContent();
        content.setBillno(shipment.getShipmentCode());
        content.setCompanycode(companyCode);
        content.setExpressbillno(shipmentSerialNo);
        content.setExpresscode(shipmentCorpCode);
        content.setIssplitbill("0");

        ESPExpressCodeSendRequest request = new ESPExpressCodeSendRequest();
        request.setBizContent(Lists.newArrayList(content));
        request.setSid(ESPServiceIDCode.sendWarehouseExpressNo);
        request.setTranReqDate(new Date());


        String json = JSONObject.toJSONString(request);
        //  log.info("回传仓发参数:{}", json);
        ESPExpressCodeSendResponse espExpressCodeSendResponse = expressClient.sendExpressCode(json);

        return espExpressCodeSendResponse;
    }

    /**
     * 取消快递单
     *
     * @return
     */
    public ESPExpressCodeSendResponse cancelExpressNo(Shipment shipment) {

        WarehouseDTO warehouseDTO = warehouseCacher.findById(shipment.getShipId());
        String companyCode = null;
        if (warehouseDTO != null) {
            companyCode = warehouseDTO.getCompanyCode();
        }

        ESPExpressCodeCancelRequestContent content = new ESPExpressCodeCancelRequestContent();
        content.setBillno(shipment.getShipmentCode());
        content.setCompanycode(companyCode);

        ESPExpressCodeCancelRequest request = new ESPExpressCodeCancelRequest();
        request.setBizContent(content);
        request.setSid(ESPServiceIDCode.cancelOXOExpressNo);
        request.setTranReqDate(new Date());


        String json = JSONObject.toJSONString(request);
        //log.info("取消快递参数:{}", json);
        ESPExpressCodeSendResponse espExpressCodeSendResponse = expressClient.cancelExpressCode(json);

        return espExpressCodeSendResponse;
    }


    private ExpressNoRequest buildExpressNoRequest(ShopOrder shopOrder, Shipment shipment) {
        //构建参数
        ExpressNoRequest request = new ExpressNoRequest();
        request.setSid(ESPServiceIDCode.getExpressNo);
        request.setTranReqDate(new Date());

        BizContent bizcontent = new BizContent();
        //填充地址信息

        ReceiverInfo receiverInfo = JsonMapper.JSON_NON_DEFAULT_MAPPER.fromJson(shipment.getReceiverInfos(), ReceiverInfo.class);
        Response<OpenShop> openShopResponse = openShopReadService.findById(shopOrder.getShopId());
        String companyCode = null;
        if (openShopResponse.isSuccess()) {
            OpenShop openShop = openShopResponse.getResult();
            Map<String, String> extra = openShop.getExtra();
            companyCode = extra.get("companyCode");
            String shopOutCode = extra.get("hkPerformanceShopOutCode");
            bizcontent.setShopcode(shopOutCode);
        }

        bizcontent.setProvince(receiverInfo.getProvince());
        bizcontent.setCity(receiverInfo.getCity());
        bizcontent.setArea(receiverInfo.getRegion());
        bizcontent.setAddress(receiverInfo.getDetail());

        //bizcontent.setBcmembercard();
        bizcontent.setBcmembername(shopOrder.getBuyerName());
        bizcontent.setBillno(shipment.getShipmentCode());
        bizcontent.setBilltype("SalesBC");
        bizcontent.setBuyermobiletel(receiverInfo.getMobile());
        //bizcontent.setBuyertel("");

        String outFrom = shopOrder.getOutFrom();
        if (MiddleChannel.VIPOXO.getValue().equals(outFrom)) {
            bizcontent.setChannel(outFrom);
            bizcontent.setExpressbillno(shipment.getShipmentSerialNo());
        } else {
            bizcontent.setChannel("mpos");
        }
        bizcontent.setCollectionamount("0");
        bizcontent.setCompanycode(companyCode);
        bizcontent.setConsigneename(receiverInfo.getReceiveUserName());

        bizcontent.setDeliverytype("10");
        //bizcontent.setDeliverydatetime();
        Long shopId = shipment.getShipId();
        if (shipment.getShipWay() == 1) {
            Response<OpenShop> shipShopResponse = openShopReadService.findById(shopId);
            if (shipShopResponse.isSuccess()) {
                OpenShop shipShop = shipShopResponse.getResult();
                Map<String, String> extra = shipShop.getExtra();
                bizcontent.setDeliveryshopcode(extra.get("hkPerformanceShopOutCode"));
                bizcontent.setDeliverycompanycode(extra.get("companyCode"));
            }
        } else {
            WarehouseDTO warehouseDTO = warehouseCacher.findById(shopId);
            bizcontent.setStockcode(warehouseDTO.getWarehouseCode());
            bizcontent.setDeliverycompanycode(warehouseDTO.getCompanyCode());
        }

        //获取门店的默认快递
        Response<Optional<Shop>> byOuterIdAndBusinessId = psShopReadService.findByOuterIdAndBusinessId(bizcontent.getDeliveryshopcode(), Long.valueOf(bizcontent.getDeliverycompanycode()));
        if (byOuterIdAndBusinessId.isSuccess()) {
            Optional<Shop> result = byOuterIdAndBusinessId.getResult();
            String shopExtraInfo = result.get().getExtra().get("shopExtraInfo");
            JSONObject json = JSONObject.parseObject(shopExtraInfo);
            JSONArray expresssCompanyList = json.getJSONArray("expresssCompanyList");
            if (expresssCompanyList.size() > 0) {
                JSONObject express = expresssCompanyList.getJSONObject(0);
                String code = express.getString("code");
                String name = express.getString("name");

                configCacheService.getUnchecked(ExpressConfigType.express_mapping);
                ExpressCodeMapping expressCodeMapping = configCacheService.expressCodeMap.get(code);
                if (expressCodeMapping != null) {
                    bizcontent.setCustomercode(expressCodeMapping.getEspExpressCode());
                    bizcontent.setFreightcompany(expressCodeMapping.getEspExpressCode());
                } else {
                    throw new UnsuportExpressCompany("不支持的快递公司");
                }
                bizcontent.setCustomername(name);
            }
        }


        //bizcontent.setExchangeintegral();
        //bizcontent.setExpectqty();
        //bizcontent.setExpressaccountid();
        bizcontent.setExpressamount(shopOrder.getShipFee().toString());
        bizcontent.setExpresstype("Express");


        //bizcontent.setFreightpay();

        //bizcontent.setInvoicecontent();
        //bizcontent.setInvoicename();
        bizcontent.setIscall("1");
        bizcontent.setIsgetexpress("1");
        //bizcontent.setIsinvoice();
        //bizcontent.setIsprintinvoice();
        //bizcontent.setIstrave();


        bizcontent.setOrderdatetime(sdf.format(shopOrder.getOutCreatedAt()));

        String fee = shopOrder.getFee().toString();
        bizcontent.setPayamount(fee);
        bizcontent.setPayamountbakup(fee);
        //bizcontent.setPaymentdate();
        bizcontent.setPaymenttype("0");
        //bizcontent.setPromzramount();

        bizcontent.setRefundchangetype(shipment.getType().toString());
        //bizcontent.setRptamount();

        //bizcontent.setSendaddress();
        //bizcontent.setSendarea();
        //bizcontent.setSendcity();
        //bizcontent.setSendcontact();
        //bizcontent.setSendcontacttel();
        //bizcontent.setSendprovince();
        //bizcontent.setShopbillno();
        bizcontent.setShopname(shopOrder.getShopName());
        bizcontent.setSourcebillno(shopOrder.getOutId());

        //bizcontent.setStockcompanycode();


        //bizcontent.setVatnumber();
        //bizcontent.setVolume();

        //设置重量
        Map<String, String> shipmentExtra = shipment.getExtra();
        String shipmentExtraInfo = shipmentExtra.get("shipmentExtraInfo");
        JSONObject shipmentExtraInfoJson = JSONObject.parseObject(shipmentExtraInfo);
        Double weight = shipmentExtraInfoJson.getDouble("weight");
        bizcontent.setWeight(weight);

        //bizcontent.setZipcode();

        //组装商品信息
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        List<Items> allItems = Lists.newArrayList();
        for (int i = 0; i < shipmentItems.size(); i++) {
            ShipmentItem shipmentItem = shipmentItems.get(i);
            Items item = new Items();

            item.setBalaprice(shipmentItem.getCleanPrice().toString());
            item.setBillno(shipment.getShipmentCode());
            item.setCompanycode(companyCode);
            item.setExpectqty(shipmentItem.getQuantity().toString());

            item.setMaterialshortname(shipmentItem.getSkuName());
            item.setPayamount(shipmentItem.getCleanFee().toString());
            item.setRetailprice(shipmentItem.getSkuPrice().toString());
            item.setRowno(String.valueOf(i + 1));
            item.setShopbillno(shipment.getShipmentCode());
            //查询尺寸
            Response<List<SkuTemplate>> bySkuCodes = skuTemplateReadService.findBySkuCodes(Lists.newArrayList(shipmentItem.getSkuCode()));
            if (bySkuCodes.isSuccess()) {
                List<SkuTemplate> skuTemplates = bySkuCodes.getResult();
                if (skuTemplates.size() > 0) {
                    SkuTemplate skuTemplate = skuTemplates.get(0);
                    String attrsJson = skuTemplate.getAttrsJson();
                    String sizeName = getSizeName(attrsJson);
                    item.setSizename(sizeName);

                    Map<String, String> extra = skuTemplate.getExtra();
                    String materialCode = extra.get("materialCode");
                    item.setMaterialcode(materialCode);
                }
            }
            item.setSku(shipmentItem.getSkuCode());

            allItems.add(item);
        }

        bizcontent.setTdq(String.valueOf(allItems.size()));
        bizcontent.setItems(allItems);


        request.setBizContent(bizcontent);

        return request;
    }


    private ExpressNoRequest buildSendExpressNoRequest(ShopOrder shopOrder, Shipment shipment, String shipmentCorpCode, String shipmentSerialNo) {
        //构建参数
        ExpressNoRequest request = new ExpressNoRequest();
        request.setSid(ESPServiceIDCode.getExpressNo);
        request.setTranReqDate(new Date());

        BizContent bizcontent = new BizContent();
        //填充地址信息
        ReceiverInfo receiverInfo = JsonMapper.JSON_NON_DEFAULT_MAPPER.fromJson(shipment.getReceiverInfos(), ReceiverInfo.class);
        Response<OpenShop> openShopResponse = openShopReadService.findById(shopOrder.getShopId());
        String companyCode = null;
        if (openShopResponse.isSuccess()) {
            OpenShop openShop = openShopResponse.getResult();
            Map<String, String> extra = openShop.getExtra();
            companyCode = extra.get("companyCode");
            String shopOutCode = extra.get("hkPerformanceShopOutCode");
            bizcontent.setShopcode(shopOutCode);
        }

        bizcontent.setProvince(receiverInfo.getProvince());
        bizcontent.setCity(receiverInfo.getCity());
        bizcontent.setArea(receiverInfo.getRegion());
        bizcontent.setAddress(receiverInfo.getDetail());

        //bizcontent.setBcmembercard();
        bizcontent.setBcmembername(shopOrder.getBuyerName());
        bizcontent.setBillno(shipment.getShipmentCode());
        bizcontent.setBilltype("SalesBC");
        bizcontent.setBuyermobiletel(receiverInfo.getMobile());
        //bizcontent.setBuyertel("");

        bizcontent.setChannel(MiddleChannel.VIPOXO.getValue());
        bizcontent.setExpressbillno(shipmentSerialNo);
        bizcontent.setCollectionamount("0");
        bizcontent.setCompanycode(companyCode);
        bizcontent.setConsigneename(receiverInfo.getReceiveUserName());

        bizcontent.setCustomercode(shipmentCorpCode);
        //  bizcontent.setCustomername(shipment.getShipmentCorpName());


        bizcontent.setDeliverytype("10");
        //bizcontent.setDeliverydatetime();
        Long shopId = shipment.getShipId();
        if (shipment.getShipWay() == 1) {
            Response<OpenShop> shipShopResponse = openShopReadService.findById(shopId);
            if (shipShopResponse.isSuccess()) {
                OpenShop shipShop = shipShopResponse.getResult();
                Map<String, String> extra = shipShop.getExtra();
                bizcontent.setDeliveryshopcode(extra.get("hkPerformanceShopOutCode"));
                bizcontent.setDeliverycompanycode(extra.get("companyCode"));
            }
        } else {
            WarehouseDTO warehouseDTO = warehouseCacher.findById(shopId);
            bizcontent.setStockcode(warehouseDTO.getWarehouseCode());
            bizcontent.setDeliverycompanycode(warehouseDTO.getCompanyCode());
        }

        //bizcontent.setExchangeintegral();
        //bizcontent.setExpectqty();
        //bizcontent.setExpressaccountid();
        bizcontent.setExpressamount(shopOrder.getShipFee().toString());
        bizcontent.setExpresstype("Express");


        bizcontent.setFreightcompany(shipmentCorpCode);
        //bizcontent.setFreightpay();

        //bizcontent.setInvoicecontent();
        //bizcontent.setInvoicename();
        bizcontent.setIscall("0");
        bizcontent.setIsgetexpress("0");
        //bizcontent.setIsinvoice();
        //bizcontent.setIsprintinvoice();
        //bizcontent.setIstrave();


        bizcontent.setOrderdatetime(sdf.format(shopOrder.getOutCreatedAt()));

        String fee = shopOrder.getFee().toString();
        bizcontent.setPayamount(fee);
        bizcontent.setPayamountbakup(fee);
        //bizcontent.setPaymentdate();
        bizcontent.setPaymenttype("0");
        //bizcontent.setPromzramount();

        bizcontent.setRefundchangetype(shipment.getType().toString());
        //bizcontent.setRptamount();

        //bizcontent.setSendaddress();
        //bizcontent.setSendarea();
        //bizcontent.setSendcity();
        //bizcontent.setSendcontact();
        //bizcontent.setSendcontacttel();
        //bizcontent.setSendprovince();
        //bizcontent.setShopbillno();
        bizcontent.setShopname(shopOrder.getShopName());
        bizcontent.setSourcebillno(shopOrder.getOutId());

        //bizcontent.setStockcompanycode();


        //bizcontent.setVatnumber();
        //bizcontent.setVolume();

        //设置重量
        Map<String, String> shipmentExtra = shipment.getExtra();
        String shipmentExtraInfo = shipmentExtra.get("shipmentExtraInfo");
        JSONObject shipmentExtraInfoJson = JSONObject.parseObject(shipmentExtraInfo);
        Double weight = shipmentExtraInfoJson.getDouble("weight");
        bizcontent.setWeight(weight);

        //bizcontent.setZipcode();

        //组装商品信息
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        List<Items> allItems = Lists.newArrayList();
        for (int i = 0; i < shipmentItems.size(); i++) {
            ShipmentItem shipmentItem = shipmentItems.get(i);
            Items item = new Items();

            item.setBalaprice(shipmentItem.getCleanPrice().toString());
            item.setBillno(shipment.getShipmentCode());
            item.setCompanycode(companyCode);
            item.setExpectqty(shipmentItem.getQuantity().toString());

            item.setMaterialshortname(shipmentItem.getSkuName());
            item.setPayamount(shipmentItem.getCleanFee().toString());
            item.setRetailprice(shipmentItem.getSkuPrice().toString());
            item.setRowno(String.valueOf(i + 1));
            item.setShopbillno(shipment.getShipmentCode());
            //查询尺寸
            Response<List<SkuTemplate>> bySkuCodes = skuTemplateReadService.findBySkuCodes(Lists.newArrayList(shipmentItem.getSkuCode()));
            if (bySkuCodes.isSuccess()) {
                List<SkuTemplate> skuTemplates = bySkuCodes.getResult();
                if (skuTemplates.size() > 0) {
                    SkuTemplate skuTemplate = skuTemplates.get(0);
                    String attrsJson = skuTemplate.getAttrsJson();
                    String sizeName = getSizeName(attrsJson);
                    item.setSizename(sizeName);

                    Map<String, String> extra = skuTemplate.getExtra();
                    String materialCode = extra.get("materialCode");
                    item.setMaterialcode(materialCode);
                }
            }
            item.setSku(shipmentItem.getSkuCode());

            allItems.add(item);
        }

        bizcontent.setTdq(String.valueOf(allItems.size()));
        bizcontent.setItems(allItems);


        request.setBizContent(bizcontent);

        return request;
    }

    public String getSizeName(String attrsJson) {
        List<JSONObject> jsonObjects = JSONObject.parseArray(attrsJson, JSONObject.class);
        for (JSONObject o : jsonObjects) {
            String attrKey = o.getString("attrKey");
            if ("尺码".equals(attrKey)) {
                return o.getString("attrVal");
            }
        }
        return "";
    }


}
