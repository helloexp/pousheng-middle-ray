package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.impl.manager.MiddleOrderManager;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by tony on 2017/7/21.
 * pousheng-middle
 */
@Slf4j
@Service
public class MiddleOrderWriteServiceImpl implements MiddleOrderWriteService{
    @Autowired
    private MiddleOrderManager middleOrderManager;
    @Override
    public Response<Boolean> updateOrderStatusAndSkuQuantities(ShopOrder shopOrder,List<SkuOrder> skuOrders, OrderOperation operation) {
        try{
            //更新订单状态逻辑,带事物
            middleOrderManager.updateOrderStatusAndSkuQuantities(shopOrder,skuOrders,operation);
            return Response.ok();

        }catch (ServiceException e1){
            log.error("failed to update order.cause:{}",Throwables.getStackTraceAsString(e1));
            return Response.fail(e1.getMessage());
        } catch (Exception e){
            log.error("failed to update order, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("order.update.fail");
        }
    }

    @Override
    public Response<Boolean> updateOrderStatusAndSkuQuantitiesForSku(ShopOrder shopOrder, List<SkuOrder> skuOrders, SkuOrder skuOrder, OrderOperation cancelOperation, OrderOperation waitHandleOperation,String skuCode) {
        try{
            //更新订单状态逻辑,带事物
            middleOrderManager.updateOrderStatusAndSkuQuantitiesForSku(shopOrder,skuOrders,skuOrder,cancelOperation,waitHandleOperation,skuCode);
            return Response.ok();

        }catch (ServiceException e1){
            log.error("failed to update order.cause:{}",Throwables.getStackTraceAsString(e1));
            return Response.fail(e1.getMessage());
        } catch (Exception e){
            log.error("failed to update order, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("order.update.fail");
        }
    }


}
