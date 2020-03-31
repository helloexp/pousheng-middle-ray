package com.pousheng.middle.yyedisyc.component;

import com.github.kevinsawicki.http.HttpRequest;
import com.pousheng.middle.hksyc.utils.Numbers;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiCancelBody;
import com.pousheng.middle.yyedisyc.dto.trade.YYEdiCancelInfo;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by songrenfei on 2017/7/19
 */
@Component
@Slf4j
public class SycYYEdiRefundCancelApi {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Value("${gateway.yyedi.host}")
    private String hkGateway;

    @Value("${gateway.yyedi.accessKey}")
    private String accessKey;

    private static final String SID = "PS_ERP_WMS_bccancelrefunds";

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
        log.info("yyedi-cancel-refund paramJson:{}, serialNo{}",paramJson,serialNo);
        String gateway = hkGateway+"/common/pserp/wms/pushbccancelrefunds";
        String responseBody = HttpRequest.post(gateway)
                .contentType("application/json")
                .header("verifycode", accessKey)
                .send(paramJson)
                .connectTimeout(10000).readTimeout(10000)
                .body();
        log.info("sync cancel refund to yyedi result:{}, serialNo:{}",responseBody,serialNo);
        return responseBody;
    }
    
	/**
	 * 2019.04.16 RAY: POUS934 電商取消退貨單接口，增加companycode參數，標註定單來源
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
