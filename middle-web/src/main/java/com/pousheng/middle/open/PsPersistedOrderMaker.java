package com.pousheng.middle.open;

import com.google.common.collect.Lists;
import com.pousheng.middle.order.enums.OrderWaitHandleType;
import io.terminus.parana.order.component.DefaultPersistedOrderMaker;
import io.terminus.parana.order.dto.RichOrder;
import io.terminus.parana.order.dto.RichSkusByShop;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.shop.model.Shop;
import java.util.Iterator;
import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/10/10
 * pousheng-middle
 */
public class PsPersistedOrderMaker extends DefaultPersistedOrderMaker {

    public PsPersistedOrderMaker(OrderLevel orderReceiverInfoLevel, OrderLevel orderInvoiceLevel) {
        super(orderReceiverInfoLevel, orderInvoiceLevel);
    }

    protected List<ShopOrder> retrieveShopOrders(RichOrder richOrder) {
        List<RichSkusByShop> richSkusByShops = richOrder.getRichSkusByShops();
        List<ShopOrder> shopOrders = Lists.newArrayListWithCapacity(richSkusByShops.size());
        Iterator var4 = richSkusByShops.iterator();

        while(var4.hasNext()) {
            RichSkusByShop richSkusByShop = (RichSkusByShop)var4.next();
            ShopOrder shopOrder = new ShopOrder();
            Shop shop = richSkusByShop.getShop();
            shopOrder.setOutId(richSkusByShop.getOuterOrderId());
            shopOrder.setOutCreatedAt(richSkusByShop.getOutCreatedAt());
            shopOrder.setOutFrom(richSkusByShop.getOutFrom());
            shopOrder.setHandleStatus(OrderWaitHandleType.ORIGIN_STATUS_SAVE.value());
            shopOrder.setBuyerName(richOrder.getBuyer().getName());
            shopOrder.setBuyerId(richOrder.getBuyer().getId());
            shopOrder.setShopId(shop.getId());
            shopOrder.setCompanyId(richOrder.getCompanyId());
            shopOrder.setRefererId(richOrder.getRefererId());
            shopOrder.setRefererName(richOrder.getRefererName());
            shopOrder.setShopName(shop.getName());
            shopOrder.setOutBuyerId(richSkusByShop.getReceiverInfo().getMobile());
            shopOrder.setOutShopId(shop.getOuterId());
            shopOrder.setStatus(richSkusByShop.getOrderStatus());
            shopOrder.setType(richSkusByShop.getOrderType());
            shopOrder.setFee(richSkusByShop.getFee());
            shopOrder.setOriginFee(richSkusByShop.getOriginFee());
            shopOrder.setDiscount(richSkusByShop.getDiscount());
            shopOrder.setShipFee(richSkusByShop.getShipFee());
            shopOrder.setOriginShipFee(richSkusByShop.getOriginShipFee());
            shopOrder.setIntegral(richSkusByShop.getIntegral());
            shopOrder.setBalance(richSkusByShop.getBalance());
            shopOrder.setPayType(richOrder.getPayType());
            shopOrder.setBuyerNote(richSkusByShop.getBuyerNote());
            shopOrder.setChannel(richSkusByShop.getChannel());
            shopOrder.setCommissionRate(richSkusByShop.getCommissionRate());
            shopOrder.setDistributionRate(richSkusByShop.getDistributionRate());
            shopOrder.setTags(richSkusByShop.getTags() == null ? richOrder.getTags() : richSkusByShop.getTags());
            shopOrder.setExtra(richSkusByShop.getExtra() == null ? richOrder.getExtra() : richSkusByShop.getExtra());
            shopOrder.setCommented(Boolean.FALSE);
            shopOrders.add(shopOrder);
        }

        return shopOrders;
    }
}
