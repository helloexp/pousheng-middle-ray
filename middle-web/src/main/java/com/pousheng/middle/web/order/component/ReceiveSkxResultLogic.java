package com.pousheng.middle.web.order.component;

import com.pousheng.middle.open.api.dto.CancelOutOrderInfo;
import com.pousheng.middle.open.api.dto.SkxShipInfo;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Description: skx同步发货信息到中台
 * @author: yjc
 * @date: 2018/8/1下午5:53
 */
@Slf4j
@Component
public class ReceiveSkxResultLogic {

    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();


    /**
     * skx同步发货信息
     * @return
     */
    public Response<Long> createShipmentResultTask(SkxShipInfo skxShipInfo) {
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.SKX_SYNC_SHIPMENT_RESULT.toString());
        biz.setContext(mapper.toJson(skxShipInfo));
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        return poushengCompensateBizWriteService.create(biz);
    }

}
