package com.pousheng.middle.open.component;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.open.api.constant.ExtraKeyConstant;
import com.pousheng.middle.order.constant.TradeConstants;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.constants.ParanaTradeConstants;
import io.terminus.open.client.item.model.PushedItem;
import io.terminus.open.client.item.service.PushedItemReadService;
import io.terminus.open.client.order.dto.*;
import io.terminus.open.client.order.enums.OpenClientOrderPayType;
import io.terminus.open.client.order.enums.OpenClientOrderStatus;
import io.terminus.parana.common.constants.JitConsts;
import io.terminus.parana.order.dto.fsm.OrderStatus;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/16
 * open-client
 */
@Component
@Slf4j
public class OpenOrderConverter {
    private static final ImmutableBiMap<OpenClientOrderStatus, Integer> ORDER_STATUS_MAPPING_BY_NUMBER =
            ImmutableBiMap.<OpenClientOrderStatus, Integer>builder()
                    .put(OpenClientOrderStatus.PAID, 1).build();


    private final static DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyyMMddHHmmss");

    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;

    @RpcConsumer
    private MappingReadService mappingReadService;

    @RpcConsumer(check = "false")
    private PushedItemReadService pushedItemReadService;

    /**
     * 组装参数
     *
     * @param openFullOrderInfo
     * @return
     */
    public OpenClientFullOrder transform(OpenFullOrderInfo openFullOrderInfo, OpenShop openShop) {

        OpenClientFullOrder fullOrder = new OpenClientFullOrder();
        OpenFullOrder order = openFullOrderInfo.getOrder();
        fullOrder.setOrderId(String.valueOf(order.getOutOrderId()));
        fullOrder.setStatus(handleResultStats(order));
        //添加绩效店铺
        fullOrder.setPerformanceShopCode(order.getPerformanceShopCode());
        fullOrder.setShipFee(Math.toIntExact(order.getShipFee() == null ? 0 : order.getShipFee()));
        fullOrder.setOriginShipFee(Math.toIntExact(order.getOriginShipFee() == null ? 0 : order.getOriginShipFee()));
        fullOrder.setBuyerName(order.getBuyerName());
        fullOrder.setBuyerRemark(order.getBuyerNote());
        //订单类型
        fullOrder.setType(order.getType());
        if (order.getPayType() != null) {
            fullOrder.setPayType(order.getPayType().equals(1) ?
                    OpenClientOrderPayType.ONLINE : OpenClientOrderPayType.CASH_ON_DELIVERY);
        }
        //添加物流代码
        Map<String, String> extra = (fullOrder.getExtra() == null ? Maps.newHashMap() : fullOrder.getExtra());
        if (!StringUtils.isEmpty(order.getOrderExpressCode())) {
            extra.put("orderHkExpressCode", order.getOrderExpressCode());
            extra.put("orderHkExpressName", order.getOrderHkExpressName());
        }
        //客服备注
        if (StringUtils.hasText(order.getSellerRemark())) {
            extra.put("customerServiceNote", order.getSellerRemark());
        }
        //新增加或者导入订单标记
        extra.put("importOrder","true");

        // 云聚类型的订单 接收第三方订单号字段outId,存在extra_json中
        if (order.getChannel().contains("yunju")) {
            extra.put(ExtraKeyConstant.YJ_OUTID, order.getOutId());
            extra.put(ExtraKeyConstant.SHIPMENT_TYPE, order.getShipmentType() + "");
//             是否关心库存
            extra.put(ExtraKeyConstant.IS_CARESTOCK, StringUtils.isEmpty(order.getIsCareStock()) ? "N" : order.getIsCareStock());
            // 是否传hk
            extra.put(ExtraKeyConstant.IS_SYNCHK, StringUtils.isEmpty(order.getIsSyncHk()) ? "N" : order.getIsSyncHk());

            //bbc 物流公司代码
            extra.put(ExtraKeyConstant.ORDER_EXPRESS_CODE,order.getOrderExpressCode());
            //bbc 物流公司名称
            extra.put(ExtraKeyConstant.ORDER_EXPRESS_NAME,order.getOrderHkExpressName());
            //bbc 物流号
            extra.put(ExtraKeyConstant.SHIPMENT_SERIAL_NO,order.getShipmentSerialNo());
            // 拣货单号
            extra.put(ExtraKeyConstant.JIT_ORDER_ID, order.getJitOrderId());
            // 客商编码
            extra.put(ExtraKeyConstant.VEND_CUST_Code, order.getVendCustCode());
            // 目的仓编码
            extra.put(ExtraKeyConstant.INTER_STOCK_CODE, order.getInterStockCode());
            // 目的仓名称
            extra.put(ExtraKeyConstant.INTER_STOCK_NAME, order.getInterStockName());
            // 下游单号
            extra.put(ExtraKeyConstant.PRE_FINISH_BILLO, order.getPreFinishBillo());
            // 承运商
            extra.put(ExtraKeyConstant.FREIGHT_COMPANY, order.getFreightCompany());
            // 批次号
            extra.put(ExtraKeyConstant.BATCH_NO, order.getBatchNo());
            // 批次描述
            extra.put(ExtraKeyConstant.BATCH_MARK, order.getBatchMark());
            // 渠道代码
            extra.put(ExtraKeyConstant.CHANNEL_CODE, order.getChannelCode());

            //预计出/入库日期
            extra.put(ExtraKeyConstant.EXPECT_DATE,order.getExpectDate());
            //发运编码
            extra.put(ExtraKeyConstant.TRANSPORT_METHOD_CODE,order.getTransportMethodCode());
            //发运方式
            extra.put(ExtraKeyConstant.TRANSPORT_METHOD_NAME,order.getTransportMethodName());
            //品牌
            extra.put(ExtraKeyConstant.CARD_REMARK,order.getCardRemark());
            //时效订单号
            extra.put(ExtraKeyConstant.REALTIME_ORDER_IDS,order.getRealtimeOrderIds());
            //订单来源
            extra.put(JitConsts.JIT_CHANNEL,order.getChannel());
            //bbc 物流公司代码
            extra.put(ExtraKeyConstant.ORDER_EXPRESS_CODE,order.getOrderExpressCode());
            //bbc 物流公司名称
            extra.put(ExtraKeyConstant.ORDER_EXPRESS_NAME,order.getOrderHkExpressName());
            //bbc 物流号
            extra.put(ExtraKeyConstant.SHIPMENT_SERIAL_NO,order.getShipmentSerialNo());

            if (Arguments.isNull(order.getType()) || Objects.equals(order.getType(), 1)) {
                extra.put(ExtraKeyConstant.YJ_TYPE, ExtraKeyConstant.YJ_BBC);
            }
            if (Objects.equals(order.getType(), 2)) {
                extra.put(ExtraKeyConstant.YJ_TYPE, ExtraKeyConstant.YJ_JIT);
            }
        }
        fullOrder.setExtra(extra);

        fullOrder.setSellerRemark(order.getSellerRemark());
        DateTime dt = DateTime.parse(order.getCreatedAt(), DFT);
        fullOrder.setCreatedAt(dt.toDate());

        OpenClientPaymentInfo paymentInfo = new OpenClientPaymentInfo();
        if (StringUtils.hasText(order.getPaymentDate())) {
            DateTime payAt = DateTime.parse(order.getCreatedAt(), DFT);
            paymentInfo.setPaidAt(payAt.toDate());
        }
        paymentInfo.setPayChannel(order.getPaymentChannelName());
        paymentInfo.setPaySerialNo(order.getPaymentSerialNo());
        fullOrder.setPaymentInfo(paymentInfo);

        //order items
        List<OpenFullOrderItem> items = openFullOrderInfo.getItem();
        List<OpenClientOrderItem> orderItems = Lists.newArrayListWithCapacity(items.size());

        //订单级别的优惠总和
        Long orderDisCount = MoreObjects.firstNonNull(order.getDiscount(), 0L);
        for (OpenFullOrderItem itemInfo : items) {
            OpenClientOrderItem orderItem = new OpenClientOrderItem();
            orderItem.setStatus(OpenClientOrderStatus.PAID);
            orderItem.setItemName(itemInfo.getItemName());

            //根据spuId+shopId查询外部商品id  (parana_pushed_items表)
            Long spuId = findSpuIdBySkuCode(itemInfo.getSkuCode());
            Response<PushedItem> pushedItemResponse = pushedItemReadService.findByItemIdAndOpenShopId(spuId, order.getShopId());
            if (pushedItemResponse.isSuccess() && Arguments.notNull(pushedItemResponse.getResult())) {
                orderItem.setItemId(pushedItemResponse.getResult().getChannelItemId());
            }

            orderItem.setSkuCode(itemInfo.getSkuCode());
            orderItem.setOrderId(String.valueOf(itemInfo.getOutSkuorderId() == null ? "" : itemInfo.getOutSkuorderId()));
            //商品级别的优惠包括商品的优惠以及订单级别的优惠的分摊
            orderItem.setDiscount(MoreObjects.firstNonNull(itemInfo.getDiscount(), 0L).intValue()+this.calcSkuDiscount(orderDisCount,order.getOriginFee(),itemInfo));
            orderItem.setPrice(Integer.valueOf(String.valueOf(itemInfo.getOriginFee())));
            orderItem.setQuantity(itemInfo.getQuantity());
            // 云聚传过来的订单信息中子单的价格和优惠都已经计算好
            if (order.getChannel().contains("yunju")) {
                orderItem.setDiscount(itemInfo.getDiscount().intValue());
            }
            orderItem.setOutOrderId(itemInfo.getVipsOrderId());
            orderItems.add(orderItem);
        }
        fullOrder.setItems(orderItems);

        //优惠金额=支付单上的平台优惠 + 订单上的店铺优惠 + 积分抵现 + 商品优惠 (这里没有算运费优惠)
        fullOrder.setDiscount(Math.toIntExact(order.getOriginFee() - order.getFee()));
        //订单原价
        fullOrder.setOriginFee(order.getOriginFee());
        fullOrder.setFee(order.getFee());
        // 云聚传过来的数据都是计算好的且订单优惠金额都已经计算好，
        // 不同于我们现有的计算模式，这里后台改变下适应我们的模式
        if (order.getChannel().contains("yunju")) {
            // 云聚传过来的实际支付金额为商品总价+运费-优惠部分
            // 而我们数据库中设置的实际支付指的是商品总价-优惠部分
            fullOrder.setFee(order.getFee() - order.getShipFee());
            fullOrder.setDiscount(MoreObjects.firstNonNull(order.getDiscount(), 0L).intValue());
        }

        //consignee
        OpenFullOrderAddress address = openFullOrderInfo.getAddress();
        OpenClientOrderConsignee consignee = new OpenClientOrderConsignee();
        consignee.setName(address.getReceiveUserName());
        consignee.setMobile(address.getMobile());
        consignee.setTelephone(address.getPhone());
        consignee.setProvince(address.getProvince());
        consignee.setCity(address.getCity());
        consignee.setRegion(address.getRegion());
        consignee.setDetail(address.getDetail());
        fullOrder.setConsignee(consignee);

        OpenFullOrderInvoice invoice = openFullOrderInfo.getInvoice();
        if (Arguments.notNull(invoice)) {
            Map<String, String> detailMap = Maps.newHashMap();
            OpenClientOrderInvoice openClientOrderInvoice = new OpenClientOrderInvoice();
            openClientOrderInvoice.setType(invoice.getInvoiceType());
            openClientOrderInvoice.setTitle(invoice.getTitleType());
            if (StringUtils.hasText(invoice.getTaxRegisterNo())) {
                detailMap.put("taxRegisterNo", invoice.getTaxRegisterNo());
            }
            if (StringUtils.hasText(invoice.getCompanyName())) {
                detailMap.put("companyName", invoice.getCompanyName());
            }
            if (StringUtils.hasText(invoice.getRegisterPhone())) {
                detailMap.put("registerPhone", invoice.getRegisterPhone());
            }
            if (StringUtils.hasText(invoice.getRegisterAddress())) {
                detailMap.put("registerAddress", invoice.getRegisterAddress());
            }
            if (StringUtils.hasText(invoice.getRegisterBank())) {
                detailMap.put("registerBank", invoice.getRegisterBank());
            }
            if (StringUtils.hasText(invoice.getBankAccount())) {
                detailMap.put("bankAccount", invoice.getBankAccount());
            }
            if (StringUtils.hasText(invoice.getTitleType())) {
            }
            {
                detailMap.put("titleType", invoice.getTitleType());
            }
            if (StringUtils.hasText(invoice.getInvoiceType())) {
                detailMap.put("type", invoice.getInvoiceType());
            }
            if (StringUtils.hasText(invoice.getEmail())) {
                detailMap.put("email", invoice.getEmail());
            }
            if (StringUtils.hasText(invoice.getMobile())) {
                detailMap.put("mobile", invoice.getMobile());
            }
            openClientOrderInvoice.setDetail(detailMap);
            fullOrder.setInvoice(openClientOrderInvoice);
        }

        if(!Strings.isNullOrEmpty(order.getStockId())){
            Map<String,String> extraMap = fullOrder.getExtra();
            if(CollectionUtils.isEmpty(fullOrder.getExtra())){
                extraMap = Maps.newHashMap();
            }
            log.debug("stockId: {}", order.getStockId());
            String visualWarehouseCode = openShop.getExtra().get(TradeConstants.VISUAL_WAREHOUSE_CODE);
            // 没有虚仓才按照指定仓库寻源，如果有虚仓默认不指定仓库
            if (StringUtils.isEmpty(visualWarehouseCode) || !Objects.equals(order.getStockId(), visualWarehouseCode)) {
                extraMap.put(ParanaTradeConstants.ASSIGN_WAREHOUSE_ID,order.getStockId());
                extraMap.put("importOrder","false");
            }
            log.debug("订单 extra 信息:{}", extra);
        }
        return fullOrder;
    }

    public OpenClientOrderStatus handleResultStats(OpenFullOrder order) {


        OrderStatus orderStatus = OrderStatus.fromInt(order.getStatus());

        OpenClientOrderStatus paid = OpenClientOrderStatus.PAID;
        OpenClientOrderStatus confirmed = OpenClientOrderStatus.CONFIRMED;

        switch (orderStatus) {
            case PAID:
                return paid;
            case CONFIRMED:
                return confirmed;

            default:
                throw new ServiceException("parana.status.invalid");
        }
    }

    public Long findSpuIdBySkuCode(String skuCode) {
        Response<List<SkuTemplate>> sP = skuTemplateReadService.findBySkuCodes(Lists.newArrayList(skuCode));
        if (!sP.isSuccess()) {
            log.error("find skuTemplate failed, skuCode is {},caused by {}", skuCode, sP.getError());
            throw new ServiceException("skuTemplate(skuCode="+skuCode+") not found");
        }
        List<SkuTemplate> skuTemplates = sP.getResult();
        List<SkuTemplate> skuTemplateList = skuTemplates.stream().filter(skuTemplate -> !Objects.equals(skuTemplate.getStatus(), -3)).collect(Collectors.toList());
        if (skuTemplateList == null || skuTemplateList.isEmpty()) {
            log.error("sku template status is error,cant not be -3,skuCode is {}", skuCode);
            throw new ServiceException("skuTemplate(skuCode="+skuCode+") not found");
        }
        return skuTemplateList.get(0).getSpuId();
    }

    /**
     * 2，
     *
     * @param orderDiscount     该优惠是order.disCount减去所有商品的discount（仅仅是订单级别的优惠，如支付优惠）
     * @param originFee         订单原价金额
     * @param openFullOrderItem 子单信息
     * @return
     */
    private Integer calcSkuDiscount(Long orderDiscount, Long originFee, OpenFullOrderItem openFullOrderItem) {
        return new BigDecimal(orderDiscount).multiply(new BigDecimal(openFullOrderItem.getOriginFee())).multiply(new BigDecimal(openFullOrderItem.getQuantity())).divide(new BigDecimal(originFee), 10, RoundingMode.HALF_DOWN).intValue();
    }

}
