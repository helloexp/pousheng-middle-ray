package com.pousheng.middle.open.component;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.OrderWaitHandleType;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.order.service.SkuOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 店铺待处理商品数量组件
 * Created by songrenfei on 2019/4/18
 */
@Component
@Slf4j
public class ShopWaitHandleSkuQuantityComponent {

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;
    @RpcConsumer
    private SkuOrderReadService skuOrderReadService;

    private final List<Integer> handleStatus = Lists.newArrayList(OrderWaitHandleType.WAIT_HANDLE.value(),
            OrderWaitHandleType.JD_PAY_ON_CASH.value(),OrderWaitHandleType.STOCK_NOT_ENOUGH.value(),
            OrderWaitHandleType.DISPATCH_ORDER_FAIL.value(),
            OrderWaitHandleType.WAREHOUSE_SATE_STOCK_NOT_FIND.value(),
            OrderWaitHandleType.ADDRESS_GPS_NOT_FOUND.value(),
            OrderWaitHandleType.FIND_ADDRESS_GPS_FAIL.value(),
            OrderWaitHandleType.WAREHOUSE_STOCK_LOCK_FAIL.value(),
            OrderWaitHandleType.SHOP_STOCK_LOCK_FAIL.value(),
            OrderWaitHandleType.WAREHOUSE_RULE_NOT_FOUND.value(),
            OrderWaitHandleType.UNKNOWN_ERROR.value(),
            OrderWaitHandleType.SHOP_MAPPING_MISS.value(),
            OrderWaitHandleType.ORIGIN_STATUS_SAVE.value(),
            OrderWaitHandleType.WAIT_AUTO_CREATE_SHIPMENT.value(),
            OrderWaitHandleType.HANDLE_DONE.value(),
            OrderWaitHandleType.NOTE_ORDER_NO_SOTCK.value());


    /**
     * 查询店铺指定商品条码的待处理数量
     * @param shopId 店铺ID
     * @param skuCode 商品条码
     * @return 待处理数量
     */
    public Long queryWaitHandleQuantiy(Long shopId,String skuCode){

        Long waitHandleNumber = 0L;
        Response<List<SkuOrder>> skuOrderRes =  skuOrderReadService.findByShopIdAndSkuCodeAndStatus(shopId,skuCode, MiddleOrderStatus.WAIT_HANDLE.getValue());
        if( !skuOrderRes.isSuccess()){
            log.error("fail to find wait handle sku order by shop  id {}, sku code:{}, error:{}",
                    shopId,skuCode,skuOrderRes.getError());
            return waitHandleNumber;
        }

        for (SkuOrder skuOrder : skuOrderRes.getResult()){
            waitHandleNumber += querySkuOrderWaitHandleNumber(skuOrder);
        }

        return waitHandleNumber;
    }


    public Multiset<String> queryWaitHandleQuantiyDetail(Long shopId,String skuCode){

        Multiset<String> current = ConcurrentHashMultiset.create();

        Response<List<SkuOrder>> skuOrderRes =  skuOrderReadService.findByShopIdAndSkuCodeAndStatus(shopId,skuCode, MiddleOrderStatus.WAIT_HANDLE.getValue());
        if( !skuOrderRes.isSuccess()){
            log.error("fail to find wait handle sku order by shop  id {}, sku code:{}, error:{}",
                    shopId,skuCode,skuOrderRes.getError());
            current.add("统计失败");
            return current;
        }

        for (SkuOrder skuOrder : skuOrderRes.getResult()){
            querySkuOrderWaitHandleNumberDetail(skuOrder,current);
        }

        return current;
    }

    private void querySkuOrderWaitHandleNumberDetail(SkuOrder skuOrder,Multiset<String> current){

        Response<ShopOrder> response = shopOrderReadService.findById(skuOrder.getOrderId());
        if( !response.isSuccess()){
            log.error("fail to find shop order by   id {},error:{}",
                    skuOrder.getOrderId(),response.getError());
            return;
        }

        ShopOrder shopOrder = response.getResult();

        //订单状态非待处理直接返回0
        if (!Objects.equals(shopOrder.getStatus(),MiddleOrderStatus.WAIT_HANDLE.getValue())){
            return;
        }

        //判断待处理状态是否符合条件
        if (isWaitHandle(shopOrder)){
            current.add(OrderWaitHandleType.from(shopOrder.getHandleStatus()).getDesc(),skuOrder.getQuantity());
        }
    }

    private Integer querySkuOrderWaitHandleNumber(SkuOrder skuOrder){

        Integer waitHandleNumber = 0;
        Response<ShopOrder> response = shopOrderReadService.findById(skuOrder.getOrderId());
        if( !response.isSuccess()){
            log.error("fail to find shop order by   id {},error:{}",
                    skuOrder.getOrderId(),response.getError());
            return waitHandleNumber;
        }

        ShopOrder shopOrder = response.getResult();

        //订单状态非待处理直接返回0
        if (!Objects.equals(shopOrder.getStatus(),MiddleOrderStatus.WAIT_HANDLE.getValue())){
            return waitHandleNumber;
        }

        //判断待处理状态是否符合条件
        if (isWaitHandle(shopOrder)){
            waitHandleNumber = skuOrder.getQuantity();
        }

        return waitHandleNumber;
    }

    private boolean isWaitHandle(ShopOrder shopOrder) {

        log.info("handleStatus values:{}",handleStatus);

        if (handleStatus.contains(shopOrder.getHandleStatus())){
           return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }


}
