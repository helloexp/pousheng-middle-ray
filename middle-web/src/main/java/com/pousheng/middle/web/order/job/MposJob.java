package com.pousheng.middle.web.order.job;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.pousheng.middle.open.mpos.MposOrderHandleLogic;
import com.pousheng.middle.shop.constant.ShopConstants;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.order.service.OrderServiceCenter;
import io.terminus.open.client.common.Pagination;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.order.dto.OpenClientFullOrder;
import io.terminus.open.client.order.enums.OpenClientOrderStatus;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by penghui on 2018/1/10
 * 定时拉取Mpos订单跟踪发货单状态
 */
@Slf4j
@Component
public class MposJob {

    @Autowired
    private HostLeader hostLeader;

    @Autowired
    private OrderServiceCenter orderServiceCenter;

    private final ExecutorService executorService;

    @Autowired
    private OpenShopReadService openShopReadService;

    @Autowired
    private MposOrderHandleLogic orderHandleLogic;

    @Value("${open.client.sync.all.order.duration.in.minute:1440}")
    private Integer syncAllOrderDurationInMinute;

    @Value("${open.client.sync.order.fetch.size:20}")
    private Integer orderFetchSize;


    public MposJob(@Value("${order.queue.size: 200000}") int queueSizeOfOrder){
        this.executorService = new ThreadPoolExecutor(2, 4, 60L, TimeUnit.MINUTES,
                new ArrayBlockingQueue<Runnable>(queueSizeOfOrder),
                new ThreadFactoryBuilder().setNameFormat("order-fetcher-%d").build(),
                (r, executor) -> log.error("task {} is rejected", r));
    }

    @Scheduled(cron = "0 */5 * * * ?")
    public void syncMposOrder() {
        if (!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }
        DateTime now = DateTime.now();
        DateTime startAt = now.minusMinutes(syncAllOrderDurationInMinute);
        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("start to sync mpos order...");
        Response<List<OpenClientShop>> findR =  openShopReadService.search(ShopConstants.CHANNEL,"");
        if (!findR.isSuccess()) {
            log.error("fail to find mpos shops when sync order,cause:{}", findR.getError());
            return;
        }
        List<OpenClientShop> openClientShops = findR.getResult();
        if(CollectionUtils.isEmpty(openClientShops)) {
            return ;
        }
        for (OpenClientShop openClientShop: openClientShops) {
            OpenShop openShop = openShopReadService.findById(openClientShop.getOpenShopId()).getResult();
            if (!openShop.enable()) continue ;
            int pageNo = 1;
            while (true) {
                Response<Pagination<OpenClientFullOrder>> findResp = orderServiceCenter.searchOrder(openShop.getId(), OpenClientOrderStatus.PAID,
                        startAt.toDate(), now.toDate(), pageNo, orderFetchSize);
                if (!findResp.isSuccess()) {
                    log.error("fail to find order for open shop(id={}) with status={},pageNo={},pageSize={},cause:{}",
                            openShop.getId(), null, pageNo, orderFetchSize, findResp.getError());
                    break;
                }
                Pagination<OpenClientFullOrder> pagination = findResp.getResult();
                final List<OpenClientFullOrder> fullOrders = pagination.getData();
                if (CollectionUtils.isEmpty(fullOrders)) {
                    break;
                }
                //异步处理
                executorService.submit(new OrderHandleTask(fullOrders));
                if (!pagination.isHasNext()) {
                    break;
                }
                pageNo++;
            }
        }
        stopwatch.stop();
        log.info("end to sync mpos order,and cost {} seconds", stopwatch.elapsed(TimeUnit.SECONDS));
    }

    private class OrderHandleTask implements Runnable{

        private final List<OpenClientFullOrder> openClientFullOrders;

        public OrderHandleTask(List<OpenClientFullOrder> openClientFullOrders){
            this.openClientFullOrders = openClientFullOrders;
        }

        @Override
        public void run() {
            orderHandleLogic.specialHandleOrder(openClientFullOrders);
        }
    }
}
