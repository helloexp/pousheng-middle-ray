package com.pousheng.middle.consume.index.processor.impl.order.builder;

import com.pousheng.middle.consume.index.processor.impl.CommonBuilder;
import com.pousheng.middle.consume.index.processor.impl.order.dto.OrderDocument;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-17 11:45<br/>
 */
@Slf4j
public class OrderDocumentBuilder extends CommonBuilder {
    public OrderDocument create(ShopOrder shopOrder) {
        OrderDocument orderDocument = new OrderDocument();
        orderDocument.setId(shopOrder.getId());
        orderDocument.setOutId(shopOrder.getOutId());
        orderDocument.setOutFrom(shopOrder.getOutFrom());
        orderDocument.setOrderCode(shopOrder.getOrderCode());
        orderDocument.setCompanyId(shopOrder.getCompanyId());
        orderDocument.setShopId(shopOrder.getShopId());
        orderDocument.setShopName(shopOrder.getShopName());
        orderDocument.setBuyerName(shopOrder.getBuyerName());
        orderDocument.setBuyerPhone(shopOrder.getOutBuyerId());
        orderDocument.setHandleStatus(shopOrder.getHandleStatus());
        orderDocument.setStatus(shopOrder.getStatus());
        orderDocument.setBuyerNode(shopOrder.getBuyerNote());

        orderDocument.setOutCreatedAt(timeString(shopOrder.getOutCreatedAt()));
        orderDocument.setUpdatedAt(timeString(shopOrder.getUpdatedAt()));
        orderDocument.setCreatedAt(timeString(shopOrder.getCreatedAt()));

        if (!CollectionUtils.isEmpty(shopOrder.getExtra())) {
            String status = shopOrder.getExtra().getOrDefault("ecpOrderStatus", "0");
            if ("-1".equals(status)) {
                orderDocument.setEcpOrderStatus(-1);
            } else if ("1".equals(status)) {
                orderDocument.setEcpOrderStatus(1);
            } else {
                orderDocument.setEcpOrderStatus(0);
            }
        }
        return orderDocument;
    }
}
