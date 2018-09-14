package com.pousheng.middle.open.api.jit;

import com.google.common.base.Splitter;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.constants.SymbolConsts;
import com.pousheng.middle.open.manager.JitOrderManager;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.MiddleOrderReadService;
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
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.constraints.NotNull;
import java.text.MessageFormat;
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
    public OPResponse<String> saveRealTimeOrder(@NotNull(message = "order.info.is.null") String orderInfo) {
        try {
            OPResponse<OpenFullOrderInfo> basicVilidateResp = validateBaiscParam(orderInfo);
            if (!basicVilidateResp.isSuccess()) {
                return OPResponse.fail(basicVilidateResp.getError());
            }
            OpenFullOrderInfo fullOrderInfo = basicVilidateResp.getResult();
            //参数验证
            OPResponse<String> validateResponse = validateBaiscParam(fullOrderInfo);
            if (!validateResponse.isSuccess()) {
                return validateResponse;
            }
            jitOrderManager.handleRealTimeOrder(fullOrderInfo);
        } catch (Exception e) {
            log.error("failed to save jit realtime order.param:{}", orderInfo, e);
            return OPResponse.fail("failed.save.jit.realtime.order", e.getMessage());
        }
        return OPResponse.ok();
    }

    /**
     * 保存推送的订单
     *
     * @param orderInfo
     * @return
     */
    @OpenMethod(key = "push.out.jit.order.api", paramNames = {"orderInfo"}, httpMethods = RequestMethod.POST)
    public OPResponse<String> saveOrder(@NotNull(message = "order.info.is.null") String orderInfo) {
        try {
            OPResponse<OpenFullOrderInfo> basicVilidateResp = validateBaiscParam(orderInfo);
            if (!basicVilidateResp.isSuccess()) {
                return OPResponse.fail(basicVilidateResp.getError());
            }
            OpenFullOrderInfo fullOrderInfo = basicVilidateResp.getResult();
            //参数验证
            OPResponse<String> validateResponse = validateBaiscParam(fullOrderInfo);
            if (!validateResponse.isSuccess()) {
                return validateResponse;
            }
            //验证时效订单是否存在
            OPResponse<List<Long>> realOrderValidateResp = validateRealOrderIdsExist(fullOrderInfo);
            if (!validateResponse.isSuccess()) {
                return OPResponse.fail(realOrderValidateResp.getError());
            }
            List<String> skuCodes = fullOrderInfo.getItem().stream().
                map(OpenFullOrderItem::getSkuCode).collect(Collectors.toList());
            //验证skuItem是否存在
            OPResponse<String> skuItemValidateResp = validateSkuItemExist(realOrderValidateResp.getResult(),
                skuCodes);
            if (!skuItemValidateResp.isSuccess()) {
                return OPResponse.fail(realOrderValidateResp.getError());
            }
            //save to db
            Response<Long> response = saveDataToTask(orderInfo);
            if (response.isSuccess()) {
                // publish event
                // 主动消费则时效性更好
                HandleJITBigOrderEvent event = new HandleJITBigOrderEvent();
                event.setId(response.getResult());
                eventBus.post(event);
                log.info("post event completed");
                return OPResponse.ok("success");
            } else {
                return OPResponse.fail("failed.save.jit.big.order");
            }

        } catch (Exception e) {
            log.error("failed to save jit big order.param:{}", orderInfo, e);
            return OPResponse.fail("failed.save.jit.big.order", e.getMessage());
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

    protected OPResponse<OpenFullOrderInfo> validateBaiscParam(String orderInfo) {
        if (StringUtils.isBlank(orderInfo)) {
            return OPResponse.fail("orderInfo is required");
        }
        OpenFullOrderInfo result = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(orderInfo, OpenFullOrderInfo.class);
        return OPResponse.ok(result);
    }

    /**
     * 业务参数验证
     *
     * @param fullOrderInfo
     * @return
     */
    protected OPResponse<String> validateBaiscParam(OpenFullOrderInfo fullOrderInfo) {
        if (fullOrderInfo == null) {
            return OPResponse.fail("param orderInfo incorrect");
        }
        OpenFullOrder openFullOrder = fullOrderInfo.getOrder();
        if (Objects.isNull(openFullOrder)) {
            return OPResponse.fail("openFullOrder.is.null");
        }
        List<OpenFullOrderItem> items = fullOrderInfo.getItem();
        if (Objects.isNull(items) || items.isEmpty()) {
            return OPResponse.fail("openFullOrderItems.is.null");
        }
        OpenFullOrderAddress address = fullOrderInfo.getAddress();
        if (Objects.isNull(address)) {
            return OPResponse.fail("openFullOrderAddress.is.null");
        }
        return OPResponse.ok();
    }

    /**
     * 验证时效订单是否存在
     *
     * @param fullOrderInfo
     * @return
     */
    protected OPResponse<List<Long>> validateRealOrderIdsExist(OpenFullOrderInfo fullOrderInfo) {
        if (StringUtils.isBlank(fullOrderInfo.getOrder().getRealtimeOrderIds())) {
            return OPResponse.fail("realtimeOrderIds is required");
        }
        List<String> outIds = Splitter.on(SymbolConsts.COMMA).trimResults().
            splitToList(fullOrderInfo.getOrder().getRealtimeOrderIds());
        if(CollectionUtils.isEmpty(outIds)){
            return OPResponse.fail("realtimeOrderIds is required");
        }
        Response<List<ShopOrder>> response = middleOrderReadService.findByOutIdsAndOutFrom(
            outIds, fullOrderInfo.getOrder().getChannelCode());
        if (response == null
            || !response.isSuccess()) {
            return OPResponse.fail("failed.to.validate.realtime.orders");
        }
        if (CollectionUtils.isEmpty(response.getResult())) {
            return OPResponse.fail("realtimeOrderIds.not.exist");
        }
        List<Long> orderIds = response.getResult().stream().map(ShopOrder::getId).collect(Collectors.toList());
        return OPResponse.ok(orderIds);
    }

    /**
     * 验证skucode是不是都被时效订单推送过来了
     *
     * @param orderIds
     * @param skuCodes
     * @return
     */
    protected OPResponse<String> validateSkuItemExist(List<Long> orderIds, List<String> skuCodes) {
        if (CollectionUtils.isEmpty(skuCodes)) {
            return OPResponse.fail("sku is required");
        }
        Response<List<String>> response = middleOrderReadService.findSkuCodesByOrderIds(orderIds);
        if (response == null
            || !response.isSuccess()) {
            return OPResponse.fail("failed.to.validate.realtime.orders");
        }
        for (String code : skuCodes) {
            if (response.getResult().contains(code)) {
                String msg = MessageFormat.format("realorders not contain skuCode {0}", code);
                return OPResponse.fail(msg);
            }
        }
        return OPResponse.ok();
    }
}
