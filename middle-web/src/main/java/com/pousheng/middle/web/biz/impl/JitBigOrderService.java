package com.pousheng.middle.web.biz.impl;

import com.google.common.collect.Lists;
import com.pousheng.middle.constants.CacheConsts;
import com.pousheng.middle.hksyc.component.JitOrderReceiptApi;
import com.pousheng.middle.hksyc.dto.JitOrderReceiptRequest;
import com.pousheng.middle.hksyc.dto.YJRespone;
import com.pousheng.middle.open.component.OpenOrderConverter;
import com.pousheng.middle.open.manager.RedisLockClient;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.Exception.ConcurrentSkipBizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.common.StringUtil;
import io.terminus.open.client.center.job.order.component.OrderExecutor;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.order.dto.OpenClientFullOrder;
import io.terminus.open.client.order.dto.OpenFullOrderInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.List;
import java.util.UUID;

/**
 * @author tanlongjun
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.OUT_OPEN_ORDER)
@Service
@Slf4j
public class JitBigOrderService implements CompensateBizService {

    @Autowired
    private RedisLockClient redisLockClient;

    @RpcConsumer
    private OpenShopReadService openShopReadService;

    @Autowired
    private OpenOrderConverter openOrderConverter;
    @Autowired
    private OrderExecutor orderExecutor;
    @Autowired
    private OpenShopCacher openShopCacher;

    @Autowired
    private JitOrderReceiptApi jitOrderReceiptApi;

    @RpcConsumer
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {

        if (null == poushengCompensateBiz) {
            log.warn("JITBigOrderService.doProcess params is null");
            return;
        }

        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("JITBigOrderService.doProcess context is null");
            throw new BizException("JITBigOrderService.doProcess context is null");
        }

        OpenFullOrderInfo fullOrderInfo = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(context, OpenFullOrderInfo.class);
        if (fullOrderInfo == null) {
            log.error("order param:{}", context);
            throw new BizException("could not handle the order info.");
        }

        String outOrderId=fullOrderInfo.getOrder().getOutOrderId();
        //加锁互斥
        String key = MessageFormat.format(
                CacheConsts.JITCacheKeys.ORDER_SYNC_LOCK_KEY_PATTERN,outOrderId);
        String ticket = UUID.randomUUID().toString();
        //发送回执
        JitOrderReceiptRequest request = new JitOrderReceiptRequest();
        request.setOrder_sn(outOrderId);
        request.setError_code(JitOrderReceiptApi.SUCCESS);
        try {
            boolean lock = redisLockClient.lock(key, CacheConsts.LONG_LOCK_TTL, ticket);
            if (!lock) {
                String msg=MessageFormat.format("order {} is processing.",outOrderId);
                throw new ConcurrentSkipBizException(msg);
            }
            //handle jit big order
            handle(fullOrderInfo);
        } catch (Exception e) {
            log.error("failed to handle JIT big order.param:{}", poushengCompensateBiz,e);
//            throw new BizException("failed to handle JIT big order,cause by {}", e);
            // TODO只执行一次
            request.setError_code(JitOrderReceiptApi.FAILED);

        } finally {
            redisLockClient.unlock(key, ticket);
        }

        YJRespone respone = jitOrderReceiptApi.sendReceipt(request);
        // 若回执发送失败 则创建补偿任务补发
        if (respone != null && 0 != respone.getError()) { //失败
            //保存bizTask
            String task = JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(request);
            saveReceiptTask(task);
        }
    }

    /**
     * 处理拣货单逻辑
     * @param openFullOrderInfo
     */
    private void handle(OpenFullOrderInfo openFullOrderInfo){
        String shopCode = openFullOrderInfo.getOrder().getCompanyCode()+"-"+openFullOrderInfo.getOrder().getShopCode();
        //查询店铺的信息，如果没有就新建一个
        Response<List<OpenClientShop>> rP = openShopReadService.search(null,null,shopCode);
        if (!rP.isSuccess()){
            log.error("find open shop failed,shopCode is {},caused by {}",shopCode,rP.getError());
            throw new ServiceException("find.open.shop.failed");
        }
        List<OpenClientShop> openClientShops = rP.getResult();
        java.util.Optional<OpenClientShop> openClientShopOptional =  openClientShops.stream().findAny();
        OpenClientShop openClientShop =   openClientShopOptional.get();
        Long openShopId = openClientShop.getOpenShopId();
        OpenShop openShop = openShopCacher.findById(openShopId);
        //组装参数
        OpenClientFullOrder openClientFullOrder = openOrderConverter.transform(openFullOrderInfo);
        orderExecutor.importOrder(openShop, Lists.newArrayList(openClientFullOrder));
    }

    /**
     * 保存发送回执任务
     * @param data
     */
    private void saveReceiptTask(String data){
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.JIT_ORDER_RECEIPT.toString());
        biz.setContext(data);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        poushengCompensateBizWriteService.create(biz);
    }

}
