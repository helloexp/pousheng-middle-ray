package com.pousheng.middle.web.order.component;

import com.alibaba.fastjson.JSONObject;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.collect.Lists;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.EditSubmitRefundItem;
import com.pousheng.middle.order.dto.PoushengSettlementPosCriteria;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.SubmitRefundInfo;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.model.PoushengSettlementPos;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.order.service.PoushengSettlementPosReadService;
import com.pousheng.middle.web.order.sync.erp.SyncErpReturnLogic;
import com.pousheng.middle.web.utils.mail.MailLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 售中退款单，由于拦截发货单失败，中台自动创建拒收单业务(目前只做了oxo的，后面可以把这个类抽象出来，针对不同的渠道做不同的处理)
 *
 * @author zhurg
 * @date 2019/6/18 - 下午2:58
 */
@Slf4j
@Component
public class AutoCreateRejectOrderHandler {

    @Autowired
    private RefundReadLogic refundReadLogic;

    @Autowired
    private OrderReadLogic orderReadLogic;

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    @Autowired
    private MailLogic mailLogic;

    @Autowired
    private RefundWriteLogic refundWriteLogic;

    @Autowired
    private MiddleOrderWriteService middleOrderWriteService;

    @Autowired
    private SyncErpReturnLogic syncErpReturnLogic;

    @RpcConsumer
    private PoushengSettlementPosReadService poushengSettlementPosReadService;

    @Value("${oxo.auto.create.reject.retry:1}")
    private int oxoAutoCreateRejectRetry;

    @Value("${pousheng.order.email.oxo.group:zhiqiang.zhang@pousheng.com}")
    private String[] middleBizEmailGroup;

    /**
     * 邮件发送开关
     */
    @Value("${pousheng.msg.send}")
    private Boolean sendLock;

    @Value("${ps.middle.system.gateway}")
    private String psMiddleSystemGateway;

    @Value("${ps.middle.system.accesskey}")
    private String psMiddleSystemAccesskey;

    @Autowired
    private OrderWriteLogic orderWriteLogic;

    /**
     * 校验需要自动创建拒收单的渠道
     *
     * @param shopOrder
     * @return
     */
    public boolean checkChannel(ShopOrder shopOrder) {
        return Objects.equals(MiddleChannel.VIPOXO.getValue(), shopOrder.getOutFrom());
    }

    /**
     * 校验该单子是否已经生成过拒收单了
     *
     * @param refunds
     * @return
     */
    public boolean checkRejectOrderExists(List<Refund> refunds) {
        if (CollectionUtils.isEmpty(refunds)) {
            return false;
        }
        Optional<Refund> optional = refunds.stream()
                .filter(refund -> Objects.equals(MiddleRefundType.REJECT_GOODS.value(), refund.getRefundType()))
                .filter(refund -> !refund.getStatus().equals(MiddleRefundStatus.CANCELED.getValue()))
                .findFirst();
        return optional.isPresent();
    }

    /**
     * 校验该订单是否已经生成了退货退款单
     *
     * @param refunds
     * @return
     */
    public Refund checkAfterSaleReturnOrderExists(List<Refund> refunds) {
        if (CollectionUtils.isEmpty(refunds)) {
            return null;
        }
        Optional<Refund> optional = refunds.stream()
                .filter(refund -> Objects.equals(MiddleRefundType.AFTER_SALES_RETURN.value(), refund.getRefundType()))
                .filter(refund -> !refund.getStatus().equals(MiddleRefundStatus.CANCELED.getValue()))
                .findFirst();
        return optional.get();
    }

    /**
     * 判断订单是否能都创建拒收单
     *
     * @param shopOrder
     * @return
     */
    public boolean orderCanCreateReject(ShopOrder shopOrder) {
        Map<String, String> map = shopOrder.getExtra();
        if (map.containsKey(TradeConstants.CUSTOMER_SERVICE_NOTE) && map.get(TradeConstants.CUSTOMER_SERVICE_NOTE).equals("取消订单")) {
            return true;
        }
        if (shopOrder.getBuyerNote().equals("取消订单") || shopOrder.getBuyerNote().equals("订单取消")) {
            return true;
        }
        return false;
    }

    /**
     * 校验拒收单能不能创建
     * http://middle-prepub.pousheng.com/api/pousheng/middle/terminus/check-refunds?shipment_code=SHP8748068&refund_type=6&_=1560994849496
     *
     * @param shipmentCode
     * @param refundCode
     * @return
     */
    public boolean checkRejectExists(String shipmentCode, String refundCode) {
        try {
            StringBuffer url = new StringBuffer(psMiddleSystemGateway);
            url.append("/middle-system-api/v1/terminus/check-refunds?refund_type=6&");
            if (!StringUtils.isEmpty(shipmentCode)) {
                url.append("shipment_code=").append(shipmentCode);
            }
            if (!StringUtils.isEmpty(refundCode)) {
                if (url.toString().endsWith("&")) {
                    url.append("refund_code=").append(refundCode);
                } else {
                    url.append("&").append("refund_code=").append(refundCode);
                }
            }
            String resp = HttpRequest.get(url.toString()).header("access-key", psMiddleSystemAccesskey).body();
            if (!StringUtils.isEmpty(resp)) {
                log.info("check-refunds for refundCode {} result {}", refundCode, resp);
                JSONObject jsonObject = JSONObject.parseObject(resp);
                return jsonObject.getBooleanValue("allow_submit");
            }
        } catch (Exception e) {
            log.error("shipmentCode {} check refunds order pos failed,cause:", shipmentCode, e);
        }
        return false;
    }

    /**
     * 校验订单是否已经生成pos
     *
     * @param shopOrder
     * @return
     */
    public boolean checkOrderPos(ShopOrder shopOrder) {
        PoushengSettlementPosCriteria criteria = new PoushengSettlementPosCriteria();
        criteria.setOrderId(shopOrder.getOrderCode());
        Response<Paging<PoushengSettlementPos>> response = poushengSettlementPosReadService.paging(criteria);
        if (!response.isSuccess()) {
            log.error("query order {} pos failed, cause:{}", criteria.getOrderId(), response.getError());
            return false;
        }
        if (null == response || null == response.getResult() || response.getResult().isEmpty()) {
            log.info("OXO订单{}销售POS未生成", shopOrder.getOrderCode());
            return false;
        }
        return true;
    }

    /**
     * 自动生成拒收单
     *
     * @param shipment
     */
    public void autoCreateRejectOrder(Shipment shipment) {
        int times = 0;
        OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderShipment.getOrderId());

        //校验渠道
        if (!checkChannel(shopOrder)) {
            return;
        }

        //校验该订单是否该生成拒收单
        if (!orderCanCreateReject(shopOrder)) {
            return;
        }

        List<Refund> refunds = refundReadLogic.findRefundsByOrderId(shopOrder.getId());

        //校验该单子是否已经生成过拒收单了
        if (checkRejectOrderExists(refunds)) {
            log.info("OXO订单号:{}已生成过拒收单", shopOrder.getOrderCode());
            return;
        }

        Refund afterSaleReturn = checkAfterSaleReturnOrderExists(refunds);
        if (Objects.nonNull(afterSaleReturn)) {
            log.info("OXO订单号:{}已生成退货退款单，无法创建拒收单", shopOrder.getOrderCode());
            if (sendLock) {
                sendWarnEmails(shopOrder, "订单取消，包含售后单，创建拒收单失败", "OXO订单（" + shopOrder.getOrderCode() + "）已取消, 创建拒收单失败，已创建退货退款（" + afterSaleReturn.getRefundCode() + "）");
            }
            return;
        }

        Long refundId = null;
        while (times <= oxoAutoCreateRejectRetry) {
            try {
                refundId = createRejectOrder(shopOrder, shipment);
                if (sendLock) {
                    sendWarnEmails(shopOrder, "订单取消，创建拒收单成功", "OXO订单:" + shopOrder.getOrderCode() + "已取消，拒收单:ASS" + refundId.toString() + "已创建成功，请人工下发");
                }
                break;
            } catch (Exception e) {
                if (times == 0) {
                    log.error("oxo order {} auto create reject order failed,cause:", shopOrder.getOrderCode(), e);
                } else {
                    log.error("oxo order {} auto create reject order retry {} times failed,cause:", shopOrder.getOrderCode(), times, e);
                }
                times++;
            }
        }
        if (null != refundId) {
            if (sendLock) {
                //发邮件
                sendWarnEmails(shopOrder, "订单取消，创建拒收单失败", "OXO订单（" + shopOrder.getOrderCode() + "）已取消，创建拒收单失败");
            }
        }
    }

    public Long createRejectOrder(ShopOrder shopOrder, Shipment shipment) {
        log.info("oxo auto create reject order start");
        //创建拒收单
        SubmitRefundInfo submitRefundInfo = new SubmitRefundInfo();
        submitRefundInfo.setOrderCode(shopOrder.getOrderCode());
        submitRefundInfo.setShipmentCode(shipment.getShipmentCode());
        submitRefundInfo.setRefundType(MiddleRefundType.REJECT_GOODS.value());
        //submitRefundInfo.setBuyerNote();
//        if (checkOrderPos(shopOrder)) {
//            submitRefundInfo.setOperationType(2);
//        } else {
//            submitRefundInfo.setOperationType(1);
//        }
        submitRefundInfo.setOperationType(1);
        submitRefundInfo.setReleOrderNo(shopOrder.getOrderCode());
        submitRefundInfo.setReleOrderType(1);

        submitRefundInfo.setShipmentCorpCode(shipment.getShipmentCorpCode());
        submitRefundInfo.setShipmentCorpName(shipment.getShipmentCorpName());
        submitRefundInfo.setShipmentSerialNo(shipment.getShipmentSerialNo());

        if (!CollectionUtils.isEmpty(shipment.getExtra()) && shipment.getExtra().containsKey(TradeConstants.SHIPMENT_EXTRA_INFO)) {
            String shipmentExtrajson = shipment.getExtra().get(TradeConstants.SHIPMENT_EXTRA_INFO);
            ShipmentExtra shipmentExtra = JsonMapper.nonEmptyMapper().fromJson(shipmentExtrajson, ShipmentExtra.class);
            submitRefundInfo.setShipmentCorpName(shipmentExtra.getShipmentCorpName());
        }


        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        List<EditSubmitRefundItem> editSubmitRefundItems = Lists.newArrayListWithCapacity(shipmentItems.size());
        long fee = 0L;
        for (ShipmentItem shipmentItem : shipmentItems) {
            EditSubmitRefundItem editSubmitRefundItem = new EditSubmitRefundItem();
            editSubmitRefundItem.setSkuOrderId(shipmentItem.getSkuOrderId());
            editSubmitRefundItem.setRefundSkuCode(shipmentItem.getSkuCode());
            editSubmitRefundItem.setRefundOutSkuCode(shipmentItem.getOutSkuCode());
            editSubmitRefundItem.setRefundQuantity(shipmentItem.getShipQuantity());
            //各个发货单行的实付金额
            editSubmitRefundItem.setFee(shipmentItem.getCleanFee().longValue());
            editSubmitRefundItems.add(editSubmitRefundItem);

            //计算各发货单行的实付金额
            fee += shipmentItem.getCleanFee();
        }
        submitRefundInfo.setFee(fee);
        submitRefundInfo.setEditSubmitRefundItems(editSubmitRefundItems);

        return refundWriteLogic.createRefund(submitRefundInfo);
    }

    public void cancelOxoOrder(Long shopOrderId) {
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        if (checkChannel(shopOrder) && orderCanCreateReject(shopOrder)) {
            try {
                //自动触发订单取消
                if (!orderWriteLogic.cancelOxoShopOrder(shopOrderId)) {
                    //判断下发货单状态是不是已经发货了，如果是已经发货了，则创建拒收单
                    List<Shipment> shipments = shipmentReadLogic.findByShopOrderId(shopOrderId);
                    List<Shipment> shipped = shipments.stream().filter(shipment -> Objects.equals(shipment.getStatus(), MiddleShipmentsStatus.SHIPPED.getValue()))
                            .collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(shipped)) {
                        shipped.forEach(shipment -> autoCreateRejectOrder(shipment));
                    }
                }
            } catch (Exception ex) {
                log.warn("try to cancel OXO order{} failed,cause:", shopOrder.getOrderCode(), ex);
            }
        }
    }

    /**
     * 发送异常邮件
     *
     * @param shopOrder
     * @param message
     */
    private void sendWarnEmails(ShopOrder shopOrder, String title, String message) {
        List<String> list = Lists.newArrayList();
        list.addAll(Arrays.asList(middleBizEmailGroup));
        log.info("send warn email for oxo auto create reject, order {}", shopOrder.getOrderCode());
        mailLogic.sendMail(String.join(",", list), title, message);
    }
}