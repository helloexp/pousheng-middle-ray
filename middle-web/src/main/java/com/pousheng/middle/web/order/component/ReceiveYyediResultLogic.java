/**
 * Copyright (C), 2012-2018, XXX有限公司
 * FileName: ReceiveYyediResultLogic
 * Author:   xiehong
 * Date:     2018/5/29 下午7:41
 * Description: yyedi会传发货单及售后单信息
 * History:
 * <author>          <time>          <version>          <desc>
 * 作者姓名           修改时间           版本号              描述
 */
package com.pousheng.middle.web.order.component;

import com.pousheng.middle.open.api.dto.YyEdiShipInfo;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 〈yyedi会传发货单及售后单信息〉
 *
 * @author xiehong
 * @create 2018/5/29 下午7:41
 */
@Slf4j
@Component
public class ReceiveYyediResultLogic {


    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    /**
     *yyedi回传发货信息任务创建
     *
     * @param okShipInfos
     * @return:Response
     * @Author:xiehong
     * @Date: 2018/5/29 下午8:23
     */
    public Response<Long> createShipmentResultTask(List<YyEdiShipInfo> okShipInfos){
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.YYEDI_SYNC_SHIPMENT_RESULT.toString());
        biz.setContext(mapper.toJson(okShipInfos));
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        return poushengCompensateBizWriteService.create(biz);
    }


    
    /**
     * 〈yyEDi回传售后单信息任务创建〉
     *
     * @param refunds 退货单信息
     * @return: Response
     * Author:xiehong
     * Date: 2018/5/31 上午11:31
     */
    public Response<Long> createRefundStatusTask(List<Refund> refunds){
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.YYEDI_SYNC_REFUND_RESULT.toString());
        biz.setContext(mapper.toJson(refunds));
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        return poushengCompensateBizWriteService.create(biz);

    }





}