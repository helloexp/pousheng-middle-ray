package com.pousheng.middle.web.order.sync.erp;

import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.sync.hk.SyncRefundLogic;
import com.pousheng.middle.web.order.sync.yyedi.SyncYYEdiReturnLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.order.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 1.同步售后单到erp
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/15
 * pousheng-middle
 */
@Slf4j
@Component
public class SyncErpReturnLogic {
    @Autowired
    private SyncRefundLogic syncRefundLogic;
    @Autowired
    private SyncYYEdiReturnLogic syncYYEdiReturnLogic;

    @Autowired
    private OrderReadLogic orderReadLogic;
    @RpcConsumer
    private OpenShopReadService openShopReadService;

    /**
     * 对于退货退款和换货的售后单需要同步到yyedi，对于仅退款的售后单需要直接同步到恒康
     * @param refund
     * @return
     */
    public Response<Boolean> syncReturn(Refund refund){

        Response<OpenShop> openShopResponse = openShopReadService.findById(refund.getShopId());
        if (!openShopResponse.isSuccess()){
            log.error("find open shop by openShopId {} failed,caused by {}",refund.getShopId(),openShopResponse.getError());
        }
        OpenShop openShop = openShopResponse.getResult();
        Map<String, String> openShopExtra =  openShop.getExtra();
        String erpSyncType = openShopExtra.get(TradeConstants.ERP_SYNC_TYPE);
        //售后仅退款的售后单直接同步到恒康
        if (Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_REFUND)){
            return syncRefundLogic.syncRefundToHk(refund);
        }
        //售后换货，退货的同步走配置渠道
        switch (erpSyncType){
            case "hk":
                return syncRefundLogic.syncRefundToHk(refund);
            case "yyEdi":
                return syncYYEdiReturnLogic.syncRefundToYYEdi(refund);
            default:
                log.error("can not find sync erp type,openShopId is {}",refund.getShopId());
                return Response.fail("find.open.shop.extra.erp.sync.type.fail");
        }
    }


    /**
     * 同步erp退货单取消
     *
     * @param refund 退货单
     * @return 同步结果
     */
    public Response<Boolean> syncReturnCancel(Refund refund){

        Response<OpenShop> openShopResponse = openShopReadService.findById(refund.getShopId());
        if (!openShopResponse.isSuccess()){
            log.error("find open shop by openShopId {} failed,caused by {}",refund.getShopId(),openShopResponse.getError());
        }
        OpenShop openShop = openShopResponse.getResult();
        Map<String, String> openShopExtra =  openShop.getExtra();
        String erpSyncType = openShopExtra.get(TradeConstants.ERP_SYNC_TYPE);
        //售后换货，退货的同步走配置渠道
        switch (erpSyncType){
            case "hk":
                return syncRefundLogic.syncRefundCancelToHk(refund);
            default:
                return Response.ok(Boolean.TRUE);
        }
    }


}
