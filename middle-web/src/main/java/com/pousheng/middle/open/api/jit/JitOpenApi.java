package com.pousheng.middle.open.api.jit;

import com.google.common.eventbus.EventBus;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.events.trade.HandleJITBigOrderEvent;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.order.dto.OpenFullOrder;
import io.terminus.open.client.order.dto.OpenFullOrderAddress;
import io.terminus.open.client.order.dto.OpenFullOrderInfo;
import io.terminus.open.client.order.dto.OpenFullOrderItem;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.entity.OPResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

/**
 * 提供给JIT调用的接口
 *
 * @author tanlongjun
 */
@OpenBean
@Slf4j
public class JitOpenApi {

    @RpcConsumer
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;

    @Autowired
    private EventBus eventBus;

    /**
     * 保存推送的订单
     * @param orderInfo
     * @return
     */
    @OpenMethod(key="push.out.jit.order.api",paramNames = {"orderInfo"},httpMethods = RequestMethod.POST)
    public OPResponse<String> saveOrder(@NotNull(message = "order.info.is.null") String orderInfo){
        try {
            //参数验证
            OPResponse<String> validateResponse=validateParam(orderInfo);
            if(!validateResponse.isSuccess()){
                return validateResponse;
            }

            //save to db
            Response<Long> response= saveDataToTask(orderInfo);
            if(response.isSuccess()){
                // publish event
                // 主动消费则时效性更好
                HandleJITBigOrderEvent event=new HandleJITBigOrderEvent();
                event.setId(response.getResult());
                eventBus.post(event);
                log.info("post event completed");
                return OPResponse.ok("success");
            }else{
                return OPResponse.fail("failed.save.jit.big.order");
            }

        } catch (Exception e) {
            log.error("failed to save jit big order.param:{}",orderInfo,e);
            return OPResponse.fail("failed.save.jit.big.order",e.getMessage());
        }
    }

    /**
     * 保存数据到补偿任务表
     * @param data
     * @return
     */
    protected Response<Long> saveDataToTask(String data){
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.OUT_OPEN_ORDER.toString());
        biz.setContext(data);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        return poushengCompensateBizWriteService.create(biz);
    }

    /**
     * 业务参数验证
     * @param val
     * @return
     */
    protected OPResponse validateParam(String val){
        OpenFullOrderInfo fullOrderInfo=JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(val,OpenFullOrderInfo.class);
        if(fullOrderInfo == null){
            return OPResponse.fail("param orderInfo incorrect");
        }
        OpenFullOrder openFullOrder = fullOrderInfo.getOrder();
        if (Objects.isNull(openFullOrder)){
            return OPResponse.fail("openFullOrder.is.null");
        }
        List<OpenFullOrderItem> items = fullOrderInfo.getItem();
        if (Objects.isNull(items)||items.isEmpty()){
            return OPResponse.fail("openFullOrderItems.is.null");
        }
        OpenFullOrderAddress address = fullOrderInfo.getAddress();
        if (Objects.isNull(address)){
            return OPResponse.fail("openFullOrderAddress.is.null");
        }
        return OPResponse.ok();
    }
}
