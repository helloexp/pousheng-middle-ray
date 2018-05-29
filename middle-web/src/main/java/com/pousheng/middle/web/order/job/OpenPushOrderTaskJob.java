package com.pousheng.middle.web.order.job;

import com.pousheng.middle.order.model.OpenPushOrderTask;
import com.pousheng.middle.order.service.OpenPushOrderTaskReadService;
import com.pousheng.middle.order.service.OpenPushOrderTaskWriteService;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.order.service.OrderServiceCenter;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.order.dto.OpenFullOrderInfo;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/22
 * pousheng-middle
 */
@ConditionalOnProperty(name = "trade.job.enable", havingValue = "true", matchIfMissing = true)
@Component
@Slf4j
@RestController
public class OpenPushOrderTaskJob {
    @Autowired
    private HostLeader hostLeader;
    @Autowired
    private OpenPushOrderTaskReadService openPushOrderTaskReadService;
    @Autowired
    private OpenPushOrderTaskWriteService openPushOrderTaskWriteService;
    @Autowired
    private OrderServiceCenter orderServiceCenter;

    @Scheduled(cron = "0 */15 * * * ?")
    public void doProcessFailedOpenPushOrderTask() {
        if (!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }
        log.info("START SCHEDULE ON OPEN PUSH ORDER TASK");
        //获取待处理的失败任务
        Response<List<OpenPushOrderTask>> r =  openPushOrderTaskReadService.findByStatus(0);
        if (!r.isSuccess()){
            log.error("find open push order task failed");
            return;
        }
        List<OpenPushOrderTask> openPushOrderTasks = r.getResult();
        for (OpenPushOrderTask openPushOrderTask:openPushOrderTasks){
            try{
                Map<String,String> extra = openPushOrderTask.getExtra();
                String openClientShopJson = extra.get("openClientShop");
                String orderInfos = extra.get("orderInfos");
                List<OpenFullOrderInfo> openFullOrderInfos = JsonMapper.nonEmptyMapper().fromJson(orderInfos, JsonMapper.nonEmptyMapper().createCollectionType(List.class,OpenFullOrderInfo.class));
                OpenClientShop openClientShop = JsonMapper.nonEmptyMapper().fromJson(openClientShopJson,OpenClientShop.class);
                Response<Boolean> response =  orderServiceCenter.syncOrderToEcp(openClientShop.getOpenShopId(),openFullOrderInfos);
                if (!response.isSuccess()) {
                    log.error("sync order to out failed,openShopId is {},orders are {},caused by {}", openClientShop.getOpenShopId(), openFullOrderInfos, r.getError());
                }else {
                    openPushOrderTask.setStatus(1);
                    Response<Boolean> updateResponse = openPushOrderTaskWriteService.update(openPushOrderTask);
                    if (!updateResponse.isSuccess()){
                        log.error("update open push order task failed,openPushOrderTaskId is {},caused by {}",openPushOrderTask.getId(),updateResponse.getError());
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        log.info("END SCHEDULE ON OPEN PUSH ORDER TASK");
    }

    /**
     * 手动补偿任务
     */
    @RequestMapping(value = "api/hk/pos/task",method = RequestMethod.GET)
    public void test2(){
       this.doProcessFailedOpenPushOrderTask();
    }
}
