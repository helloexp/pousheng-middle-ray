package com.pousheng.middle.order.impl.service;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.order.dto.MiddleOrderCriteria;
import com.pousheng.middle.order.dto.fsm.MiddleOrderType;
import com.pousheng.middle.order.impl.dao.ShopOrderExtDao;
import com.pousheng.middle.order.impl.dao.SkuOrderExtDao;
import com.pousheng.middle.order.model.SkuOrderLockStock;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.impl.dao.InvoiceDao;
import io.terminus.parana.order.impl.dao.OrderInvoiceDao;
import io.terminus.parana.order.impl.dao.OrderReceiverInfoDao;
import io.terminus.parana.order.impl.dao.ShopOrderDao;
import io.terminus.parana.order.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单读服务
 * Created by songrenfei on 2017/6/16
 */
@Slf4j
@Service
@RpcProvider
public class MiddleOrderReadServiceImpl implements MiddleOrderReadService {

    @Autowired
    private ShopOrderDao shopOrderDao;
    @Autowired
    private OrderInvoiceDao orderInvoiceDao;
    @Autowired
    private InvoiceDao invoiceDao;
    @Autowired
    private OrderReceiverInfoDao orderReceiverInfoDao;

    @Autowired
    private ShopOrderExtDao shopOrderExtDao;

    @Autowired
    private SkuOrderExtDao skuOrderExtDao;


    @Override
    public Response<Paging<ShopOrder>> pagingShopOrder(MiddleOrderCriteria criteria) {
        try {
            Paging<ShopOrder> paging = shopOrderDao.paging(criteria.getOffset(),criteria.getLimit(),criteria.toMap());
            return Response.ok(paging);
        } catch (Exception e) {
            log.error("failed to paging shop order, criteria={}, cause:{}",criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.order.find.fail");
        }
    }

    @Override
    public Response<List<Invoice>> findInvoiceInfo(Long orderId, OrderLevel orderLevel) {
        try {
            List<OrderInvoice> orderInvoices =
                    orderInvoiceDao.findByOrderIdAndOrderType(orderId, orderLevel.getValue());
            List<Invoice> invoices = invoiceDao.findByIds(Lists.transform(orderInvoices, new Function<OrderInvoice, Long>() {
                @Override
                public Long apply(OrderInvoice input) {
                    return input.getInvoiceId();
                }
            }));
            return Response.ok(invoices);
        } catch (Exception e) {
            log.error("failed to find order invoice, order id={}, order level:{} cause:{}",orderId, orderLevel.getValue(), Throwables.getStackTraceAsString(e));
            return Response.fail("order.invoice.find.fail");
        }
    }

    @Override
    public Response<List<OrderReceiverInfo>> findOrderReceiverInfo(Long orderId, OrderLevel orderLevel) {
        try {
            return Response.ok(orderReceiverInfoDao.findByOrderIdAndOrderLevel(orderId, orderLevel));
        }catch (Exception e){
            log.error("find order receiver info by order id:{} order level:{} fai,cause:{}",orderId,orderLevel.getValue(),Throwables.getStackTraceAsString(e));
            return Response.fail("order.receiver.info.find.fail");
        }
    }

    @Override
    public Response<List<ShopOrder>> findByOutIdsAndOutFrom(List<String> outIds, String outFrom) {
        try {
            return Response.ok(shopOrderExtDao.findByOutIdsAndOutFrom(outIds, outFrom));
        }catch (Exception e){
            log.error("failed to find shop orders by out_ids:{},out_from:{},cause:{}",outIds,outFrom,
                Throwables.getStackTraceAsString(e));
            return Response.fail("failed.to.find.shop.orders");
        }
    }

    @Override
    public Response<List<String>> findSkuCodesByOrderIds(List<Long> orderIds) {
        return Response.ok(skuOrderExtDao.findSkuCodesByOrderIds(orderIds));
    }

    /**
     * 查询云聚Jit时效订单sku_order 中占用的数量
     * @param shopIds
     * @param warehouseIds
     * @param skuCodes
     * @return
     */
    @Override
    public Response<List<SkuOrderLockStock>> findOccupyQuantityList(List<Long> shopIds,
                                                           List<Long> warehouseIds, List<String> skuCodes){
        try {
            return Response.ok(skuOrderExtDao.queryOccupyQuantityList(shopIds,warehouseIds,skuCodes,
                MiddleOrderType.JIT_REAL_TIME.getValue()));
        }catch (Exception e){
            log.error("failed to find shop orders by shopIds:{},warehouseIds:{},skuCodes:{}, cause:{}",
                    shopIds,warehouseIds,skuCodes,Throwables.getStackTraceAsString(e));
            return Response.fail("failed.to.find.occupy.quantity.list");
        }

    }
}
