package com.pousheng.middle.order.service;

import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.model.SkuOrder;

import java.util.List;
import java.util.Map;

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
    public Response<Boolean> updateOrderStatusAndSkuQuantities(ShopOrder shopOrder,List<SkuOrder> skuOrders, OrderOperation operation);

    /**
     * 更新订单状态,回滚子单待处理数量,子单取消使用
     * @param shopOrder 店铺订单
     * @param skuOrders 需要回滚成待处理状态的子单
     * @param skuOrder  需要撤销的子单
     * @param cancelOperation 撤销子单取消动作,取消成功或者取消失败
     * @param skuCode 子单撤单失败时添加的skuCode用于标识
     * @return
     */
    public Response<Boolean> updateOrderStatusAndSkuQuantitiesForSku(ShopOrder shopOrder, List<SkuOrder> skuOrders, SkuOrder skuOrder,OrderOperation cancelOperation,OrderOperation waitHandleOperation,String skuCode);

    /**
     * 更新子单的skuCode and skuId
     * @param skuId 子单skuId
     * @param skuCode 子单skuCode
     * @param id  子单主键(更新条件)
     * @return
     */
    public Response<Boolean> updateSkuOrderCodeAndSkuId(long skuId,String skuCode,long id);

    /**
     * 更新订单的收货信息
     * @param shopOrderId 店铺订单主键
     * @param receiverInfoMap 编辑的收货信息
     * @param buyerNote 买家备注
     * @return
     */
    public Response<Boolean> updateReceiveInfos(long shopOrderId, Map<String,Object> receiverInfoMap,String buyerNote);

    /**
     * 编辑订单的发票信息
     * @param shopOrderId 店铺订单主键
     * @param invoicesMap 编辑的发票信息
     * @return
     */
    public Response<Boolean> updateInvoices(long shopOrderId, Map<String,String> invoicesMap,String title);

    /**
     * 更新订单的售后地址信息
     *
     * @param shopOrderId  店铺订单id
     * @param receiverInfo 新的收货地址信息
     * @return 是否更新成功
     */
    Response<Boolean> updateReceiveInfo(Long shopOrderId, ReceiverInfo receiverInfo);

    /**
     * 更新订单的买家名称
     *
     * @param shopOrderId 店铺订单id
     * @param buyerName   新的买家名称
     * @return 是否更新成功
     */
    Response<Boolean> updateBuyerNameOfOrder(Long shopOrderId, String buyerName);
}
