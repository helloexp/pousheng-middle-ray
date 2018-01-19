package com.pousheng.middle.web.order.sync.mpos;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.web.order.component.AutoCompensateLogic;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 同步mpos订单状态
 * created by ph on 2017/01/10
 */
@Component
@Slf4j
public class SyncMposOrderLogic {


    @Autowired
    private SyncMposApi syncMposApi;

    @Autowired
    private AutoCompensateLogic autoCompensateLogic;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    /**
     * 同步无法派出商品至mpos
     * @param shopOrder                 订单
     * @param skuCodeAndQuantityList    商品编码及数量
     * @return
     */
    public void syncNotDispatcherSkuToMpos(ShopOrder shopOrder, List<SkuCodeAndQuantity> skuCodeAndQuantityList){
        Map<String,Object> param = this.assembNotDispatcherSkuParam(shopOrder,skuCodeAndQuantityList);
        Response<Boolean> response = this.syncNotDispatcherSkuToMpos(param);
        if(!response.isSuccess()){
            autoCompensateLogic.createAutoCompensationTask(param,TradeConstants.FAIL_NOT_DISPATCHER_SKU_TO_MPOS);
        }
    }

    /**
     * 同步无法派出商品至mpos
     * @return
     */
    public Response<Boolean> syncNotDispatcherSkuToMpos(Map<String,Object> param){
        try{
            Response<Boolean> response = mapper.fromJson(syncMposApi.syncNotDispatcherSkuToMpos(param),Response.class);
            if(!response.isSuccess() || Objects.equals(response.getResult(),false)){
                log.error("sync not dispatched sku to mpos fail,cause:{}",response.getError());
                throw new ServiceException("sync.not.dispatcher.sku.fail");
            }
        }catch (Exception e){
            log.error("sync not dispatched sku to mpos fail,cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("sync.not.dispatcher.sku.fail");
        }
        return Response.ok(true);
    }

    /**
     * 恒康收到退货 --> 中台 --> mpos
     * @param outerId
     * @return
     */
    public void notifyMposRefundReceived(String outerId){
        outerId = outerId.substring(outerId.indexOf("_")+1);
        Map<String,Object> param = Maps.newHashMap();
        param.put("afterSalesId",outerId);
        Response<Boolean> response = this.notifyMposRefundReceived(param);;
        if(!response.isSuccess()){
            autoCompensateLogic.createAutoCompensationTask(param,TradeConstants.FAIL_REFUND_RECEIVE_TO_MPOS);
        }
    }

    /**
     * 恒康收到退货 --> 中台 --> mpos
     * @param param
     * @return
     */
    public Response<Boolean> notifyMposRefundReceived(Map<String,Object> param){
        try{
            Response<Boolean> response = mapper.fromJson(syncMposApi.syncRefundReceive(param),Response.class);
            if(!response.isSuccess() || Objects.equals(response.getResult(),false)){
                log.error("sync refund receive to mpos fail,cause:{}",response.getError());
                throw new ServiceException("sync.refund.receive.fail");
            }
        }catch (Exception e){
            log.error("sync refund receive to mpos fail,cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("sync.refund.receive.fail");
        }
        return Response.ok(true);
    }

    /**
     * 组装参数
     * @param shopOrder                 订单
     * @param skuCodeAndQuantityList    商品代码和数量
     * @return
     */
    private Map<String,Object> assembNotDispatcherSkuParam(ShopOrder shopOrder,List<SkuCodeAndQuantity> skuCodeAndQuantityList){
        Map<String,Object> param = Maps.newHashMap();
        param.put("orderId",shopOrder.getOutId());
        List<String> skuCodes = skuCodeAndQuantityList.stream().map(SkuCodeAndQuantity::getSkuCode).collect(Collectors.toList());
        param.put("skuCodeList",mapper.toJson(skuCodes));
        return param;
    }

}
