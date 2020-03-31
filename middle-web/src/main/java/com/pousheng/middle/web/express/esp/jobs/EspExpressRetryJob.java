package com.pousheng.middle.web.express.esp.jobs;

import cn.hutool.core.date.DatePattern;
import com.google.common.collect.Lists;
import com.pousheng.middle.order.model.PoushengEspLog;
import com.pousheng.middle.order.service.PoushengEspLogService;
import com.pousheng.middle.web.express.ExpressConfigType;
import com.pousheng.middle.web.express.esp.ConfigCacheService;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Shipment;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @Desc 重试推送任务，重试3次
 * @Author GuoFeng
 * @Date 2019/9/17
 */

@Service
@Slf4j
public class EspExpressRetryJob {

    @Autowired
    private PoushengEspLogService poushengEspLogService;

    @Autowired
    private EspExpressSynJob espExpressSynJob;

    @Autowired
    private HostLeader hostLeader;

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    @Autowired
    private ConfigCacheService configCacheService;

    //在整点3分触发，避开任务触发高峰期
    @Scheduled(cron = "0 3 0/1 * * ?")
    public void retry() {
        //检查是否主节点
        if (!hostLeader.isLeader()) {
            log.info("EspExpressRetryJob: current leader is {}, skip", hostLeader.currentLeaderId());
            return;
        }

        //检查配置
        String config = configCacheService.getUnchecked(ExpressConfigType.push_express);
        if (StringUtils.hasText(config) && ("true".equals(config) || "false".equals(config))) {
            if (!Boolean.valueOf(config)) {
                log.info("未开启推送快递，要开启推送，请将content设置为true，生效时间为30分钟后，重试任务停止");
                return;
            }
        } else {
            log.info("推送快递没有设置或者设置值错误，重试任务默认不运行");
            return;
        }

        DateTime dateTime = new DateTime();
        //查询前面两个小时之内失败的记录
        DateTime startDateTime = dateTime.minusMinutes(125);
        String dateString = startDateTime.toString(DatePattern.NORM_DATETIME_PATTERN);
        log.info("开始重试失败的快递推送, 数据时间:{}", dateString);
        //查询要重试的记录
        Response<List<PoushengEspLog>> logToRetryResponse = poushengEspLogService.findLogToRetry(-1, 3, startDateTime.toDate(), null);
        if (logToRetryResponse.isSuccess()) {
            List<PoushengEspLog> logList = logToRetryResponse.getResult();
            log.info("查询到失败记录{}条", logList.size());
            if (logList != null && logList.size() > 0) {
                List<Shipment> data = Lists.newArrayList();
                for (PoushengEspLog poushengEspLog : logList) {
                    Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(poushengEspLog.getMiddleShipmentNo());
                    if (shipment != null) {
                        data.add(shipment);
                    }
                }
                //重试任务
                if (data.size() > 0) {
                    espExpressSynJob.sendExpress(data);
                }
            }
        }

        log.info("结束重试失败的快递推送, 数据时间:{}", dateString);
    }
}
