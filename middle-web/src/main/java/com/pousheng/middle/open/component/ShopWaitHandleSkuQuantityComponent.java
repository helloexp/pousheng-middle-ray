package com.pousheng.middle.open.component;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.OrderWaitHandleType;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
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

    private final List<Integer> handleStatus = Lists.newArrayList(
            OrderWaitHandleType.WAIT_HANDLE.value(),//1, "尚未尝试自动生成发货单"
            OrderWaitHandleType.JD_PAY_ON_CASH.value(),//3, "京东货到付款"
            OrderWaitHandleType.STOCK_NOT_ENOUGH.value(),//5, "库存不足")
            OrderWaitHandleType.DISPATCH_ORDER_FAIL.value(),//7, "派单失败，请联系开发人员协助排查"
            OrderWaitHandleType.WAREHOUSE_SATE_STOCK_NOT_FIND.value(),//8, "存在安全库存没有设置的仓库"
            OrderWaitHandleType.ADDRESS_GPS_NOT_FOUND.value(),//9, "门店或仓库地址信息不存在"
            OrderWaitHandleType.FIND_ADDRESS_GPS_FAIL.value(),//10, "门店或仓库地址信息查询失败"
            OrderWaitHandleType.WAREHOUSE_STOCK_LOCK_FAIL.value(),//11, "mpos仓库商品库存锁定失败"
            OrderWaitHandleType.SHOP_STOCK_LOCK_FAIL.value(),//12, "mpos门店商品库存锁定失败"
            OrderWaitHandleType.WAREHOUSE_RULE_NOT_FOUND.value(),//13, "来源店铺没有配置对应的默认发货仓规则"
            OrderWaitHandleType.UNKNOWN_ERROR.value(),//14, "未知错误"
            OrderWaitHandleType.SHOP_MAPPING_MISS.value(),//15, "门店对应的店铺映射关系缺失"
            OrderWaitHandleType.ORIGIN_STATUS_SAVE.value(),//-1, "初始状态"
            OrderWaitHandleType.WAIT_AUTO_CREATE_SHIPMENT.value(),//0, "防止并发"
            OrderWaitHandleType.HANDLE_DONE.value(),//6, "已经处理"
            OrderWaitHandleType.NOTE_ORDER_NO_SOTCK.value());//17,"备注订单无库存"


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
            Integer shipping = skuOrder.getShipping();
            if (Arguments.isNull(shipping)){
                shipping = 0;
            }

            current.add(OrderWaitHandleType.from(shopOrder.getHandleStatus()).getDesc(),skuOrder.getQuantity() - shipping);
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
            Integer shipping = skuOrder.getShipping();
            if (Arguments.isNull(shipping)){
                shipping = 0;
            }
            waitHandleNumber = skuOrder.getQuantity() - shipping;
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
