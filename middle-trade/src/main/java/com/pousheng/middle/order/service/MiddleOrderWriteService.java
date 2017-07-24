package com.pousheng.middle.order.service;

import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.impl.dao.SkuOrderDao;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;

import java.util.List;

/**
 * Created by tony on 2017/7/21.
 * pousheng-middle
 */
public interface MiddleOrderWriteService {

    /**
     * 更新总单与子单的状态(事物操作)以及回滚子单的待处理数量,整单取消使用
     * @param skuOrders
     * @param operation
     */
    public Response<Boolean> updateOrderStatusAnndSkuQuantities(ShopOrder shopOrder,List<SkuOrder> skuOrders, OrderOperation operation);

    /**
     * 更新订单状态,回滚子单待处理数量,子单取消使用
     * @param shopOrder 店铺订单
     * @param skuOrders 需要回滚成待处理状态的子单
     * @param skuOrder  需要撤销的子单
     * @param cancelOperation 撤销子单取消动作,取消成功或者取消失败
     * @return
     */
    public Response<Boolean> updateOrderStatusAndSkuQuantities4Sku(ShopOrder shopOrder, List<SkuOrder> skuOrders, SkuOrder skuOrder,OrderOperation cancelOperation,OrderOperation waitHandleOperation);
}
