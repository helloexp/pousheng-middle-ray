package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.order.dto.OrderShipmentCriteria;
import com.pousheng.middle.order.dto.ShipmentPagingInfo;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.order.impl.dao.OrderShipmentDao;
import io.terminus.parana.order.impl.dao.ShipmentDao;
import io.terminus.parana.order.impl.dao.ShopOrderDao;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;

/**
 * Created by songrenfei on 2017/6/20
 */
@Slf4j
@Service
@RpcProvider
public class OrderShipmentReadServiceImpl implements OrderShipmentReadService{

    @Autowired
    private OrderShipmentDao orderShipmentDao;
    @Autowired
    private ShipmentDao shipmentDao;
    @Autowired
    private ShopOrderDao shopOrderDao;


    @Override
    public Response<List<OrderShipment>> findByOrderIdAndOrderLevel(Long orderId, OrderLevel orderLevel) {
        try {
            List<OrderShipment> orderShipments = orderShipmentDao.findByOrderIdAndOrderType(orderId, orderLevel.getValue());
            if (CollectionUtils.isEmpty(orderShipments)) {
                return Response.ok(Collections.<OrderShipment>emptyList());
            }
            return Response.ok(orderShipments);
        }catch (Exception e) {
            log.error("failed to find order shipment(orderId={}, orderLevel={}), cause:{}",
                    orderId, orderLevel.toString(), Throwables.getStackTraceAsString(e));
            return Response.fail("order.shipment.find.fail");
        }
    }


    @Override
    public Response<List<OrderShipment>> findByAfterSaleOrderIdAndOrderLevel(Long afterSaleOrderId, OrderLevel orderLevel) {
        try {
            List<OrderShipment> orderShipments = orderShipmentDao.findByAfterSaleOrderIdAndOrderType(afterSaleOrderId, orderLevel.getValue());
            if (CollectionUtils.isEmpty(orderShipments)) {
                return Response.ok(Collections.<OrderShipment>emptyList());
            }
            return Response.ok(orderShipments);
        }catch (Exception e) {
            log.error("failed to find order shipment(afterSaleOrderId={}, orderLevel={}), cause:{}",
                    afterSaleOrderId, orderLevel.toString(), Throwables.getStackTraceAsString(e));
            return Response.fail("order.shipment.find.fail");
        }
    }

    @Override
    public Response<Paging<ShipmentPagingInfo>> findBy(OrderShipmentCriteria criteria) {
        try {
            Paging<OrderShipment> paging = orderShipmentDao.paging(criteria.toMap());
            return Response.ok(transToDto(paging));
        } catch (Exception e) {
            log.error("failed to paging shipment, criteria={}, cause:{}",criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("shipment.find.fail");
        }
    }

    @Override
    public Response<OrderShipment> findById(Long id) {
        try {
            OrderShipment orderShipment = orderShipmentDao.findById(id);
            if(Arguments.isNull(orderShipment)){
                log.error("order shipment(id:{}) not exist",id);
                return Response.fail("order.shipment.not.exist");
            }
            return Response.ok(orderShipment);
        } catch (Exception e) {
            log.error("failed to find order shipment, id={}, cause:{}",id, Throwables.getStackTraceAsString(e));
            return Response.fail("shipment.find.fail");
        }
    }



    @Override
    public Response<OrderShipment> findByShipmentId(Long shipmentId) {
        try {
            List<OrderShipment> orderShipments = orderShipmentDao.findByShipmentId(shipmentId);
            if(CollectionUtils.isEmpty(orderShipments)){
                log.error("not find order shipment by shipment id:{}",shipmentId);
                return Response.fail("order.shipment.not.exist");
            }
            return Response.ok(orderShipments.get(0));//一个发货单只会对应一条关联信息
        } catch (Exception e) {
            log.error("failed to find order shipment by shipment id={}, cause:{}",shipmentId, Throwables.getStackTraceAsString(e));
            return Response.fail("order.shipment.find.fail");
        }
    }

    @Override
    public Response<OrderShipment> findByOrderIdAndSkuCodeAndQuantity(Long id, String skuCode, Integer quantity) {
        return null;
    }

    private Paging<ShipmentPagingInfo> transToDto(Paging<OrderShipment> paging) {
        Paging<ShipmentPagingInfo> dtoPaging = new Paging<ShipmentPagingInfo>();
        List<ShipmentPagingInfo> shipmentDtos = Lists.newArrayListWithCapacity(paging.getData().size());
        dtoPaging.setTotal(paging.getTotal());
        for (OrderShipment orderShipment : paging.getData()){
            ShipmentPagingInfo dto = new ShipmentPagingInfo();
            dto.setOrderShipment(orderShipment);
            try {
                dto.setShipment(shipmentDao.findById(orderShipment.getShipmentId()));
                dto.setShopOrder(shopOrderDao.findById(orderShipment.getOrderId()));
            }catch (Exception e){
                log.error("find shipment by id:{} fail,cause:{}",orderShipment.getShipmentId(),Throwables.getStackTraceAsString(e));
                continue;
            }

            shipmentDtos.add(dto);
        }
        dtoPaging.setData(shipmentDtos);
        return dtoPaging;
    }

    @Override
    public Response<Paging<OrderShipment>> paging(OrderShipmentCriteria criteria){
        try {
            Paging<OrderShipment> paging = orderShipmentDao.paging(criteria.toMap());
            return Response.ok(paging);
        } catch (Exception e) {
            log.error("failed to paging shipment, criteria={}, cause:{}",criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("shipment.find.fail");
        }
    }

    @Override
    public Response<Integer> countByShopId(Long shopId) {
        try {
            Integer count = orderShipmentDao.countTodayByShopId(shopId);
            return Response.ok(count);
        } catch (Exception e) {
            log.error("failed to paging shipment, shopId={}, cause:{}",shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("shipment.find.fail");
        }
    }
}
