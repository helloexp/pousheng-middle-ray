package com.pousheng.middle.open.service;

import io.terminus.common.model.Response;
import io.terminus.open.client.center.OrderServiceRegistryCenter;
import io.terminus.open.client.center.order.service.OrderServiceCenterImpl;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.Pagination;
import io.terminus.open.client.order.dto.*;
import io.terminus.open.client.order.enums.OpenClientOrderStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Xiongmin
 * 2019/4/29
 */
@Primary
@Profile("prepub")
@Component
@ConditionalOnProperty(name = "mock.order.fetch.enable", havingValue = "true")
public class MockOrderServiceCenter extends OrderServiceCenterImpl {

    public MockOrderServiceCenter(OpenShopCacher openShopCacher, OrderServiceRegistryCenter orderServiceRegistryCenter) {
        super(openShopCacher, orderServiceRegistryCenter);
    }

    @Override
    public Response<Pagination<OpenClientFullOrder>> searchOrder(Long shopId, OpenClientOrderStatus status,
                                                                 Date startAt, Date endAt, Integer pageNo,
                                                                 Integer pageSize) {
        Pagination<OpenClientFullOrder> pagination = new Pagination<>();
        pagination.setTotal(5000L);
        pagination.setHasNext(true);
        pagination.setData(getMockOpenClientFullOrders(status, pageNo, pageSize));
        return Response.ok(pagination);
    }

    @Override
    public Response<Pagination<OpenClientFullOrder>> searchPresale(Long shopId, Date startAt, Date endAt,
                                                                   Integer pageNo, Integer pageSize) {
        Pagination<OpenClientFullOrder> pagination = new Pagination<>();
        pagination.setTotal(5000L);
        pagination.setHasNext(true);
        pagination.setData(getMockOpenClientFullOrders(OpenClientOrderStatus.NOT_PAID, pageNo, pageSize));
        return Response.ok(pagination);
    }

    private List<OpenClientFullOrder> getMockOpenClientFullOrders(OpenClientOrderStatus orderStatus,
                                                                  Integer pageNo, Integer pageSize) {
        List<OpenClientFullOrder> list = new ArrayList<>();
        Integer startIndex = (pageNo - 1) * pageSize + 1;
        Integer endIndex = startIndex + pageSize;
        Date now = new Date();
        for (; startIndex < endIndex; startIndex++) {
            OpenClientFullOrder openClientFullOrder = mockOpenClientFullOrder(orderStatus, now, startIndex);
            list.add(openClientFullOrder);
        }
        return list;
    }

    private OpenClientFullOrder mockOpenClientFullOrder(OpenClientOrderStatus orderStatus,
                                                        Date now, Integer currentIndex) {
        OpenClientFullOrder openClientFullOrder = new OpenClientFullOrder();
        openClientFullOrder.setStatus(orderStatus);
        // out_order_id 唯一即可
        String orderId = "mock" + orderStatus + now.getTime() + "_" + currentIndex;
        openClientFullOrder.setOrderId(orderId);
        openClientFullOrder.setBuyerName("mock buyer");
        openClientFullOrder.setOriginFee(10000L);
        openClientFullOrder.setFee(8000L);
        openClientFullOrder.setDiscount(2000);
        openClientFullOrder.setOriginShipFee(0);
        openClientFullOrder.setShipFee(0);
        openClientFullOrder.setItems(mockItems(orderId, orderStatus));
        openClientFullOrder.setConsignee(mockOpenClientOrderConsignee());
        openClientFullOrder.setInvoice(new OpenClientOrderInvoice());
        openClientFullOrder.setCreatedAt(now);
        openClientFullOrder.setPaymentInfo(mockOpenClientPaymentInfo(now));
        return openClientFullOrder;
    }

    private OpenClientPaymentInfo mockOpenClientPaymentInfo(Date paidAt) {
        OpenClientPaymentInfo openClientPaymentInfo = new OpenClientPaymentInfo();
        openClientPaymentInfo.setPaidAt(paidAt);
        openClientPaymentInfo.setPaySerialNo("2019042922001173301039251354");
        return openClientPaymentInfo;
    }

    private OpenClientOrderConsignee mockOpenClientOrderConsignee() {
        OpenClientOrderConsignee openClientOrderConsignee = new OpenClientOrderConsignee();
        openClientOrderConsignee.setName("mock OpenClientOrderConsignee");
        openClientOrderConsignee.setMobile("13989489203");
        openClientOrderConsignee.setProvince("湖北省");
        openClientOrderConsignee.setCity("武汉市");
        openClientOrderConsignee.setRegion("洪山区");
        openClientOrderConsignee.setDetail("卓**街道**街道雄**道**路菜鸟驿站");
        return openClientOrderConsignee;
    }

    private List<OpenClientOrderItem> mockItems(String orderId, OpenClientOrderStatus orderStatus) {
        List<OpenClientOrderItem> list = new ArrayList<>();
        OpenClientOrderItem item = new OpenClientOrderItem();
        item.setOrderId(orderId);
        item.setStatus(orderStatus);
        item.setItemId("586338940952");
        item.setItemName("【双品节】PONY波尼春夏季新品男女款圆领运动字母透气短袖T恤92W2AT77");
        item.setItemCode("92W2AT77");
        item.setSkuId("3983644912768");
        item.setSkuCode("92M2AT23BK006");
        item.setPrice(23900);
        item.setQuantity(1);
        item.setDiscount(5876);
        list.add(item);
        return list;
    }
}
