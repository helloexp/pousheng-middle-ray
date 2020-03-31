package com.pousheng.middle.web.warehouses;

import com.esotericsoftware.minlog.Log;
import com.pousheng.middle.search.dto.StockSendCriteria;
import com.pousheng.middle.search.stock.StockSendRuleDTO;
import com.pousheng.middle.search.stock.StockSendSearchComponent;
import io.swagger.annotations.Api;
import io.terminus.common.model.Paging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-18 15:04<br/>
 */
@Api
@RestController
@RequestMapping("/api/stock-send/rules")
@Slf4j
public class WarehouseRuleSearcher {

    private final StockSendSearchComponent stockSendSearchComponent;

    public WarehouseRuleSearcher(StockSendSearchComponent stockSendSearchComponent) {
        this.stockSendSearchComponent = stockSendSearchComponent;
    }

    @GetMapping("/search")
    public Paging<StockSendRuleDTO> search(StockSendCriteria criteria) {
        // todo: pre-cond criteria
     //   log.info("param:{}", criteria);
        return  stockSendSearchComponent.search(criteria);
    }
}
