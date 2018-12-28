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
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Throwables;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

/**
 * Description: 云聚库存更新
 * User:        liangyj
 * Date:        2018/6/26
 */
@Component
@Slf4j
public class YjStockPushClient {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final String YJ_ERROR_CODE_SUCCESS = "0";
    private static final String URL_PATH = "/updatemgsendstock";

    private String yjGateway;
    private String yjAccessKey;
    @Autowired
    OpenShopCacher openShopCacher;

    public void setParam(String yjGateway,
                         String yjAccessKey) {
        this.yjGateway = yjGateway;
        this.yjAccessKey = yjAccessKey;
    }

    public Response<Boolean> syncStocks(String traceId,YjStockRequest request, Long shopId){
        log.info("YUN-JIT-STOCK-PUSH-CLIENT-START, traceId:{} ,request:{} ",traceId, request.toString());
        OpenShop openshop = openShopCacher.findById(shopId);
        this.yjGateway = openshop.getGateway();
        this.yjAccessKey = openshop.getAccessToken();
        request.setSerialno(traceId);
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        //String paramJson = JsonMapper.nonEmptyMapper().toJson(request);
        String gateway =yjGateway+URL_PATH;
        log.info(gateway);
        String responseBody="";
        try {
            String paramJson = mapper.writeValueAsString(request);
            log.info("rpc yunju stock push serialNo:{},paramJson:{}",traceId,paramJson);
            responseBody= HttpRequest.post(gateway)
                    .header("verifycode",yjAccessKey)
                    .header("serialNo",traceId)
                    .header("sendTime",DateTime.now().toString(DateTimeFormat.forPattern(DATE_PATTERN)))
                    .contentType("application/json")
                    .trustAllHosts().trustAllCerts()
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
