package com.pousheng.middle.open.qimen;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.open.ReceiverInfoCompleter;
import com.pousheng.middle.open.api.dto.YyEdiShipInfo;
import com.pousheng.middle.open.erp.ErpOpenApiClient;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.utils.XmlUtils;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.event.OpenClientOrderSyncEvent;
import io.terminus.open.client.common.OpenClientException;
import io.terminus.open.client.common.channel.OpenClientChannel;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

/**
 * 奇门api实现
 * Created by cp on 8/19/17.
 */
@RestController
@RequestMapping("/api/qm")
@Slf4j
public class QiMenApi {

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;

    @RpcConsumer
    private MiddleOrderWriteService middleOrderWriteService;

    private final ReceiverInfoCompleter receiverInfoCompleter;
    private final EventBus eventBus;
    private final ErpOpenApiClient erpOpenApiClient;
    private final PoushengCompensateBizWriteService poushengCompensateBizWriteService;
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    private static final String WMS_APP_KEY = "terminus-wms";

    private static final String WMS_APP_SECRET = "anywhere-wms";

    @Autowired
    public QiMenApi(ReceiverInfoCompleter receiverInfoCompleter,
                    EventBus eventBus,
                    ErpOpenApiClient erpOpenApiClient,
                    PoushengCompensateBizWriteService poushengCompensateBizWriteService) {
        this.receiverInfoCompleter = receiverInfoCompleter;
        this.eventBus = eventBus;
        this.erpOpenApiClient = erpOpenApiClient;
        this.poushengCompensateBizWriteService = poushengCompensateBizWriteService;
    }

    @PostMapping(value = "/wms")
    public String gatewayOfWms(HttpServletRequest request) {
        String body = retrievePayload(request);
        log.info("wms receive request,method={},body:{}", request.getParameter("method"), body);
        DeliveryOrderCreateRequest deliveryOrderCreateRequest = XmlUtils.toPojo(body, DeliveryOrderCreateRequest.class);

        try {
            checkSign(deliveryOrderCreateRequest);
        } catch (OpenClientException e) {
            return XmlUtils.toXml(QimenResponse.fail(e.getBody()));
        }

        final String outerOrderId = deliveryOrderCreateRequest.getDeliveryOrder().getDeliveryOrderCode();

        Response<Optional<ShopOrder>> findShopOrder = shopOrderReadService.findByOutIdAndOutFrom(outerOrderId, OpenClientChannel.TAOBAO.name());
        if (!findShopOrder.isSuccess()) {
            log.error("fail to find shop order by outId={},outFrom={} when sync receiver info,cause:{}",
                    outerOrderId, OpenClientChannel.TAOBAO, findShopOrder.getError());
            return XmlUtils.toXml(QimenResponse.fail("order.find.fail"));
        }
        Optional<ShopOrder> shopOrderOptional = findShopOrder.getResult();

        if (!shopOrderOptional.isPresent()) {
            log.error("shop order not found where outId={},outFrom=taobao when sync receiver info", outerOrderId);
            return XmlUtils.toXml(QimenResponse.fail("order.not.found"));
        }
        ShopOrder shopOrder = shopOrderOptional.get();

        ReceiverInfo receiverInfo = toParanaReceiverInfo(deliveryOrderCreateRequest.getDeliveryOrder().getReceiverInfo());
        Response<Boolean> updateR = middleOrderWriteService.updateReceiveInfo(shopOrder.getId(), receiverInfo);
        if (!updateR.isSuccess()) {
            log.error("fail to update order(shopOrderId={}) receiverInfo to {},cause:{}",
                    shopOrder.getId(), receiverInfo, updateR.getError());
            return XmlUtils.toXml(QimenResponse.fail(updateR.getError()));
        }

        String buyerName = deliveryOrderCreateRequest.getDeliveryOrder().getBuyerNick();
        String outBuyerId = receiverInfo.getMobile();
        if (StringUtils.hasText(buyerName)||StringUtils.hasText(outBuyerId)) {
            Response<Boolean> updateBuyerInfoR = middleOrderWriteService.updateBuyerInfoOfOrder(shopOrder.getId(), buyerName,outBuyerId);
            if (!updateBuyerInfoR.isSuccess()) {
                log.error("fail to update buyerName to {} and outOrderId to {} for shopOrder(id={}),cause:{}",
                        buyerName,outBuyerId, shopOrder.getId(), updateBuyerInfoR.getError());
                return XmlUtils.toXml(QimenResponse.fail(updateBuyerInfoR.getError()));
            }
        }
        //抛出一个事件用于天猫自动生成发货单
        //OpenClientOrderSyncEvent event = new OpenClientOrderSyncEvent(shopOrder.getId());
        //eventBus存在队列阻塞和数据丢失风险，改通过定时任务执行的方式
        //eventBus.post(event);
        this.createShipmentResultTask(shopOrder.getId());

        return XmlUtils.toXml(QimenResponse.ok());
    }

    /**
     * @Description TODO
     * @Date        2018/5/31
     * @param       shopOrderId
     * @return
     */
    private void createShipmentResultTask(Long shopOrderId){
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.TMALL_ORDER_CREATE_SHIP.toString());
        biz.setContext(mapper.toJson(shopOrderId));
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        poushengCompensateBizWriteService.create(biz);
    }


    @PostMapping(value = "/erp")
    public String gatewayOfErp(HttpServletRequest request) {
        log.info("erp receive request:{}", request);
        return XmlUtils.toXml(QimenResponse.ok());
    }

    /**
     * 手动触发同步收货人信息
     *
     * @param shopOrderId 中台店铺订单id
     */
    @GetMapping(value = "/sync-order-receiver")
    public String syncOrderReceiver(@RequestParam("orderId") Long shopOrderId) {
        Response<ShopOrder> findR = shopOrderReadService.findById(shopOrderId);
        if (!findR.isSuccess()) {
            log.error("fail to find shop order by id={},cause:{}",
                    shopOrderId, findR.getError());
            throw new JsonResponseException(findR.getError());
        }
        ShopOrder shopOrder = findR.getResult();

        if (!"taobao".equalsIgnoreCase(shopOrder.getOutFrom())) {
            log.warn("shop order(id={}) is not taobao order,so skip to sync order receiver",
                    shopOrderId);
            return "is.not.taobao.order";
        }
        try {
            erpOpenApiClient.doPost("order.receiver.sync",
                    ImmutableMap.of("shopId", shopOrder.getShopId(), "orderId", shopOrder.getOutId()));
        } catch (Exception e) {
            log.error("fail to send sync order receiver request to erp for order(outOrderId={},openShopId={}),cause:{}",
                    shopOrder.getOutId(), shopOrder.getShopId(), Throwables.getStackTraceAsString(e));
            return "fail";
        }

        return "ok";
    }

    private String retrievePayload(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()))) {
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } catch (IOException e) {
                //ignore
            }
        } catch (IOException e) {
            //ignore
        }
        return sb.toString();
    }

    private void checkSign(DeliveryOrderCreateRequest request) {
        DeliveryOrderCreateRequest.ExtendProps extendProps = request.getExtendProps();
        if (extendProps == null) {
            log.error("extend props is empty for request:{}", request);
            throw new OpenClientException(400, "extend.props.empty");
        }

        if (!StringUtils.hasText(extendProps.getWmsAppKey())) {
            log.error("extend props not provide appKey for request:{}", request);
            throw new OpenClientException(400, "app.key.miss");
        }

        if (!Objects.equal(WMS_APP_KEY, extendProps.getWmsAppKey())) {
            log.error("unknown app key:{}", extendProps.getWmsAppKey());
            throw new OpenClientException(400, "invalid.app.key");
        }

        if (!StringUtils.hasText(extendProps.getWmsSign())) {
            log.error("extend props not provide sign for request:{}", request);
            throw new OpenClientException(400, "sign.miss");
        }
        String expectedSign = generateSign(request);
        if (!Objects.equal(extendProps.getWmsSign(), expectedSign)) {
            log.error("sign({}) not match for request:{},expected sign is:{}", request, expectedSign);
            throw new OpenClientException(400, "sign.not.match");
        }
    }

    private String generateSign(DeliveryOrderCreateRequest request) {
        Map<String, Object> params = Maps.newTreeMap();
        params.put("appKey", WMS_APP_KEY);
        params.put("deliveryOrderCode", request.getDeliveryOrder().getDeliveryOrderCode());

        if (StringUtils.hasText(request.getDeliveryOrder().getBuyerNick())) {
            params.put("buyerNick", request.getDeliveryOrder().getBuyerNick());
        }

        DeliveryOrderCreateRequest.ReceiverInfo receiverInfo = request.getDeliveryOrder().getReceiverInfo();
        params.put("name", receiverInfo.getName());
        if (StringUtils.hasText(receiverInfo.getMobile())) {
            params.put("mobile", receiverInfo.getMobile());
        }
        params.put("province", receiverInfo.getProvince());
        params.put("city", receiverInfo.getCity());
        if (StringUtils.hasText(receiverInfo.getArea())) {
            params.put("area", receiverInfo.getArea());
        }
        params.put("detailAddress", receiverInfo.getDetailAddress());
        return WmsSignUtils.generateSign(WMS_APP_SECRET, params);
    }

    private ReceiverInfo toParanaReceiverInfo(DeliveryOrderCreateRequest.ReceiverInfo receiverInfo) {
        ReceiverInfo r = new ReceiverInfo();
        r.setReceiveUserName(receiverInfo.getName());
        r.setProvince(receiverInfo.getProvince());
        r.setCity(receiverInfo.getCity());
        r.setRegion(receiverInfo.getArea());
        r.setMobile(receiverInfo.getMobile());
        r.setPhone(receiverInfo.getTel());
        r.setPostcode(receiverInfo.getZipCode());
        r.setDetail(receiverInfo.getDetailAddress());
        receiverInfoCompleter.complete(r);
        return r;
    }


}
