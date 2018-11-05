package com.pousheng.middle.web.job;

import com.google.common.collect.Lists;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.web.order.sync.vip.SyncVIPLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.order.dto.OrderCriteria;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/9/28
 */
@Slf4j
@ConditionalOnProperty(value = "is.stock.task.consume", havingValue = "true", matchIfMissing = false)
@RestController
public class SyncVIPInfoJobs {

    @Autowired
    private HostLeader hostLeader;

    @Autowired
    private SyncVIPLogic syncVIPLogic;

    @RpcConsumer
    private OpenShopReadService openShopReadService;

    @Autowired
    private ShopOrderReadService shopOrderReadService;

    @RpcConsumer
    private ShopReadService shopReadService;

    private final static int BATCH_SIZE = 10;

    /**
     * 每1小时触发一次 同步唯品会物流
     */
    @Scheduled(cron = "0 0 0/1 * * ?")
    public void synchronizeTrack() {
        if (!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }
        log.info("START JOB VipOrderLogisticsTrackJobs.synchronizeTrack");
        Response<List<OpenShop>> response = openShopReadService.findByChannel(MiddleChannel.VIP.getValue());
        if (!response.isSuccess()) {
            throw new JsonResponseException(response.getError());
        }
        for (OpenShop openShop : response.getResult()) {
            int pageNo = 1;
            boolean next = batchHandle(pageNo, BATCH_SIZE, openShop.getId());
            while (next) {
                pageNo++;
                next = batchHandle(pageNo, BATCH_SIZE, openShop.getId());
            }
        }
        log.info("END JOB VipOrderLogisticsTrackJobs.synchronizeTrack");

    }

    @RequestMapping("api/my/test")
    @Scheduled(cron = "0 0/5 * * * ?")
    public void syncWarehouseMapping() {
        if (!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }
        log.info("START JOB VipOrderLogisticsTrackJobs.syncWarehouseMapping");

        Response<List<OpenShop>> resp = openShopReadService.findByChannel(MiddleChannel.VIP.getValue());
        if (!resp.isSuccess()) {
            throw new JsonResponseException(resp.getError());
        }
        if (CollectionUtils.isEmpty(resp.getResult())) {
            return;
        }
        Long shopId = resp.getResult().get(0).getId();
        syncVIPLogic.syncWarehouseMapping(shopId);
        log.info("END JOB VipOrderLogisticsTrackJobs.syncWarehouseMapping");

    }


    private boolean batchHandle(int pageNo, int size, Long shopId) {
        List<Integer> status = Lists.newArrayList(MiddleOrderStatus.SHIPPED.getValue());
        OrderCriteria criteria = new OrderCriteria();
        criteria.setStatus(status);
        criteria.setShopId(shopId);
        Response<Paging<ShopOrder>> pagingRes = shopOrderReadService.findBy(pageNo, size, criteria);
        if (!pagingRes.isSuccess()) {
            log.error("paging shop order fail,criteria:{},error:{}", criteria, pagingRes.getError());
            return Boolean.FALSE;
        }
        Paging<ShopOrder> paging = pagingRes.getResult();
        List<ShopOrder> shopOrders = paging.getData();

        if (paging.getTotal().equals(0L) || CollectionUtils.isEmpty(shopOrders)) {
            return Boolean.FALSE;
        }
        syncVIPLogic.getOrderLogisticsTrack(shopId, shopOrders);
        int current = shopOrders.size();
        // 判断是否存在下一个要处理的批次
        return current == size;
    }

}
