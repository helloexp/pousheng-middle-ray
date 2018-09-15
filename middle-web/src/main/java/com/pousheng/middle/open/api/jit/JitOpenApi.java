package com.pousheng.middle.open.api.jit;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.constants.SymbolConsts;
import com.pousheng.middle.open.manager.JitOrderManager;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.biz.Exception.JitUnlockStockTimeoutException;
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
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    @Autowired
    private JitOrderManager jitOrderManager;

    @RpcConsumer
    private MiddleOrderReadService middleOrderReadService;

    /**
     * 保存时效的订单
     *
     * @param orderInfo
     * @return
     */
    @OpenMethod(key = "push.out.rt.order.api", paramNames = {"orderInfo"}, httpMethods = RequestMethod.POST)
    public void saveRealTimeOrder(@NotNull(message = "order.info.is.null") String orderInfo) {
        log.info("receive yj rt order info {}", orderInfo);

        OpenFullOrderInfo fullOrderInfo = validateBaiscParam(orderInfo);
        //参数验证
        validateBaiscParam(fullOrderInfo);
        OPResponse<String> response =null;
        try {
            response = jitOrderManager.handleRealTimeOrder(fullOrderInfo);

        } catch (JitUnlockStockTimeoutException juste) {
            log.warn("lock stock timeout. try to save unlock task biz to recover stock.", orderInfo,
                Throwables.getStackTraceAsString(juste));
            jitOrderManager.saveUnlockInventoryTask(juste.getData());
        } catch (Exception e) {
            log.error("failed to save jit realtime order.param:{},cause:{}", orderInfo,
                Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, "failed.save.jit.realtime.order");
        }
        if (response == null
            || !response.isSuccess()) {
            log.error("failed to save jit realtime order.param:{},cause:{}", orderInfo,
                JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(response));
            throw new OPServerException(200, response.getError());
        }
        log.info("receive yj rt order:{} success",orderInfo);
    }

    /**
     * 保存推送的订单
     *
     * @param orderInfo
     * @return
     */
    @OpenMethod(key = "push.out.jit.order.api", paramNames = {"orderInfo"}, httpMethods = RequestMethod.POST)
    public void saveOrder(@NotNull(message = "order.info.is.null") String orderInfo) {
        log.info("receive yj jit order info {}", orderInfo);
        try {
            OpenFullOrderInfo fullOrderInfo = validateBaiscParam(orderInfo);
            //参数验证
             validateBaiscParam(fullOrderInfo);
            //验证时效订单是否存在
            Response<List<Long>> realOrderValidateResp = validateRealOrderIdsExist(fullOrderInfo);
            if (!realOrderValidateResp.isSuccess()) {
                log.error("valid jit order info:{} fail,error:{}",orderInfo,realOrderValidateResp.getError());
                throw new OPServerException(200,realOrderValidateResp.getError());
            }
            //save to db
            Response<Long> response = saveDataToTask(orderInfo);
            if (response.isSuccess()) {
                // publish event
                // 主动消费则时效性更好
                HandleJITBigOrderEvent event = new HandleJITBigOrderEvent();
                event.setId(response.getResult());
                eventBus.post(event);
                log.info("receive yj jit order success");
            } else {
                throw new OPServerException("failed.save.jit.big.order");
            }

        } catch (Exception e) {
            log.error("failed to save jit big order.param:{},cause:{}", orderInfo, Throwables.getStackTraceAsString(e));
            throw new OPServerException(200,"failed.save.jit.big.order");
        }
    }

    /**
     * 保存数据到补偿任务表
     *
     * @param data
     * @return
     */
    protected Response<Long> saveDataToTask(String data) {
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.OUT_OPEN_ORDER.toString());
        biz.setContext(data);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        return poushengCompensateBizWriteService.create(biz);
    }

    protected OpenFullOrderInfo validateBaiscParam(String orderInfo) {
        if (StringUtils.isBlank(orderInfo)) {
            log.error("yj rt order info invalid is blank");
            throw new OPServerException(200,"orderInfo is required");
        }

        try {
            return JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(orderInfo, OpenFullOrderInfo.class);
        } catch (Exception e){
            log.error("trans order info json：{} to object fail,cause:{}",orderInfo, Throwables.getStackTraceAsString(e));
            throw new OPServerException("order.info.invalid");
        }
    }

    /**
     * 业务参数验证
     *
     * @param fullOrderInfo
     * @return
     */
    protected void validateBaiscParam(OpenFullOrderInfo fullOrderInfo) {
        if (fullOrderInfo == null) {
            throw new OPServerException(200,"param orderInfo incorrect");
        }
        OpenFullOrder openFullOrder = fullOrderInfo.getOrder();
        if (Objects.isNull(openFullOrder)) {
            throw new OPServerException(200,"openFullOrder.is.null");
        }
        List<OpenFullOrderItem> items = fullOrderInfo.getItem();
        if (Objects.isNull(items) || items.isEmpty()) {
            throw new OPServerException(200,"openFullOrderItems.is.null");
        }
        OpenFullOrderAddress address = fullOrderInfo.getAddress();
        if (Objects.isNull(address)) {
            throw new OPServerException(200,"openFullOrderAddress.is.null");
        }
    }

    /**
     * 验证时效订单是否存在
     *
     * @param fullOrderInfo
     * @return
     */
    protected Response<List<Long>> validateRealOrderIdsExist(OpenFullOrderInfo fullOrderInfo) {
        if (StringUtils.isBlank(fullOrderInfo.getOrder().getRealtimeOrderIds())) {
            return Response.fail("realtimeOrderIds is required");
        }
        List<String> outIds = Splitter.on(SymbolConsts.COMMA).trimResults().
            splitToList(fullOrderInfo.getOrder().getRealtimeOrderIds());
        if (CollectionUtils.isEmpty(outIds)){
            return Response.fail("realtimeOrderIds is required");
        }
        Response<List<ShopOrder>> response = middleOrderReadService.findByOutIdsAndOutFrom(
            outIds, fullOrderInfo.getOrder().getChannelCode());
        if (response == null
            || !response.isSuccess()) {
            return Response.fail("failed.to.validate.realtime.orders");
        }
        if (CollectionUtils.isEmpty(response.getResult())) {
            return Response.fail("realtimeOrderIds.not.exist");
        }
        List<Long> orderIds = response.getResult().stream().map(ShopOrder::getId).collect(Collectors.toList());
        return Response.ok(orderIds);
    }
}
