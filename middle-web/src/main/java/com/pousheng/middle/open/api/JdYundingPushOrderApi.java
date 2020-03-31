package com.pousheng.middle.open.api;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.job.order.api.OrderReceiver;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.order.dto.OpenClientFullOrder;
import io.terminus.open.client.order.dto.OpenClientOrderItem;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 京东云鼎订单推送接口
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/15
 * pousheng-middle
 */
@OpenBean
@Slf4j
public class JdYundingPushOrderApi {
    @Autowired
    private OrderReceiver orderReceiver;
    @Autowired
    private MiddleOrderWriteService middleOrderWriteService;
    @Autowired
    private ShopOrderReadService shopOrderReadService;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private OpenShopReadService openShopReadService;

    /**
     * 京东云鼎订单拉取服务
     */
    @OpenMethod(key = "jd.order.push.api", paramNames = {"shopId", "openClientFullOrders"}, httpMethods = RequestMethod.POST)
    public void pushJdOrder(Long shopId,
                            @NotEmpty(message = "openClientFullOrders.is.null") String openClientFullOrders) {
        log.info("JD-YUNDING-SYNC-ORDER-START shopId is {}, openClientFullOrders is:{} ", shopId, openClientFullOrders);
        //订单列表
        List<OpenClientFullOrder> openClientFullOrderList = JsonMapper.nonEmptyMapper()
                .fromJson(openClientFullOrders, JsonMapper.nonEmptyMapper()
                        .createCollectionType(List.class, OpenClientFullOrder.class));
        //查询店铺服务
        Response<OpenShop> openShopResponse = openShopReadService.findById(shopId);
        if (!openShopResponse.isSuccess()) {
            log.error("find open shop failed,shopId is {},caused by {}", shopId, openShopResponse.getError());
            throw new OPServerException(200, openShopResponse.getError());
        }
        OpenClientShop openClientShop = OpenClientShop.from(openShopResponse.getResult());
        //订单处理
        orderReceiver.receiveOrder(openClientShop, openClientFullOrderList);
        log.info("JD-YUNDING-SYNC-ORDER-END param: shopId is {}, openClientFullOrders is:{} ", shopId, openClientFullOrders);
    }

    /**
     * 更新订单金额
     * @param shopId 店铺id
     * @param openClientFullOrders 订单集合
     */
    @OpenMethod(key = "jd.order.update.amount.api", paramNames = {"shopId", "openClientFullOrders"}, httpMethods = RequestMethod.POST)
    public void pushUpdateJdOrderAmount(Long shopId,
                                        @NotEmpty(message = "openClientFullOrders.is.null") String openClientFullOrders ){
        log.info("JD-YUNDING-SYNC-UPDATE-ORDER-AMOUNT-START shopId is {}, openClientFullOrders is:{} ", shopId, openClientFullOrders);
        List<OpenClientFullOrder> openClientFullOrderList = JsonMapper.nonEmptyMapper()
                .fromJson(openClientFullOrders, JsonMapper.nonEmptyMapper()
                        .createCollectionType(List.class, OpenClientFullOrder.class));
        for (OpenClientFullOrder openClientFullOrder:openClientFullOrderList){
            Response<OpenShop> openShopResponse = openShopReadService.findById(shopId);
            OpenShop openShop = openShopResponse.getResult();
            Response<Optional<ShopOrder>> shopOrderResponse = shopOrderReadService.findByOutIdAndOutFrom(openClientFullOrder.getOrderId(),openShop.getChannel());
            Optional<ShopOrder> shopOrderOptional = shopOrderResponse.getResult();
            if (!shopOrderOptional.isPresent()){
                continue;
            }
            ShopOrder shopOrder = shopOrderResponse.getResult().get();

            ShopOrder newShopOrder = new ShopOrder();
            newShopOrder.setId(shopOrder.getId());
            newShopOrder.setFee(openClientFullOrder.getFee());
            newShopOrder.setDiscount(openClientFullOrder.getDiscount());
            newShopOrder.setShipFee(openClientFullOrder.getShipFee());
            newShopOrder.setOriginShipFee(openClientFullOrder.getShipFee());
            Map<String,String> fullOrderExtra = openClientFullOrder.getExtra();
            String platformDiscount = fullOrderExtra.get(TradeConstants.PLATFORM_DISCOUNT_FOR_SHOP);
            if (!StringUtils.isEmpty(platformDiscount)){
                Map<String,String> shopOrderExtra = shopOrder.getExtra();
                shopOrderExtra.put(TradeConstants.PLATFORM_DISCOUNT_FOR_SHOP,platformDiscount);
                newShopOrder.setExtra(shopOrderExtra);
            }

            Response<Boolean> shopOrderR = middleOrderWriteService.updateShopOrder(newShopOrder);
            if (!shopOrderR.isSuccess()) {
                log.error("shopOrder failed,id is {}", shopOrder.getId());
            } else {
                List<OpenClientOrderItem> items = openClientFullOrder.getItems();

                Map<String,Long> skuIdAndShareDiscount = Maps.newHashMap();
                Long fees = 0L;
                for (OpenClientOrderItem openClientOrderItem:items){
                    fees += (openClientOrderItem.getPrice()*openClientOrderItem.getQuantity()-openClientOrderItem.getDiscount());
                }
                Long alreadyShareDiscout = 0L;
                for (int i = 0;i<items.size()-1;i++){
                    Long itemShareDiscount =  (((items.get(i).getPrice()*items.get(i).getQuantity()
                            -items.get(i).getDiscount()))*Long.valueOf(platformDiscount))/fees;
                    alreadyShareDiscout += itemShareDiscount;
                    skuIdAndShareDiscount.put(items.get(i).getSkuId(),itemShareDiscount);
                }
                //计算剩余没有分配的平台优惠
                Long remainShareDiscount = Long.valueOf(platformDiscount)-alreadyShareDiscout;


                List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByShopOrderId(shopOrder.getId());
                for (SkuOrder skuOrder : skuOrders) {
                    for (OpenClientOrderItem item : items) {
                        if (Objects.equals(skuOrder.getOutSkuId(), item.getSkuId())) {
                            log.info("update skuOrder");
                            SkuOrder newSkuOrder = new SkuOrder();
                            newSkuOrder.setId(skuOrder.getId());
                            newSkuOrder.setOriginFee(Long.valueOf(item.getPrice() * item.getQuantity()));
                            newSkuOrder.setDiscount(Long.valueOf(item.getDiscount()));
                            newSkuOrder.setFee(newSkuOrder.getOriginFee() - newSkuOrder.getDiscount());

                            Long shareDiscount = skuIdAndShareDiscount.get(skuOrder.getOutSkuId());
                            Map<String, String> skuOrderExtra = skuOrder.getExtra();
                            //子订单插入平台优惠金额
                            if (shareDiscount==null){
                                skuOrderExtra.put(TradeConstants.PLATFORM_DISCOUNT_FOR_SKU, String.valueOf(remainShareDiscount));
                            }else{
                                skuOrderExtra.put(TradeConstants.PLATFORM_DISCOUNT_FOR_SKU, String.valueOf(shareDiscount));
                            }
                            newSkuOrder.setExtra(skuOrderExtra);

                            Response<Boolean> skuOrderR = middleOrderWriteService.updateSkuOrder(newSkuOrder);
                            if (!skuOrderR.isSuccess()) {
                                log.error("skuOrder failed,id is", newSkuOrder.getId());
                            }
                        } else {
                            log.info("do not update skuOrder");
                        }
                    }
                }
            }
        }

        log.info("JD-YUNDING-SYNC-UPDATE-ORDER-AMOUNT-END param: shopId is {}, openClientFullOrders is:{} ", shopId, openClientFullOrders);
    }

}

