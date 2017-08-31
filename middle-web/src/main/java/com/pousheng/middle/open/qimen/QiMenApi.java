package com.pousheng.middle.open.qimen;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.utils.XmlUtils;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.OpenClientException;
import io.terminus.open.client.common.channel.OpenClientChannel;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

    private static final String WMS_APP_KEY = "terminus-wms";

    private static final String WMS_APP_SECRET = "anywhere-wms";

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
        return XmlUtils.toXml(QimenResponse.ok());
    }

    @PostMapping(value = "/erp")
    public String gatewayOfErp(HttpServletRequest request) {
        log.info("erp receive request:{}", request);
        return XmlUtils.toXml(QimenResponse.ok());
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
            log.error("sign({}) not match for request:{},expected sign is:{}",request, expectedSign);
            throw new OpenClientException(400, "sign.not.match");
        }
    }

    private String generateSign(DeliveryOrderCreateRequest request) {
        Map<String, Object> params = Maps.newTreeMap();
        params.put("appKey", WMS_APP_KEY);
        params.put("deliveryOrderCode", request.getDeliveryOrder().getDeliveryOrderCode());

        DeliveryOrderCreateRequest.ReceiverInfo receiverInfo = request.getDeliveryOrder().getReceiverInfo();
        params.put("name", receiverInfo.getName());
        params.put("mobile", receiverInfo.getMobile());
        params.put("province", receiverInfo.getProvince());
        params.put("city", receiverInfo.getCity());
        params.put("area", receiverInfo.getArea());
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
        //TODO 查询地址的id
        return r;
    }


}
