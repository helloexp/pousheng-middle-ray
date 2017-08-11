package com.pousheng.middle.web.order.job;

import com.pousheng.middle.order.dto.MiddleRefundCriteria;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.RefundItem;
import com.pousheng.middle.order.dto.RefundPaging;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.component.RefundWriteLogic;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.RefundCriteria;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Refund;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 售中退款定时任务触发
 * Created by tony on 2017/8/10.
 * pousheng-middle
 */
@Component
@Slf4j
public class RefundJob {
    @Autowired
    private HostLeader hostLeader;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @Autowired
    private RefundWriteLogic refundWriteLogic;

    /**
     * 每隔9分钟执行一次,拉取中台售中退款的退款单
     */
    @Scheduled(cron = "0 0/9 * * * ? ")
    public void doneRefund() {
        log.info("START SCHEDULE ON SALE REFUND");
        RefundCriteria criteria = new RefundCriteria();
        criteria.setStatus(Arrays.asList(MiddleRefundStatus.WAIT_HANDLE.getValue()));
        criteria.setType(MiddleRefundType.ON_SALES_REFUND.value());
        Response<Paging<RefundPaging>> response = refundReadLogic.refundPaging((MiddleRefundCriteria) criteria);
        if (!response.isSuccess()) {
            log.error("find  refund paging failed,caused by {}", response.getError());
            throw new ServiceException(response.getError());
        }
        List<RefundPaging> refunds = response.getResult().getData();
        for (RefundPaging refundPaging : refunds) {
            Refund refund = refundPaging.getRefund();
            OrderRefund orderRefund = refundPaging.getOrderRefund();
            RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
            try {
                if (Objects.equals(refundExtra.getOrderType(), "1")) {
                    //整单退款,调用整单退款的逻辑
                    orderWriteLogic.autoCancelShopOrder(orderRefund.getOrderId());
                } else if (Objects.equals(refundExtra.getOrderType(), "2")) {

                    List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
                    for (RefundItem refundItem : refundItems) {
                        //子单退款,调用子单退款的逻辑
                        orderWriteLogic.autoCancelSkuOrder(orderRefund.getOrderId(), refundItem.getSkuCode());
                    }
                }
            } catch (ServiceException e) {
                log.error("on sale refund failed,cause by {}",e.getMessage());
            } catch (Exception e) {
                log.error("on sale refund failed,cause by {}",e.getMessage());
            }

            //更新售后单状态为已退款
            refundWriteLogic.updateStatus(refund, MiddleOrderEvent.ON_SALE_RETURN.toOrderOperation());
        }
        log.info("END SCHEDULE ON SALE REFUND");
    }
}
