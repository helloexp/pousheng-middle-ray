package com.pousheng.middle.web.order.job;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.MiddleRefundCriteria;
import com.pousheng.middle.order.dto.RefundItem;
import com.pousheng.middle.order.dto.RefundPaging;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.component.RefundWriteLogic;
import com.pousheng.middle.web.order.sync.erp.SyncErpReturnLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.order.service.RefundReadService;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 售中退款定时任务触发
 * Created by tony on 2017/8/10.
 * pousheng-middle
 */
@ConditionalOnProperty(name = "trade.job.enable", havingValue = "true", matchIfMissing = true)
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
    private OrderReadLogic orderReadLogic;
    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @RpcConsumer
    private RefundReadService refundReadService;
    @RpcConsumer
    private OrderWriteService orderWriteService;
    @Autowired
    private SyncErpReturnLogic syncErpReturnLogic;


    /**
     * 每隔20分钟执行一次,拉取中台售中退款的退款单
     */
    @Scheduled(cron = "0 0/20 * * * ? ")
    public void doneRefund() {


        if(!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }

        log.info("START JOB RefundJob.doneRefund");
        //当前时间减去6小时
        Date createStartAt = DateTime.now().minusHours(6).toDate();
        //查询6小时内未处理的退款单
        Response<List<Refund>> response = refundReadService.findByTradeNoAndCreatedAt(TradeConstants.REFUND_WAIT_CANCEL,createStartAt);
        if (!response.isSuccess()) {
            log.error("find  refund paging failed,caused by {}", response.getError());
            return;
        }

        List<Refund> refunds = response.getResult();
        Response<Map<Long,OrderRefund>> orderRefundGroupByRefundIdMapRes = refundReadLogic.groupOrderRerundByRefundId(refunds);
        if(!orderRefundGroupByRefundIdMapRes.isSuccess()){
            log.error("query order refund fail by  refunds:{}",refunds);
            return;
        }

        Map<Long,OrderRefund> orderRefundMap = orderRefundGroupByRefundIdMapRes.getResult();

        for (Refund refund : refunds) {
            String orderType = refund.getExtra().get("orderType");
            OrderRefund orderRefund = orderRefundMap.get(refund.getId());
            if(Arguments.isNull(orderRefund)){
                log.error("not find order refund by refund id:{}");
                continue;
            }
            try {
                int count =0;
                if (Objects.equals(orderType, "1")) {
                    //整单退款,调用整单退款的逻辑
                    log.info("try to auto cancel shop order shopOrderId is {}",orderRefund.getOrderId());
                    ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderRefund.getOrderId());
                    try {
                        Map<String,String> shopOrderExtra = shopOrder.getExtra();
                        shopOrderExtra.put(TradeConstants.SHOP_ORDER_CANCEL_REASON,"电商取消同步");
                        orderWriteService.updateOrderExtra(shopOrder.getId(), OrderLevel.SHOP,shopOrderExtra);
                    }catch (Exception e){
                        log.error("add shop order cancel reason failed,shop order id is {}",shopOrder.getId());
                    }
                    orderWriteLogic.autoCancelShopOrder(orderRefund.getOrderId());
                } else if (Objects.equals(orderType, "2")) {
                    log.info("try to auto cancel sku order shopOrderId is {}",orderRefund.getOrderId());
                    List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
                    for (RefundItem refundItem : refundItems) {
                        //子单退款,调用子单退款的逻辑
                        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderRefund.getOrderId());
                        //判断该店铺订单是否存在取消失败的子单
                        if (Objects.equals(shopOrder.getStatus(), MiddleOrderStatus.CANCEL_FAILED.getValue())){
                            count++;
                            continue;
                        }
                        SkuOrder skuOrder = orderReadLogic.findSkuOrderByShopOrderIdAndSkuCode(orderRefund.getOrderId(),refundItem.getSkuCode());
                        try {
                            Map<String,String> skuOrderExtra = skuOrder.getExtra();
                            skuOrderExtra.put(TradeConstants.SKU_ORDER_CANCEL_REASON,"电商取消同步");
                            orderWriteService.updateOrderExtra(skuOrder.getId(),OrderLevel.SKU,skuOrderExtra);
                        }catch (Exception e){
                            log.error("add sku order cancel reason failed,sku order id is {}",skuOrder.getId());
                        }
                        orderWriteLogic.autoCancelSkuOrder(orderRefund.getOrderId(), refundItem.getSkuCode(), skuOrder.getId());
                    }
                }else{
                    throw new ServiceException("error.order.type");
                }
                if (count==0){ //表明该店铺订单中没有取消失败的发货单
                    Refund updateRefund = new Refund();
                    updateRefund.setId(refund.getId());
                    updateRefund.setTradeNo(TradeConstants.REFUND_CANCELED);//借用tradeNo字段来标记售中退款的逆向单是否已处理
                    Response<Boolean> updateRes = refundWriteLogic.update(updateRefund);
                    if(!updateRes.isSuccess()){
                        log.error("update refund:{} fail,error:{}",updateRefund,updateRes.getError());
                    }
                }
            } catch (Exception e) {
                log.error("on sale refund failed,cause by {}", Throwables.getStackTraceAsString(e));
            }

        }
        log.info("END JOB RefundJob.doneRefund");
    }

    /**
     * @Description 定时同步VIP OXO 待同步的售后单
     * @Date        2018/11/22
     * @param
     * @return
     */
    //@Scheduled(cron = "0 0/5 * * * ? ")
    //todo VIP OXO售后单暂不进行自动审核和同步EDI
    public void syncVipOxoRefundToYYedi(){
        log.debug("VIP-OXO-REFUND-SYNC-YYEDI");
        int pageNo = 1;
        while(true){
            //查询待同步HK VIPOXO的售后单
            MiddleRefundCriteria criteria = new MiddleRefundCriteria();
            criteria.setStatus(Lists.newArrayList(MiddleRefundStatus.WAIT_SYNC_HK.getValue()));
            criteria.setChannels(Lists.newArrayList(MiddleChannel.VIPOXO.getValue()));
            Response<Paging<RefundPaging>> response = refundReadLogic.refundPaging(criteria);
            if (!response.isSuccess()) {
                log.error("find refund by criteria:{} fail,error:{}", criteria, response.getError());
                throw new JsonResponseException(response.getError());
            }
            List<RefundPaging> refundPagings = response.getResult().getData();

            if (refundPagings.isEmpty()){
                log.info("all refunds done pageNo is {}",pageNo);
                break;
            }
            for (RefundPaging refundPaging:refundPagings) {
                log.debug("VIP-OXO-REFUND-SYNC-YYEDI, refundId:{}",refundPaging.getRefund().getId());
                //同步售后单
                Refund refund = refundPaging.getRefund();
                Refund syncRefund = refundReadLogic.findRefundById(refund.getId());
                Response<Boolean> syncRes = syncErpReturnLogic.syncReturn(syncRefund);
                if (!syncRes.isSuccess()) {
                    log.error("sync VIP OXO refund(id:{}) to hk fail,error:{}", refund.getId(),
                            syncRes.getError());
                }
            }
            pageNo++;
        }
    }
}
