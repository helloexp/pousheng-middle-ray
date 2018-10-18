package com.pousheng.middle.web.order.job;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import io.terminus.common.model.Response;
import io.terminus.open.client.jd.order.JdOrderService;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @Description: not paid order for jd to cancel status
 * @author: yjc
 * @date: 2018/10/12下午1:26
 */
@ConditionalOnProperty(name = "trade.job.enable", havingValue = "true", matchIfMissing = true)
@Component
@Slf4j
public class JdNotPaidOrderJob{


    @Autowired
    private HostLeader hostLeader;
    @Autowired
    private JdRedisHandler jdRedisHandler;
    @Autowired
    private ShopOrderReadService shopOrderReadService;
    @Autowired
    private JdOrderService jdOrderService;
    @Autowired
    private OrderWriteLogic orderWriteLogic;


    /**
     * 对京东渠道待发货的单据，
     * 判断上游有取消动作来进行逆向取消
     */
    @Scheduled(cron = "0 0/20 * * * ? ")
    public void doJdNotPaidOrder() {
        if(!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }
        log.info("START JOB JdNotPaidOrderJob.doJdNotPaidOrder");

        List<String> outerOrderIds = jdRedisHandler.getRedisValue();
        if(CollectionUtils.isEmpty(outerOrderIds)) {
            return;
        }
        log.info("jd not paid order out id list info {}", outerOrderIds);
        outerOrderIds.stream().forEach(outerOrderId->{
            Response<Optional<ShopOrder>> shopOrderResponse = shopOrderReadService.findByOutIdAndOutFrom(outerOrderId,MiddleChannel.JD.getValue());
            if (!shopOrderResponse.isSuccess()) {
                log.error("find order fail by outerOrderId {}, error {}", outerOrderId, shopOrderResponse.getError());
            }
            Optional<ShopOrder> shopOrderOptional = shopOrderResponse.getResult();
            if (!shopOrderOptional.isPresent()){
                log.error("find order fail by outerOrderId {}", outerOrderId);
                return;
            }
            ShopOrder shopOrder = shopOrderResponse.getResult().get();

            Response<Boolean> response = jdOrderService.
                    existOrder(shopOrder.getShopId(), shopOrder.getOutId(), "TRADE_CANCELED", "orderId");
            if(!response.isSuccess()) {
                log.error("find cancel order detail fail by shopId {}, outId {}, error {}",
                        shopOrder.getShopId(), shopOrder.getOutId(), response.getError());
                return;
            }
            // 逆向取消
            try {
                orderWriteLogic.autoCancelShopOrder(shopOrder.getId());
            } catch (Exception e) {
                log.error("auto cancel jd shop order {} fail, error {} ", shopOrder.getId(), Throwables.getStackTraceAsString(e));
                return;
            }
            log.info("END JOB JdNotPaidOrderJob.doJdNotPaidOrder");

            jdRedisHandler.consume();
            log.info("redis current content is {}", jdRedisHandler.getRedisValue());
        });
    }

}
