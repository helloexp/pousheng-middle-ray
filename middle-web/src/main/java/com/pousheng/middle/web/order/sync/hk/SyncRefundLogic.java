package com.pousheng.middle.web.order.sync.hk;

import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * 同步恒康逆向订单逻辑
 * Created by songrenfei on 2017/6/27
 */
@Slf4j
@Component
public class SyncRefundLogic {


    /**
     * 同步恒康退货单
     * @param refundId 退货单id
     * @return 同步结果 result 为恒康的退货单编号
     */
    public Response<String> syncRefundToHk(Long refundId){

        return Response.ok(new Random(10000).toString());

    }


    /**
     * 同步恒康退货单取消
     * @param refundId 退货单id
     * @return 同步结果
     */
    public Response<Boolean> syncRefundCancelToHk(Long refundId){

        return Response.ok(Boolean.TRUE);

    }
    
}
