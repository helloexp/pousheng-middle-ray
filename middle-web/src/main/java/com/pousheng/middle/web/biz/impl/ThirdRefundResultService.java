package com.pousheng.middle.web.biz.impl;

import com.pousheng.middle.hksyc.component.SycYunjuRefundOrderApi;
import com.pousheng.middle.hksyc.dto.ExchangeDetail;
import com.pousheng.middle.hksyc.dto.YJExchangeReturnRequest;
import com.pousheng.middle.hksyc.dto.YJRespone;
import com.pousheng.middle.open.api.dto.YYEdiRefundConfirmItem;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.RefundItem;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.events.trade.TaobaoConfirmRefundEvent;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.common.StringUtil;
import io.terminus.open.client.center.AfterSaleServiceRegistryCenter;
import io.terminus.open.client.common.Pagination;
import io.terminus.open.client.order.dto.OpenClientAfterSale;
import io.terminus.open.client.order.enums.OpenClientAfterSaleStatus;
import io.terminus.open.client.order.service.OpenClientAfterSaleService;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.service.RefundWriteService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Description: 天猫或苏宁售后确认收货事件任务
 * User:        liangyj
 * Date:        2018/5/31
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.THIRD_REFUND_RESULT)
@Service
@Slf4j
public class ThirdRefundResultService implements CompensateBizService {
    @Autowired
    private AfterSaleServiceRegistryCenter afterSaleServiceRegistryCenter;
    @RpcConsumer
    private RefundWriteService refundWriteService;
    @Autowired
    private RefundReadLogic refundReadLogic;

    private static final JsonMapper MAPPER = JsonMapper.nonEmptyMapper();

    @Autowired
    private SycYunjuRefundOrderApi refundOrderApi;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        if (null == poushengCompensateBiz) {
            log.warn("ThirdRefundResultService.doProcess params is null");
            return;
        }
        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("ThirdRefundResultService.doProcess context is null");
            throw new BizException("ThirdRefundResultService.doProcess context is null");
        }
        TaobaoConfirmRefundEvent event = JsonMapper.nonEmptyMapper().fromJson(context, TaobaoConfirmRefundEvent.class);
        if (event == null) {
            log.warn("ThirdRefundResultService.doProcess taobaoConfirmRefundEvent is null");
            throw new BizException("ThirdRefundResultService.doProcess taobaoConfirmRefundEvent is null");
        }
        try {
            updateRefundStatusForTaobao(event);
        }catch (Exception e){
            log.error("update third channel refund status,caused by {}", e);
            throw new BizException("update third channel refund status,caused by {}", e);
        }
    }

    /**
     * @Description 天猫或苏宁售后确认收货事件处理
     * @Date        2018/6/1
     * @param       event
     * @return
     */
    public void updateRefundStatusForTaobao(TaobaoConfirmRefundEvent event) {
        log.info("begin to update third channel refund status,refundId is {},openAfterSaleId is {},openOrderId is {},openShopId is {},channel is {} ", event.getRefundId(), event.getOpenAfterSaleId(), event.getOpenOrderId(), event.getOpenShopId(), event.getChannel());
        //天猫渠道
        if (Objects.equals(event.getChannel(), MiddleChannel.TAOBAO.getValue())) {
            OpenClientAfterSaleService afterSaleService = this.afterSaleServiceRegistryCenter.getAfterSaleService(MiddleChannel.TAOBAO.getValue());
            Response<OpenClientAfterSale> r = afterSaleService.findByAfterSaleId(Long.valueOf(event.getOpenShopId()), event.getOpenAfterSaleId());
            if (!r.isSuccess()) {
                log.error("find taobao afterSaleOrder failed,taobaoAfterSaleOrderId is {},refundId is{},caused by{}", event.getOpenAfterSaleId(), event.getRefundId(), event.getOpenShopId());
                return;
            }
            //如果淘宝售后单状态是success(已退款),中台售后单状态同样变成已退款
            OpenClientAfterSale afterSale = r.getResult();
            if (Objects.equals(afterSale.getStatus(), OpenClientAfterSaleStatus.SUCCESS)) {
                Response<Boolean> updateR = refundWriteService.updateStatus(event.getRefundId(), MiddleRefundStatus.REFUND.getValue());
                if (!updateR.isSuccess()) {
                    log.error("fail to update refund(id={}) status to {} when receive after sale:{},cause:{}",
                            event.getRefundId(), MiddleRefundStatus.REFUND.getValue(), updateR.getError());
                }
            }
        }
        //苏宁渠道
        if (Objects.equals(event.getChannel(), MiddleChannel.SUNING.getValue()) || Objects.equals(event.getChannel(), MiddleChannel.SUNINGSALE.getValue())) {
            OpenClientAfterSaleService afterSaleService = this.afterSaleServiceRegistryCenter.getAfterSaleService(MiddleChannel.SUNING.getValue());
            Response<Pagination<OpenClientAfterSale>> r = afterSaleService.findByOrderId(Long.valueOf(event.getOpenShopId()), event.getOpenOrderId());
            if (!r.isSuccess()) {
                log.error("find taobao afterSaleOrder failed,taobaoAfterSaleOrderId is {},refundId is{},caused by{}", event.getOpenAfterSaleId(), event.getRefundId(), event.getOpenShopId());
                return;
            }
            List<OpenClientAfterSale> openClientAfterSales = r.getResult().getData();
            if (openClientAfterSales == null || openClientAfterSales.isEmpty()) {
                return;
            }
            Refund refund = refundReadLogic.findRefundById(event.getRefundId());
            for (OpenClientAfterSale openClientAfterSale : openClientAfterSales) {
                if (refund.getOutId().contains(openClientAfterSale.getOpenOrderId()) && Objects.equals(openClientAfterSale.getStatus(), OpenClientAfterSaleStatus.SUCCESS)) {
                    Response<Boolean> updateR = refundWriteService.updateStatus(event.getRefundId(), MiddleRefundStatus.REFUND.getValue());
                    if (!updateR.isSuccess()) {
                        log.error("fail to update refund(id={}) status to {} when receive after sale:{},cause:{}",
                                event.getRefundId(), MiddleRefundStatus.REFUND.getValue(), updateR.getError());
                    }
                }
            }
        }

        //云聚渠道
        if (Objects.equals(event.getChannel(), MiddleChannel.YUNJUBBC.getValue())) {

            Refund refund = refundReadLogic.findRefundById(event.getRefundId());
            YJExchangeReturnRequest request = new YJExchangeReturnRequest();

            Map<String, String> extraMap = refund.getExtra();
            request.setExchange_id(refundReadLogic.getOutafterSaleIdTaobao(refund.getOutId())); //退货单号
            ArrayList<ExchangeDetail> exchangeDetails = Lists.newArrayList();

            List<RefundItem> refundItems = MAPPER.fromJson(extraMap.get(TradeConstants.REFUND_ITEM_INFO),
                MAPPER.createCollectionType(List.class, RefundItem.class));
            List<YYEdiRefundConfirmItem> confirmItems = MAPPER.fromJson(
                extraMap.get(TradeConstants.REFUND_YYEDI_RECEIVED_ITEM_INFO),
                MAPPER.createCollectionType(List.class, YYEdiRefundConfirmItem.class));
            confirmItems.forEach(confirmItem -> {
                ExchangeDetail detail = new ExchangeDetail();
                RefundItem refundItem = getRefundItem(confirmItem.getItemCode(), refundItems);
                detail.setBar_code(confirmItem.getItemCode());
                detail.setLine_number(refundItem.getSkuOrderId());
                detail.setOk_num(StringUtils.isEmpty(confirmItem.getQuantity()) ? 0
                    : Integer.valueOf(confirmItem.getQuantity()));   //退货入库的正品数量(无数量传0)
                detail.setError_num(0); //    退货入库的残品数量(无数量传0)
                exchangeDetails.add(detail);
            });
            request.setExchange_detail(exchangeDetails);
            YJRespone yjRespone = refundOrderApi.doSyncRefundOrder(request);
            if (!Objects.isNull(yjRespone) && 0 == yjRespone.getError()) {
                Response<Boolean> updateR = refundWriteService.updateStatus(event.getRefundId(),
                    MiddleRefundStatus.REFUND.getValue());
                if (!updateR.isSuccess()) {
                    log.error("fail to update refund(id={}) status to {} when receive after sale:{},cause:{}",
                        event.getRefundId(), MiddleRefundStatus.REFUND.getValue(), updateR.getError());
                }
            } else {
                log.error("refundOrderApi.doSyncRefundOrder return failed.response:{}", MAPPER.toJson(yjRespone));
            }
        }
    }


    private RefundItem getRefundItem(String itemCode,List<RefundItem> refundItems){

        for (RefundItem refundItem : refundItems) {
            if (itemCode.equals(refundItem.getSkuCode())){

                return refundItem;
            }
        }
        return null;
    }
}
