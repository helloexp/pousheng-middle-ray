package com.pousheng.middle.yyedisyc.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.pousheng.middle.hksyc.utils.Numbers;
import com.pousheng.middle.yyedisyc.dto.trade.ParameterWMS;
import com.pousheng.middle.yyedisyc.dto.trade.YJErpShipmentInfo;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiShipmentInfo;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiShipmentInfoBody;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.shop.OpenShopCacher;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by songrenfei on 2017/7/19
 */
@Component
@Slf4j
public class SycYYEdiShipmentOrderApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Value("${gateway.yyedi.host}")
    private String hkGateway;

    @Value("${gateway.yyedi.accessKey}")
    private String accessKey;

    /**
     * 允许使用宝唯
     */
    @Value("${gateway.yyedi.bw-enable:false}")
    private boolean bwEnable;

    @Autowired
    OpenShopCacher openShopCacher;
    private String yjGateway;
    private String yjAccessKey;

    private static final String SID = "PS_ERP_WMS_bcorders";

    public String doSyncShipmentOrder(YYEdiShipmentInfo requestData){

        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        YYEdiShipmentInfoBody body = new YYEdiShipmentInfoBody();
        body.bizContent(requestData).sid(SID).tranReqDate(DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)));
        String paramJson = JsonMapper.nonEmptyMapper().toJson(body);
        log.info("sync shipment to yyedi erp paramJson:{},serialNo:{}", paramJson, serialNo);
        String gateway = hkGateway + "/common/pserp/wms/pushbcorders";
        String responseBody = HttpRequest.post(gateway)
                .contentType("application/json")
                .header("verifycode", accessKey)
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();
        log.info("sync shipment to yyedi erp result:{},serialNo:{}", responseBody, serialNo);
        return responseBody;
    }

    public HttpRequest syncYJErpShipmentOrder(List<YJErpShipmentInfo> requestData) {
        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        String paramJson = JsonMapper.nonEmptyMapper().toJson(requestData.get(0));
        log.info("sync shipment to yj erp paramJson:{}, serialNo:{}", paramJson, serialNo);
        String gateway = hkGateway + "/common-yjerp/yjerp/default/pushmgorderset";
        if (bwEnable) {
            gateway = hkGateway + "/common-yjerp/bw/yjerp/pushmgorderset";
        }
        HttpRequest httpRequest = HttpRequest.post(gateway)
                .header("verifycode", accessKey)
                .header("serialNo", serialNo)
                .header("sendTime", DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000);

        log.info("sync shipment to yj erp ,serialNo:{}", serialNo);
        return httpRequest;
    }

    /**
     * 2019.04.16 RAY: POUS934 電商銷售單接口增加billsource參數
     *
     * @param reqData
     * @param billSource 訂單來源
     * @return responseBody
     */
    public String doSyncShipmentOrder(YYEdiShipmentInfo reqData, ParameterWMS.BillSource billSource) {

        reqData.setBillsource(billSource.getCode());
        return doSyncShipmentOrder(reqData);
    }
}
