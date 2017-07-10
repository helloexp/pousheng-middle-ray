package com.pousheng.middle.open.api;

import com.google.common.base.Throwables;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ExpressCodeCriteria;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.service.ExpressCodeReadService;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.component.MiddleOrderFlowPicker;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.component.RefundWriteLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.RefundWriteService;
import io.terminus.parana.order.service.ShipmentWriteService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.spring.web.json.Json;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 订单open api
 * Created by songrenfei on 2017/6/15
 */
@OpenBean
@Slf4j
public class OrderOpenApi {

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @RpcConsumer
    private ExpressCodeReadService expressCodeReadService;
    @RpcConsumer
    private OrderWriteLogic orderWriteLogic;
    @RpcConsumer
    private OrderReadLogic orderReadLogic;
    @RpcConsumer
    private RefundWriteLogic refundWriteLogic;
    @RpcConsumer
    private RefundReadLogic refundReadLogic;
    @RpcConsumer
    private RefundWriteService refundWriteService;
    @RpcConsumer
    private OrderShipmentReadService orderShipmentReadService;


    private final static DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyyMMddHHmmss");


    /**
     * 恒康同步发货完成状态到中台
     *
     * @param shipmentId       中台发货单号
     * @param hkShipmentId     恒康发货单号
     * @param shipmentCorpCode 物流公司代码
     * @param shipmentSerialNo 物流单号
     * @param shipmentDate     发货时间
     * @return 是否同步成功
     */
    @OpenMethod(key = "hk.shipments.api", paramNames = {"shipmentId", "hkShipmentId", "shipmentCorpCode", "shipmentSerialNo",
            "shipmentDate"}, httpMethods = RequestMethod.POST)
    public void syncHkShipmentStatus(@NotNull(message = "shipment.id.is.null") Long shipmentId,
                                     @NotEmpty(message = "hk.shipment.id.is.null") String hkShipmentId,
                                     @NotEmpty(message = "shipment.corp.code.empty") String shipmentCorpCode,
                                     @NotEmpty(message = "shipment.serial.no.empty") String shipmentSerialNo,
                                     @NotEmpty(message = "shipment.date.empty") String shipmentDate) {
        log.info("HK-SYNC-SHIPMENT-STATUS-START param shipmentId is:{} hkShipmentId is:{} shipmentCorpCode is:{} " +
                "shipmentSerialNo is:{} shipmentDate is:{}", shipmentId, hkShipmentId, shipmentCorpCode, shipmentSerialNo, shipmentDate);

        try {

            DateTime dt = DateTime.parse(shipmentDate, DFT);
            Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);

            //判断状态及获取接下来的状态
            Flow flow = flowPicker.pickShipments();
            OrderOperation orderOperation = MiddleOrderEvent.SHIP.toOrderOperation();
            if (!flow.operationAllowed(shipment.getStatus(), orderOperation)) {
                log.error("shipment(id={})'s status({}) not fit for ship",
                        shipment.getId(), shipment.getStatus());
                throw new ServiceException("shipment.current.status.not.allow.ship");
            }
            Integer targetStatus = flow.target(shipment.getStatus(), orderOperation);
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);

            if (!Objects.equals(hkShipmentId, shipmentExtra.getOutShipmentId())) {
                log.error("hk shipment id:{} not equal middle shipment(id:{} ) out shipment id:{}", hkShipmentId, shipment.getId(), shipmentExtra.getOutShipmentId());
                throw new ServiceException("hk.shipment.id.not.matching");
            }


            //封装更新信息
            Shipment update = new Shipment();
            update.setId(shipment.getId());
            Map<String, String> extraMap = shipment.getExtra();
            shipmentExtra.setShipmentSerialNo(shipmentSerialNo);
            shipmentExtra.setShipmentCorpCode(shipmentCorpCode);
            //通过恒康代码查找快递名称
            shipmentExtra.setShipmentCorpName(makeExpressNameByhkCode(shipmentCorpCode));
            shipmentExtra.setShipmentDate(dt.toDate());
            extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, mapper.toJson(shipmentExtra));
            update.setExtra(extraMap);

            //更新状态
            Response<Boolean> updateStatusRes = shipmentWriteService.updateStatusByShipmentId(shipment.getId(), targetStatus);
            if (!updateStatusRes.isSuccess()) {
                log.error("update shipment(id:{}) status to :{} fail,error:{}", shipment.getId(), targetStatus, updateStatusRes.getError());
                throw new ServiceException(updateStatusRes.getError());
            }

            //更新基本信息
            Response<Boolean> updateRes = shipmentWriteService.update(update);
            if (!updateRes.isSuccess()) {
                log.error("update shipment(id:{}) extraMap to :{} fail,error:{}", shipment.getId(), extraMap, updateRes.getError());
                throw new ServiceException(updateStatusRes.getError());
            }
            //判断发货单是否发货完
            if (Objects.equals(shipment.getType(), ShipmentType.SALES_SHIP.value())) {
                //判断发货单是否已经全部发货完成,如果全部发货完成之后需要更新order的状态为待收货
                Response<OrderShipment> orderShipmentResponse = orderShipmentReadService.findByShipmentId(shipment.getId());
                OrderShipment orderShipment = orderShipmentResponse.getResult();
                long orderShopId = orderShipment.getOrderId();
                ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShopId);
                if (Objects.equals(shopOrder.getStatus(), MiddleOrderStatus.WAIT_SHIP.getValue())) {
                    Response<List<OrderShipment>> listResponse = orderShipmentReadService.findByOrderIdAndOrderLevel(orderShopId, OrderLevel.SHOP);
                    List<Integer> orderShipMentStatusList = Lists.transform(listResponse.getResult(), new Function<OrderShipment, Integer>() {
                        @Nullable
                        @Override
                        public Integer apply(@Nullable OrderShipment orderShipment) {
                            return orderShipment.getStatus();
                        }
                    });
                    if (!orderShipMentStatusList.contains(MiddleShipmentsStatus.WAIT_SHIP.getValue())) {
                        //待发货--商家已经发货
                        boolean updateRlt = orderWriteLogic.updateOrder(shopOrder, OrderLevel.SHOP, MiddleOrderEvent.SHIP);
                        if (!updateRlt) {
                            log.error("update shopOrder status error (id:{}),original status is {}", shopOrder.getId(), shopOrder.getStatus());
                            throw new JsonResponseException("update.shop.order.status.error");
                        }
                    }
                }
            }
            if (Objects.equals(shipment.getType(), ShipmentType.EXCHANGE_SHIP.value())) {
                //如果发货单已经全部发货完成,需要更新refund表的状态为待确认收货,rufund表的状态为待收货完成
                Response<OrderShipment> orderShipmentResponse = orderShipmentReadService.findByShipmentId(shipment.getId());
                OrderShipment orderShipment = orderShipmentResponse.getResult();
                long afterSaleOrderId = orderShipment.getAfterSaleOrderId();
                Refund refund = refundReadLogic.findRefundById(afterSaleOrderId);
                if (Objects.equals(refund.getStatus(), MiddleRefundStatus.WAIT_SHIP.getValue())) {
                    Response<List<OrderShipment>> listResponse = orderShipmentReadService.findByAfterSaleOrderIdAndOrderLevel(afterSaleOrderId, OrderLevel.SHOP);
                    List<Integer> orderShipMentStatusList = Lists.transform(listResponse.getResult(), new Function<OrderShipment, Integer>() {
                        @Nullable
                        @Override
                        public Integer apply(@Nullable OrderShipment orderShipment) {
                            return orderShipment.getStatus();
                        }
                    });
                    if (!orderShipMentStatusList.contains(MiddleShipmentsStatus.WAIT_SHIP.getValue())) {
                        //更新售后单的处理状态

                        Response<Boolean> resRlt = refundWriteLogic.updateStatus(refund, MiddleOrderEvent.SHIP.toOrderOperation());
                        if (!resRlt.isSuccess()) {
                            log.error("update refund status error (id:{}),original status is {}", refund.getId(), refund.getStatus());
                            throw new JsonResponseException("update.refund.status.error");
                        }
                        //将shipmentExtra的已发货时间塞入值
                        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
                        refundExtra.setShipAt(new Date());
                        Map<String, String> extrMap = refund.getExtra();
                        extrMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
                        refund.setExtra(extrMap);
                        Response<Boolean> updateRefundRes = refundWriteService.update(refund);
                        if (!updateRefundRes.isSuccess()) {
                            log.error("update refund(id:{}) fail,error:{}", refund, updateRes.getError());
                            throw new JsonResponseException("update.refund.error");
                        }
                    }
                }
            }
        } catch (JsonResponseException | ServiceException e) {
            log.error("hk sync shipment(id:{}) to pousheng fail,error:{}", shipmentId, e.getMessage());
            throw new OPServerException(e.getMessage());
        } catch (Exception e) {
            log.error("hk sync shipment(id:{}) fail,cause:{}", shipmentId, Throwables.getStackTraceAsString(e));
            throw new OPServerException("sync.fail");
        }

        log.info("HK-SYNC-SHIPMENT-STATUS-END");
    }


    @OpenMethod(key = "hk.refund.confirm.received.api", paramNames = {"refundOrderId", "hkRefundOrderId", "itemInfo", "receivedDate"}, httpMethods = RequestMethod.POST)
    public void syncHkRefundStatus(@NotNull(message = "refund.order.id.is.null") Long refundOrderId,
                                   @NotEmpty(message = "hk.refund.order.id.is.null") String hkRefundOrderId,
                                   @NotEmpty(message = "item.info.empty") String itemInfo,
                                   @NotEmpty(message = "received.date.empty") String receivedDate) {
        log.info("HK-SYNC-REFUND-STATUS-START param refundOrderId is:{} hkRefundOrderId is:{} itemInfo is:{} " +
                "shipmentDate is:{}", refundOrderId, hkRefundOrderId, itemInfo, receivedDate);

        try {
            Refund refund = refundReadLogic.findRefundById(refundOrderId);
            if (!Objects.equals(hkRefundOrderId, refund.getOutId())) {
                log.error("hk refund id:{} not equal middle refund(id:{} ) out id:{}", hkRefundOrderId, refund.getId(), refund.getOutId());
                throw new ServiceException("hk.refund.id.not.matching");
            }

            //todo 恒康返回的商品信息如何处理

            DateTime dt = DateTime.parse(receivedDate, DFT);

            RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
            refundExtra.setHkReturnDoneAt(dt.toDate());
            refundExtra.setHkConfirmItemInfo(itemInfo);

            //更新状态
            OrderOperation orderOperation = getSyncConfirmSuccessOperation(refund);
            Response<Boolean> updateStatusRes = refundWriteLogic.updateStatus(refund, orderOperation);
            if (!updateStatusRes.isSuccess()) {
                log.error("update refund(id:{}) status,operation:{} fail,error:{}", refund.getId(), orderOperation.getText(), updateStatusRes.getError());
                throw new ServiceException(updateStatusRes.getError());
            }

            //更新扩展信息
            Refund update = new Refund();
            update.setId(refundOrderId);
            Map<String, String> extraMap = refund.getExtra();
            extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
            update.setExtra(extraMap);

            Response<Boolean> updateExtraRes = refundWriteLogic.update(update);
            if (!updateExtraRes.isSuccess()) {
                log.error("update refund(id:{}) extra:{} fail,error:{}", refundOrderId, refundExtra, updateExtraRes.getError());
                //这就就不抛出错了，中台自己处理即可。
            }

        } catch (JsonResponseException | ServiceException e) {
            log.error("hk sync refund confirm to middle fail,error:{}", e.getMessage());
            throw new OPServerException(e.getMessage());
        } catch (Exception e) {
            log.error("hk sync refund confirm to middle fail,cause:{}", Throwables.getStackTraceAsString(e));
            throw new OPServerException("sync.fail");
        }
        log.info("HK-SYNC-REFUND-STATUS-END");

    }


    @OpenMethod(key = "hk.hello.world.api", paramNames = {"name"}, httpMethods = RequestMethod.GET)
    public String helloWord(@NotEmpty(message = "name.empty") String name) {
        log.info("HK-HELLER-WORLD-START param name is:{} ", name);

        log.info("HK-HELLER-WORLD-START-END");
        return "hell world:" + name;
    }

    public String makeExpressNameByhkCode(String hkExpressCode) {
        ExpressCodeCriteria criteria = new ExpressCodeCriteria();
        criteria.setHkCode(hkExpressCode);
        Response<Paging<ExpressCode>> response = expressCodeReadService.pagingExpressCode(criteria);
        if (!response.isSuccess()) {
            log.error("failed to pagination expressCode with criteria:{}, error code:{}", criteria, response.getError());
            throw new JsonResponseException(response.getError());
        }
        if (response.getResult().getData().size() == 0) {
            log.error("there is not any express info by hkCode:{}", hkExpressCode);
            throw new JsonResponseException("express.info.is.not.exist");
        }
        ExpressCode expressCode = response.getResult().getData().get(0);
        return expressCode.getName();
    }


    //获取同步成功事件
    private OrderOperation getSyncConfirmSuccessOperation(Refund refund) {
        MiddleRefundType middleRefundType = MiddleRefundType.from(refund.getRefundType());
        if (Arguments.isNull(middleRefundType)) {
            log.error("refund(id:{}) type:{} invalid", refund.getId(), refund.getRefundType());
            throw new JsonResponseException("refund.type.invalid");
        }

        switch (middleRefundType) {
            case AFTER_SALES_RETURN:
                return MiddleOrderEvent.RETURN.toOrderOperation();
            case AFTER_SALES_REFUND:
                log.error("refund(id:{}) type:{} not allow hk confirm", refund.getId(), refund.getRefundType());
                throw new JsonResponseException("refund.not.allow.hk.confirm");
            case AFTER_SALES_CHANGE:
                return MiddleOrderEvent.RETURN_CHANGE.toOrderOperation();
            default:
                log.error("refund(id:{}) type:{} invalid", refund.getId(), refund.getRefundType());
                throw new JsonResponseException("refund.type.invalid");
        }

    }

}
