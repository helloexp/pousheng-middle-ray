package com.pousheng.middle.consume.index.job;

import com.pousheng.inventory.api.service.WarehouseShopGroupReadService;
import com.pousheng.middle.consume.index.processor.core.IndexEvent;
import com.pousheng.middle.consume.index.processor.impl.sendRule.StockShopProcessor;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/8/1
 */
@Slf4j
@RestController
@RequestMapping("/job")
public class ScanWarehouseRuleJobController {

    @RpcConsumer(version = "1.0.0")
    private WarehouseShopGroupReadService warehouseShopGroupReadService;

    @Autowired
    private StockShopProcessor stockShopProcessor;

    @RequestMapping("/full")
    public void startFullPush(){
        log.info("开始全量推送默认发货仓规则");
        Response<List<Long>> allIdResponse = warehouseShopGroupReadService.findAllId();
        if (allIdResponse.isSuccess()) {
            List<Long> ids = allIdResponse.getResult();
            if (ids != null && ids.size() > 0) {
                HashSet<Long> idSet = Sets.newHashSet(ids);
                log.info("共发现{}条数据:{}", idSet.size(), idSet);
                stockShopProcessor.doProcess(idSet);
            }
        }
        log.info("全量推送默认发货仓规则结束");
    }

}
