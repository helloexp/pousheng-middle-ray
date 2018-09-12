package com.pousheng.middle.open.stock.yunju;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.github.kevinsawicki.http.HttpRequest;
import com.pousheng.middle.open.stock.yunju.dto.YjStockRequest;
import com.pousheng.middle.open.stock.yunju.dto.YjStockResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Throwables;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

/**
 * Description: 云聚库存更新
 * User:        liangyj
 * Date:        2018/6/26
 */
@Api(description = "云聚库存推送API")
@RestController
@RequestMapping("/api/yj/stock")
@Component
@Slf4j
public class YjStockPushClient {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String YJ_ERROR_CODE_SUCCESS = "0";
    private static final String URL_PATH = "/common-yjerp/yjerp/default/updatemgsendstock";

    @Value("${gateway.hk.host}")
    private String hkGateway;

    @Value("${gateway.hk.accessKey}")
    private String accessKey;

    @ApiOperation("设置云聚库存更新接口ESB地址和参数")
    @RequestMapping(value = "/setting/param", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public void setParam(@RequestParam(required = true)String hkGateway,
                               @RequestParam(required = true)String accessKey) {
        this.hkGateway = hkGateway;
        this.accessKey = accessKey;
    }

    @ApiOperation("推送云聚库存")
    @RequestMapping(value = "/push", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> syncStocks(@RequestBody String traceId,@RequestBody YjStockRequest request){
        //String serialNo = "TO" + System.currentTimeMillis() + Numbers.randomZeroPaddingNumber(6, 100000);

        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        //String paramJson = JsonMapper.nonEmptyMapper().toJson(request);
        String gateway =hkGateway+URL_PATH;
        log.info(gateway);
        String responseBody="";
        try {
            String paramJson = mapper.writeValueAsString(request);
            log.info("rpc yunju stock push serialNo:{},paramJson:{}",traceId,paramJson);
            responseBody= HttpRequest.post(gateway)
                    .header("verifycode",accessKey)
                    .header("serialNo",traceId)
                    .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                    .contentType("application/json")
                    //.trustAllHosts().trustAllCerts()
                    .send(paramJson)
                    .connectTimeout(10000).readTimeout(10000)
                    .body();
            log.info("rpc yunju stock push serialNo:{},responseBody={}",traceId,responseBody);
            YjStockResponse response  = JsonMapper.nonEmptyMapper().fromJson(responseBody,YjStockResponse.class);

            if(response!=null && YJ_ERROR_CODE_SUCCESS.equals(response.getError()) ){
                return Response.ok();
            }else{
                log.error("rpc yunju stock push error,responseBody={}",response.toString());
                return Response.fail(response.toString());
            }
        }catch ( Exception e){
            log.error("rpc yunju stock push exception happens,exception={}", Throwables.getStackTrace(e));
            return Response.fail(e.getLocalizedMessage());
        }
    }
}
