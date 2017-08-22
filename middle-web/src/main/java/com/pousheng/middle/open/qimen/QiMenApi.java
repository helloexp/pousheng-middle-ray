package com.pousheng.middle.open.qimen;

import com.pousheng.middle.utils.XmlUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 奇门api实现
 * Created by cp on 8/19/17.
 */
@RestController
@RequestMapping("/api/qm")
@Slf4j
public class QiMenApi {

    @PostMapping(value = "/wms")
    public String gatewayOfWms(HttpServletRequest request) {
        String body = retrievePayload(request);
        log.info("wms receive request,method={},body:{}", request.getParameter("method"), body);
        //TODO 校验签名
        DeliveryOrderCreateRequest deliveryOrderCreateRequest = XmlUtils.toPojo(body, DeliveryOrderCreateRequest.class);
        //TODO 保存收货地址信息
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


}
