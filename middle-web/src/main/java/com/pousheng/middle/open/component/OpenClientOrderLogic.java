package com.pousheng.middle.open.component;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.job.order.api.OrderReceiver;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.order.dto.*;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 中台订单创建接口
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/3/12
 * open-client
 */
@Slf4j
@Component
public class OpenClientOrderLogic {
    @Autowired
    private OpenOrderConverter openOrderConverter;
    @Autowired
    private OpenShopCacher openShopCacher;
    @Autowired
    private OrderReceiver orderReceiver;
    @Autowired
    private ShopOrderReadService shopOrderReadService;
    @Autowired
    private SkuTemplateReadService skuTemplateReadService;
    /**
     * 订单创建
     * @param orderInfo
     * @return
     */
    public Response<Boolean> createOrder(OpenFullOrderInfo orderInfo){
        //数据校验
        this.busiValidate(orderInfo);
        //校验订单号是否已经存在
        this.validateOutOrderIdAndChannel(orderInfo.getOrder().getOutOrderId(),orderInfo.getOrder().getChannel());
        //校验货品条码是否重复
        this.validateDuplicateSkuCode(orderInfo);
        //校验货品条码是否存在
        for (OpenFullOrderItem openFullOrderItem:orderInfo.getItem()){
            this.validateSkuTemplate(openFullOrderItem.getSkuCode());
        }
        //日志打印
        log.info("Begin to create middle order,outOrderId is {}",orderInfo.getOrder().getOutOrderId());
        //组装参数,查询店铺信息
        OpenShop openShop = openShopCacher.findById(orderInfo.getOrder().getShopId());
        //组装订单信息
        OpenClientFullOrder openClientFullOrder = openOrderConverter.transform(orderInfo, openShop);
        //插入订单
        orderReceiver.receiveOrder(OpenClientShop.from(openShop), Lists.newArrayList(openClientFullOrder));
        log.info("End to create middle order,outOrderId is {}",orderInfo.getOrder().getOutOrderId());
        return Response.ok(Boolean.TRUE);
    }

    /**
     * 批量订单创建
     * @param orderInfos
     * @return
     */
    public Response<Boolean> batchCreateOrder(List<OpenFullOrderInfo> orderInfos){
        //日志打印
        log.info("Begin to create middle order");
        //业务校验
        for (OpenFullOrderInfo orderInfo:orderInfos){
            this.busiValidate(orderInfo);
            //校验订单是否存在
            this.validateOutOrderIdAndChannel(orderInfo.getOrder().getOutOrderId(),orderInfo.getOrder().getChannel());
            //校验货品条码是否重复
            this.validateDuplicateSkuCode(orderInfo);
            //校验货品条码是否有效
            for (OpenFullOrderItem openFullOrderItem:orderInfo.getItem()){
                this.validateSkuTemplate(openFullOrderItem.getSkuCode());
            }
            //组装参数,查询店铺信息
            OpenShop openShop = openShopCacher.findById(orderInfo.getOrder().getShopId());
        }
        //根据店铺将订单归组
        ListMultimap<Long, OpenFullOrderInfo> mulitMaps =  Multimaps.index(orderInfos, new Function<OpenFullOrderInfo, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable OpenFullOrderInfo orderInfo) {
                return orderInfo.getOrder().getShopId();
            }
        });
        for (Long shopId:mulitMaps.keySet()){
            OpenShop openShop = openShopCacher.findById(shopId);
            List<OpenFullOrderInfo> openFullOrderInfos = mulitMaps.get(shopId);
            List<OpenClientFullOrder> openClientFullOrders  = Lists.newArrayList();
            for (OpenFullOrderInfo openFullOrderInfo:openFullOrderInfos){
                OpenClientFullOrder openClientFullOrder = openOrderConverter.transform(openFullOrderInfo, openShop);
                openClientFullOrders.add(openClientFullOrder);
            }
            //插入订单
            orderReceiver.receiveOrder(OpenClientShop.from(openShop), openClientFullOrders);
        }
        log.info("End to create middle order");
        return Response.ok(Boolean.TRUE);
    }

    public void busiValidate(OpenFullOrderInfo orderInfo){
        if (Objects.isNull(orderInfo)){
            throw new ServiceException("orders.is.null");
        }
        if (Objects.isNull(orderInfo.getOrder())){
            throw new ServiceException("shop.order.is.null");
        }
        OpenFullOrder openFullOrder = orderInfo.getOrder();
        //来源渠道
        if (StringUtils.isEmpty(openFullOrder.getChannel())){
            throw new ServiceException("channel.is.empty");
        }
        //外部订单号
        if (StringUtils.isEmpty(openFullOrder.getOutOrderId())){
            throw new ServiceException("outOrderId.is.empty");
        }
        //订单创建时间
        if (StringUtils.isEmpty(openFullOrder.getCreatedAt())){
            throw new ServiceException("outCreateDate.is.empty");
        }
        //买家名称
        if (StringUtils.isEmpty(openFullOrder.getBuyerName())){
            throw new ServiceException("buyerName.is.empty");
        }
        //买家备注
        if (StringUtils.isEmpty(openFullOrder.getBuyerMobile())){
            throw new ServiceException("buyerMobile.is.empty");
        }
        //店铺id
        if (Objects.isNull(openFullOrder.getShopId())){
            throw new ServiceException("shopId.is.empty");
        }
        //店铺名称
        if (Objects.isNull(openFullOrder.getShopName())){
            throw new ServiceException("shopName.is.empty");
        }
        //物流公司代码
        if (Objects.isNull(openFullOrder.getOrderExpressCode())){
            throw new ServiceException("orderExpressCode.is.empty");
        }
        //运费原价金额
        if (Objects.isNull(openFullOrder.getOriginShipFee())){
            throw new ServiceException("originShipFee.is.empty");
        }
        //实际运费金额
        if (Objects.isNull(openFullOrder.getShipFee())){
            throw new ServiceException("shipFee.is.empty");
        }
        //订单优惠
        if (Objects.isNull(openFullOrder.getDiscount())){
            throw new ServiceException("discount.is.empty");
        }
//        //支付方式
//        if (Objects.isNull(openFullOrder.getPayType())){
//            throw new ServiceException("payType.is.empty");
//        }
//        //支付渠道名称
//        if (Objects.isNull(openFullOrder.getPaymentChannelName())){
//            throw new ServiceException("paymentChannelName.is.empty");
//        }
//        //支付流水号
//        if (Objects.isNull(openFullOrder.getPaymentSerialNo())){
//            throw new ServiceException("paymentSerialNo.is.empty");
//        }
        if (Objects.isNull(orderInfo.getItem())){
            throw new ServiceException("item.is.null");
        }
        //货品条码信息
        List<OpenFullOrderItem> orderItems = orderInfo.getItem();
        if(orderItems.isEmpty()){
            throw new ServiceException("item.is.empty");
        }
        for (OpenFullOrderItem item:orderItems){
            //校验货品条码
            if (StringUtils.isEmpty(item.getSkuCode())){
                throw new ServiceException("skuCode.is.empty");
            }
            //校验数量
            if (Objects.isNull(item.getQuantity())){
                throw new ServiceException("quantity.is.empty");
            }
            //校验原价
            if (Objects.isNull(item.getOriginFee())){
                throw new ServiceException("originFee.is.empty");
            }
            //校验折扣
            if (Objects.isNull(item.getDiscount())){
                throw new ServiceException("itemDiscount.is.empty");
            }
        }
        //地址信息
        if (Objects.isNull(orderInfo.getAddress())){
            throw new ServiceException("address.is.null");
        }
        OpenFullOrderAddress address = orderInfo.getAddress();
        //校验省份
        if (StringUtils.isEmpty(address.getProvince())){
            throw new ServiceException("province.is.empty");
        }
        //校验城市
        if (StringUtils.isEmpty(address.getCity())){
            throw new ServiceException("city.is.empty");
        }
        //校验县区
        if (StringUtils.isEmpty(address.getRegion())){
            throw new ServiceException("region.is.empty");
        }
        //校验详细地址
        if (StringUtils.isEmpty(address.getDetail())){
            throw new ServiceException("addressDetail.is.empty");
        }
        //校验收货人地址
        if (StringUtils.isEmpty(address.getReceiveUserName())){
            throw new ServiceException("receiveUserName.is.empty");
        }
        //校验手机号
        if (StringUtils.isEmpty(address.getMobile())){
            throw new ServiceException("mobile.is.empty");
        }
    }

    private void validateOutOrderIdAndChannel(String outOrderId,String channel){
        Response<Optional<ShopOrder>> shopOrderOptional = shopOrderReadService.findByOutIdAndOutFrom(outOrderId,channel);
        if (!shopOrderOptional.isSuccess()){
            throw new ServiceException("find.shopOrder.failed");
        }
        if (shopOrderOptional.getResult().isPresent()){
            throw new ServiceException("shopOrder.already.exist");
        }
    }


    private void validateSkuTemplate(String skuCode){
        Response<List<SkuTemplate>> response =  skuTemplateReadService.findBySkuCodes(Lists.newArrayList(skuCode));
        if (!response.isSuccess()){
            throw new ServiceException("find.skuCode.failed");
        }
        List<SkuTemplate> skuTemplates = response.getResult();
        if (skuTemplates.isEmpty()){
            throw new ServiceException("no.exist.skuCode");
        }
        List<SkuTemplate> skuTemplateList = skuTemplates.stream().filter(Objects::nonNull).filter(skuTemplate -> !Objects.equals(skuTemplate.getStatus(),-3)).collect(Collectors.toList());
        if (skuTemplateList.isEmpty()){
            throw new ServiceException("invalid.skuCode");
        }
    }

    private void validateDuplicateSkuCode(OpenFullOrderInfo openFullOrderInfo){
        List<OpenFullOrderItem> items = openFullOrderInfo.getItem();
        List<String> skuCodeList = items.stream().filter(Objects::nonNull).map(OpenFullOrderItem::getSkuCode).collect(Collectors.toList());
        Set<String> skuCodeSet = items.stream().filter(Objects::nonNull).map(OpenFullOrderItem::getSkuCode).collect(Collectors.toSet());
        if (skuCodeList.size()!=skuCodeSet.size()){
            throw new ServiceException("duplicate.sku.code");
        }
    }
}
