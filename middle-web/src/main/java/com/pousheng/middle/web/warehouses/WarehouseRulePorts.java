package com.pousheng.middle.web.warehouses;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.PoushengCompensateBizCriteria;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import com.pousheng.middle.search.dto.StockSendCriteria;
import com.pousheng.middle.warehouse.companent.WarehouseRulesClient;
import com.pousheng.middle.web.events.trade.ExportTradeBillEvent;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import com.pousheng.middle.web.warehouses.component.StockSendRuleExcelParser;
import com.pousheng.middle.web.warehouses.dto.StockSendImportRequest;
import com.pousheng.middle.web.warehouses.importRule.ImportWarehouseRuleTask;
import de.danielbechler.util.Collections;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.common.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * 默认发货仓规则导入导出接口
 *
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-06-19 13:54<br/>
 */
@Slf4j
@Api("默认发货仓规则导入导出接口")
@RestController
@RequestMapping("/api/stock-send/rules/ports")
public class WarehouseRulePorts {
    private final WarehouseRulesClient warehouseRulesClient;
    private final OpenShopCacher openShopCacher;
    private final CompensateBizLogic compensateBizLogic;
    private final OpenShopReadService openShopReadService;
    private final StockSendRuleExcelParser stockSendRuleExcelParser;

    @Autowired
    private ImportWarehouseRuleTask warehouseRuleTask;

    @Autowired
    private PoushengCompensateBizReadService poushengCompensateBizReadService;

    public WarehouseRulePorts(WarehouseRulesClient warehouseRulesClient,
                              OpenShopCacher openShopCacher,
                              CompensateBizLogic compensateBizLogic,
                              OpenShopReadService openShopReadService,
                              StockSendRuleExcelParser stockSendRuleExcelParser) {
        this.warehouseRulesClient = warehouseRulesClient;
        this.openShopCacher = openShopCacher;
        this.compensateBizLogic = compensateBizLogic;
        this.openShopReadService = openShopReadService;
        this.stockSendRuleExcelParser = stockSendRuleExcelParser;
    }


    /**
     * 查询导入文件的处理记录
     *
     * @param pageNo   第几页
     * @param pageSize 分页大小
     * @return 查询结果
     */
    @ApiOperation("查询售后单导入的处理记录")
    @GetMapping(value = "/import/result")
    @OperationLogType("查询售后单导入的处理记录")
    public Paging<PoushengCompensateBiz> importPaging(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                                      @RequestParam(required = false, value = "pageSize") Integer pageSize,
                                                      Integer bizId) {
        PoushengCompensateBizCriteria criteria = new PoushengCompensateBizCriteria();
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);
        if (bizId != null) {
            criteria.setBizId(bizId.toString());
        }
        criteria.setBizType(PoushengCompensateBizType.IMPORT_WAREHOUSE_RULE.name());
        Response<Paging<PoushengCompensateBiz>> response = poushengCompensateBizReadService.pagingForShow(criteria);
        if (!response.isSuccess()) {
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    /**
     * 线上默认发货仓导出
     *
     * @return
     */
    @GetMapping("/export/online")
    public boolean exportsOnline() {
        List<Long> shopIds = Lists.newArrayList();
        for (MiddleChannel channel : MiddleChannel.values()) {
//            if (MiddleChannel.VIPOXO.equals(channel)) {
//                continue;
//            }

            Response<List<OpenShop>> r = openShopReadService.findByChannel(channel.getValue());
            if (!r.isSuccess()) {
                log.error("failed to find open shop by channel: {}, cause{}", channel, r.getError());
                throw new JsonResponseException(r.getError());
            }
            r.getResult().stream().filter(shop -> {
                //过滤掉线下门店，无法通过类型判断，只能判断名称前缀是mpos，历史遗留问题，shit
                String shopName = shop.getShopName();
                if (StringUtils.hasText(shopName)) {
                    if (! shopName.startsWith("mpos")) {
                        return true;
                    }
                }
                return false;
            }).map(OpenShop::getId).forEach(shopIds::add);
        }
        StockSendCriteria criteria = new StockSendCriteria();
        criteria.setShopIds(shopIds);

        return sendExportToMQ(criteria);
    }

    /**
     * 线下默认发货仓导出
     *
     * @return
     */
    @GetMapping("/export/offline")
    public boolean exportsOffline(StockSendCriteria criteria) {
        if (Collections.isEmpty(criteria.getZoneIds())) {
            return false;
        }
        criteria.setShopType(1L);
        return sendExportToMQ(criteria);
    }

    /**
     * 默认发货仓导入
     *
     * @return
     */
    @RequestMapping(value = "/import", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Long> imports(@RequestBody StockSendImportRequest request) {
        if ((Collections.isEmpty(request.getShopIds())) && (request.getIsAll() == false || request.getIsAll() == null)) {
            return Response.fail("请选择门店范围");
        }
        if (StringUtils.isEmpty(request.getFilePath())) {
            return Response.fail("文件地址不能为空");
        }

        if (request.getDelete() == null) {
            return Response.fail("请选择操作方式");
        }

        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.IMPORT_WAREHOUSE_RULE.toString());
        biz.setContext(JSONObject.toJSONString(request));
        biz.setBizId(UUID.randomUUID().toString());
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        //此处的POSHENG_MIDDLE_EXPORT_COMPENSATE_BIZ_TOPIC会在导入导出的模块执行，但是没有设置导入的topic，暂用导出的topic
        long bizId = compensateBizLogic.createBizAndSendMq(biz, MqConstants.POSHENG_MIDDLE_EXPORT_COMPENSATE_BIZ_TOPIC);
        log.info("导入默认发货仓规则:任务提交成功, ID:{}", bizId);
        return Response.ok();
    }

    @RequestMapping(value = "/import/test", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean importsTest(@RequestBody StockSendImportRequest request) {
        log.info("param:{}", request);

        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.IMPORT_WAREHOUSE_RULE.toString());
        biz.setContext(JSONObject.toJSONString(request));
        biz.setBizId(UUID.randomUUID().toString());
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());

        warehouseRuleTask.processFile(biz);

        return true;
    }

    private Boolean sendExportToMQ(StockSendCriteria criteria) {
        // export online
        ExportTradeBillEvent event = new ExportTradeBillEvent();
        event.setType(TradeConstants.EXPORT_STOCK_SEND);
        event.setCriteria(criteria);
        event.setUserId(UserUtil.getUserId());
        //换成Biz任务形式
        String context = JsonMapper.nonEmptyMapper().toJson(event);
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.EXPORT_TRADE_BILL.name());
        biz.setContext(context);
        biz.setBizId(TradeConstants.EXPORT_STOCK_SEND);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.name());
        compensateBizLogic.createBizAndSendMq(biz, MqConstants.POSHENG_MIDDLE_EXPORT_COMPENSATE_BIZ_TOPIC);
        return Boolean.TRUE;
    }
}
