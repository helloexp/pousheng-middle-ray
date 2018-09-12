package com.pousheng.middle.open.stock;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;


@Api(description = "库存推送API")
@RestController
@RequestMapping("/api/stock/push")

@Component
@Slf4j
public class StockPusherApi {

    @Autowired
    private StockPusherClient stockPusherClient;
    @Autowired
    private StockPusherLogic stockPushLogic;


    @ApiOperation("库存推送")
    @RequestMapping(value = "/submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> submit(@RequestBody List<String> skuCodes) {
        stockPusherClient.submit(skuCodes);
        return Response.ok(true);
    }

    @ApiOperation("是否启用推送记录缓存")
    @RequestMapping(value = "/cache/setting", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> submit(@RequestBody boolean flag) {
        stockPushLogic.setStockPusherCacheEnable(flag);
        return Response.ok(true);
    }

}
