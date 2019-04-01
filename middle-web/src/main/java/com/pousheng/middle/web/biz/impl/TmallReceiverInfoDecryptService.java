package com.pousheng.middle.web.biz.impl;

import com.pousheng.middle.open.PsOrderReceiver;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.biz.dto.ReceiverInfoDecryptDTO;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.common.StringUtil;
import io.terminus.open.client.common.channel.OpenClientChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Tmall 收货人信息脱敏
 *
 * @author tanlongjun
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.TMALL_RECEIVER_INFO_DECRYPT)
@Service
@Slf4j
public class TmallReceiverInfoDecryptService implements CompensateBizService {

    @Autowired
    private PsOrderReceiver psOrderReceiver;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {

        if (null == poushengCompensateBiz) {
            log.warn("doProcess params is null");
            return;
        }

        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            throw new BizException("doProcess context is null");
        }
        ReceiverInfoDecryptDTO data = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(
            context, ReceiverInfoDecryptDTO.class);
        if (data == null) {
            log.error("receiver info decrypt param:{}", context);
            throw new BizException("could not handle the tmall receiver info.");
        }

        //如果是天猫订单，则发请求到端点erp，把收货地址信息同步过来
        if (OpenClientChannel.from(data.getOutFrom()) == OpenClientChannel.TAOBAO) {
            psOrderReceiver.syncReceiverInfo(data);
        }
        //如果是天猫分销订单，则发请求到端点erp，把收货地址同步过来
        if (OpenClientChannel.from(data.getOutFrom()) == OpenClientChannel.TFENXIAO) {
            psOrderReceiver.syncFenxiaoReceiverInfo(data);
        }
    }

}
