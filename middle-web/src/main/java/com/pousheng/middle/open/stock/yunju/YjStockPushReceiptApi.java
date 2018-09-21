package com.pousheng.middle.open.stock.yunju;

import com.google.common.base.Throwables;
import com.pousheng.middle.open.stock.yunju.dto.YjStockReceiptRequest;
import com.pousheng.middle.web.middleLog.dto.StockLogDto;
import com.pousheng.middle.web.middleLog.dto.StockLogTypeEnum;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.rocketmq.core.TerminusMQProducer;
import io.terminus.common.utils.JsonMapper;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Description: receipt service for yunju stock push
 * User:        liangyj
 * Date:        2018/7/7
 */
@OpenBean
@Slf4j
@RestController
@RequestMapping("/api/yunju/stock/push")
@Api(description = "云聚库存更新")
public class YjStockPushReceiptApi {
    @Autowired
    private TerminusMQProducer producer;
    @Value("${terminus.rocketmq.stockLogTopic}")
    private String stockLogTopic;

    @ApiOperation("云聚库存更新回执")
    @RequestMapping(value = "/receipt", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @OpenMethod(key = "yj.stock.push.receipt.api", paramNames = {"result"}, httpMethods = RequestMethod.POST)
    public void receipt(@RequestBody YjStockReceiptRequest request){
        if(log.isDebugEnabled()) {
            log.debug("YUN-JIT-STOCK-PUSH-RESULT-RECEIPT, parameter:{}", request.toString());
        }
        try{
            String logJson = JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(request);
            producer.send(stockLogTopic, new StockLogDto().logJson(logJson).type(StockLogTypeEnum.YUNJUTOMIDDLE.value()));
        }catch (JsonResponseException | ServiceException e) {
            log.error("failed to update yunju stock receipt,error:{}", e.getMessage());
            throw new OPServerException(200,e.getMessage());
        }catch (Exception e){
            log.error("failed to update yunju stock receipt,cause:{} ",Throwables.getStackTraceAsString(e));
            throw new OPServerException(200,e.getMessage());
        }

    }
}
