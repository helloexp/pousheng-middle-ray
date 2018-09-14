package com.pousheng.middle.open.stock.yunju;

import com.google.common.base.Throwables;
import com.pousheng.middle.open.stock.yunju.dto.StockItem;
import com.pousheng.middle.open.stock.yunju.dto.StockPushLogStatus;
import com.pousheng.middle.open.stock.yunju.dto.YjStockReceiptRequest;
import com.pousheng.middle.open.stock.yunju.dto.YjStockReceiptResponse;
import com.pousheng.middle.warehouse.model.StockPushLog;
import com.pousheng.middle.warehouse.service.MiddleStockPushLogWriteService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;


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
    private static final String YJ_ERROR_CODE_SUCESS = "0";
    @Autowired
    private MiddleStockPushLogWriteService middleStockPushLogWriteService;

    @ApiOperation("云聚库存更新回执")
    @RequestMapping(value = "/receipt", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @OpenMethod(key = "yj.stock.push.receipt.api", paramNames = {"result"}, httpMethods = RequestMethod.POST)
    public void receipt(@RequestBody YjStockReceiptRequest request){
        log.info("YUN-JIT-STOCK-PUSH-RESULT-START, parameter:{}",request.toString());
        YjStockReceiptResponse resp = new YjStockReceiptResponse();
        List<StockPushLog> stockPushLogs = new ArrayList<>();
        try {
            String requestNo = request.getSerialNo();
            List<StockItem> items = request.getItems();
            items.forEach(item -> {
                StockPushLog pushLog = new StockPushLog();
                String lineNo = item.getLineNo();
                int status = YJ_ERROR_CODE_SUCESS.equals(item.getError()) ? StockPushLogStatus.DEAL_SUCESS.value() : StockPushLogStatus.DEAL_SUCESS.value();
                String cause = item.getErrorInfo();
                pushLog.setRequestNo(requestNo);
                pushLog.setLineNo(lineNo);
                pushLog.setStatus(status);
                pushLog.setCause(cause);
                stockPushLogs.add(pushLog);
            });
            middleStockPushLogWriteService.batchUpdateResultByRequestIdAndLineNo(stockPushLogs);
        }catch (JsonResponseException | ServiceException e) {
            log.error("failed to update yunju stock receipt,error:{}", e.getMessage());
            throw new OPServerException(200,e.getMessage());
        }catch (Exception e){
            log.error("failed to update yunju stock receipt,cause:{} ",Throwables.getStackTraceAsString(e));
            throw new OPServerException(200,e.getMessage());
        }
        log.info("YUN-JIT-STOCK-PUSH-RESULT-END");
    }
}
