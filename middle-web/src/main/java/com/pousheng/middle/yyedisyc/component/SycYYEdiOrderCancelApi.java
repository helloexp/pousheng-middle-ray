package com.pousheng.middle.yyedisyc.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.pousheng.middle.hksyc.utils.Numbers;
import com.pousheng.middle.yyedisyc.dto.trade.*;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by songrenfei on 2017/7/19
 */
@Component
@Slf4j
public class SycYYEdiOrderCancelApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Value("${gateway.yyedi.host}")
    private String hkGateway;
    @Value("${gateway.yyedi.accessKey}")
    private String accessKey;


    private static final String SID = "PS_ERP_WMS_bccancelorders";

    /**
     *
     * @param reqeustData
     * @return
     */
    public String doCancelOrder(YYEdiCancelInfo reqeustData){

        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        YYEdiCancelBody body = new YYEdiCancelBody();
        body.bizContent(reqeustData).sid(SID).tranReqDate(DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)));
        String paramJson = JsonMapper.nonEmptyMapper().toJson(body);
        log.info("start do cancel yyedi order paramJson:{}, serialNo:{}",paramJson,serialNo);
        String gateway = hkGateway+"/common/pserp/wms/pushbccancelorders";
        String responseBody = HttpRequest.post(gateway)
                .contentType("application/json")
                .header("verifycode", accessKey)
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();
        log.info("end do cancel yyedi order paramJson:{},result:{}, serialNo:{}",paramJson,responseBody,serialNo);
        return responseBody;
    }

    public String doYJErpCancelOrder(List<YJErpCancelInfo> requestData){
        String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);
        String paramJson = JsonMapper.nonEmptyMapper().toJson(requestData.get(0));
        log.info("sync cancel shipment to yj erp paramJson :{}, serialNo:{}",paramJson,serialNo);
        String gateway = hkGateway + "/common-yjerp/yjerp/default/pushmgordercancel";
        String responseBody = HttpRequest.post(gateway)
                .header("verifycode",accessKey)
                .header("serialNo",serialNo)
                .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                .contentType("application/json")
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();

        log.info("sync cancel shipment to yj erp result:{}, serialNo:{}",responseBody,serialNo);
        return responseBody;
    }
    
	/**
	 * 2019.04.16 RAY : 電商取消銷售單接口，增加companycode參數，標註定單來源
	 * 
	 * @param reqData
	 * @param companyCode 定單來源
	 * @return responseBody
	 */
	public String doCancelOrder(YYEdiCancelInfo reqData, String companyCode) {
		reqData.setCompanycode(companyCode);
		return doCancelOrder(reqData);
	}

}
