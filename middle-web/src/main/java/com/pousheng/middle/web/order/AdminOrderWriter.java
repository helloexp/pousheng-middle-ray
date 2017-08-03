package com.pousheng.middle.web.order;

import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ExpressCodeCriteria;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.service.ExpressCodeReadService;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.sync.ecp.SyncOrderToEcpLogic;
import com.pousheng.middle.web.utils.permission.PermissionCheck;
import com.pousheng.middle.web.utils.permission.PermissionCheckParam;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.order.model.Invoice;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by tony on 2017/7/18.
 * pousheng-middle
 */
@RestController
@Slf4j
public class AdminOrderWriter {
    @Autowired
    private SyncOrderToEcpLogic syncOrderToEcpLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @RpcConsumer
    private OpenShopReadService openShopReadService;
    @Autowired
    private ExpressCodeReadService expressCodeReadService;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private MiddleOrderWriteService middleOrderWriteService;

    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();
    /**
     * 发货单已发货,同步订单信息到电商
     * @param shopOrderId
     */
    @RequestMapping(value = "api/order/{id}/sync/ecp",method = RequestMethod.PUT)
    @PermissionCheck(PermissionCheck.PermissionCheckType.SHOP_ORDER)
    public void syncOrderInfoToEcp(@PathVariable(value = "id") @PermissionCheckParam Long shopOrderId){
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        //获取发货单id
        String ecpShipmentId = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.ECP_SHIPMENT_ID,shopOrder);
        Shipment shipment  = shipmentReadLogic.findShipmentById(Long.valueOf(ecpShipmentId));
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        ExpressCode expressCode = makeExpressNameByhkCode(shipmentExtra.getShipmentCorpCode());
        //同步到电商平台
        String expressCompanyCode = orderReadLogic.getExpressCode(shopOrder.getShopId(),expressCode);
        Response<Boolean> syncRes =syncOrderToEcpLogic.syncOrderToECP(shopOrder,expressCompanyCode,shipment.getId());
        if(!syncRes.isSuccess()){
            log.error("sync shopOrder(id:{}) to ecp fail,error:{}",shopOrderId,syncRes.getError());
            throw new JsonResponseException(syncRes.getError());
        }

    }

    /**
     * 取消子订单(自动)
     * @param shopOrderId
     * @param skuCode
     */
    @RequestMapping(value = "api/order/{id}/auto/cancel/sku/order", method = RequestMethod.PUT)
    public void autoCancelSkuOrder(@PathVariable("id") Long shopOrderId, @RequestParam("skuCode") String skuCode) {
        orderWriteLogic.autoCancelSkuOrder(shopOrderId,skuCode);
    }

    /**
     * 整单撤销,状态恢复成初始状态
     * @param shopOrderId
     */
    @RequestMapping(value = "api/order/{id}/rollback/shop/order",method = RequestMethod.PUT)
    public void rollbackShopOrder(@PathVariable("id")Long shopOrderId){
      orderWriteLogic.rollbackShopOrder(shopOrderId);
    }

    /**
     * 订单(包括整单和子单)取消失败,手工操作逻辑
     * @param shopOrderId
     */
    @RequestMapping(value="api/order/{id}/cancel/order",method = RequestMethod.PUT)
    public void cancelShopOrder(@PathVariable("id") Long shopOrderId){
        //判断是整单取消还是子单取消
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        //获取是否存在失败的sku记录
        String skuCodeCanceled = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.SKU_CODE_CANCELED,shopOrder);
        if(StringUtils.isNotEmpty(skuCodeCanceled)){
            orderWriteLogic.cancelSkuOrder(shopOrderId,skuCodeCanceled);
        }else{
            orderWriteLogic.cancelShopOrder(shopOrderId);
        }

    }
    /**
     * 整单取消,子单整单发货单状态变为已取消(自动)
     * @param shopOrderId
     */
    @RequestMapping(value="api/order/{id}/auto/cancel/shop/order",method = RequestMethod.PUT)
    public void autoCancelShopOrder(@PathVariable("id") Long shopOrderId){
        orderWriteLogic.autoCancelShopOrder(shopOrderId);
    }



    /**
     * 电商确认收货,此时通知中台修改shopOrder中ecpOrderStatus状态为已完成
     * @param shopOrderId
     */
    @RequestMapping(value = "api/order/{id}/confirm",method = RequestMethod.PUT)
    public void confirmOrders(@PathVariable("id") Long shopOrderId){
        ShopOrder shopOrder =  orderReadLogic.findShopOrderById(shopOrderId);
        orderWriteLogic.updateEcpOrderStatus(shopOrder, MiddleOrderEvent.CONFIRM.toOrderOperation());
    }

    /**
     *修改skuCode 和skuId
     * @param id sku订单主键
     * @param skuCode 中台条码
     * @param skuId   skuId
     */
    @RequestMapping(value ="/api/sku/order/{id}/update/sku/code",method = RequestMethod.PUT)
    public void  updateSkuOrderCodeAndSkuId(@PathVariable("id") Long id, @RequestParam("skuCode") String skuCode,@RequestParam("skuId") String skuId){
        //判断该订单是否生成过发货单
        Boolean result = orderReadLogic.isShipmentCreated(id);
        if (!result){
            throw new JsonResponseException("shipment.exist.can.not.edit.sku.code");
        }
        Response<Boolean> response = middleOrderWriteService.updateSkuOrderCodeAndSkuId(Long.parseLong(skuId),skuCode,id);
        if (!response.isSuccess()){
            log.error("update skuCode failed,skuCodeId is({})",id);
            throw new JsonResponseException(response.getError());
        }
    }

    /**
     *  添加中台客服备注,各个状态均可添加
     * @param id  店铺订单主键
     * @param customerSerivceNote 客服备注
     */
    @RequestMapping(value ="/api/order/{id}/add/customer/service/note",method = RequestMethod.PUT)
    public void addCustomerServiceNote(@PathVariable("id") Long id, @RequestParam("customerSerivceNote") String customerSerivceNote){
       orderWriteLogic.addCustomerServiceNote(id,customerSerivceNote);
    }

    /**
     * 修改订单的收货信息
     * @param id 店铺订单主键
     * @param data 收货信息实体
     * @param buyerNote 买家备注
     * @return true (更新成功)or false (更新失败)
     */
    @RequestMapping(value = "/api/order/{id}/edit/receiver/info",method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public void editReceiverInfos(@PathVariable("id")Long id, @RequestParam("data")String data,@RequestParam(value = "buyerNote",required = false) String buyerNote){
        Boolean result = orderReadLogic.isShipmentCreatedForShopOrder(id);
        if (!result){
            throw new JsonResponseException("shipment.exist.can.not.edit.sku.code");
        }
        Map<String,String> receiverInfoMap = JSON_MAPPER.fromJson(data, JSON_MAPPER.createCollectionType(HashMap.class, String.class, String.class));
        if(receiverInfoMap == null) {
            log.error("failed to parse receiverInfoMap:{}",data);
            throw new JsonResponseException("receiver.info.map.invalid");
        }
        Response<Boolean> response = middleOrderWriteService.updateReceiveInfos(id,receiverInfoMap,buyerNote);
        if (!response.isSuccess()){
            log.error("failed to edit receiver info:{},shopOrderId is(={})",data,id);
            throw new JsonResponseException(response.getError());
        }
    }

    /**
     * 修改订单的发票信息
     * @param id
     * @param data
     * @return
     */
    @RequestMapping(value = "/api/order/{id}/edit/invoice",method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public void editInvoiceInfos(@PathVariable("id")Long id,@RequestParam("data")String data){
        Boolean result = orderReadLogic.isShipmentCreatedForShopOrder(id);
        if (!result){
            throw new JsonResponseException("shipment.exist.can.not.edit.sku.code");
        }
        Map<String,String> invoiceMap = JSON_MAPPER.fromJson(data, JSON_MAPPER.createCollectionType(HashMap.class, String.class, String.class));
        if(invoiceMap == null) {
            log.error("failed to parse invoiceMap:{}",data);
            throw new JsonResponseException("invoice.map.invalid");
        }
        Response<Boolean> response = middleOrderWriteService.updateInvoices(id,invoiceMap);
        if (!response.isSuccess()){
            log.error("failed to edit invoiceMap:{}",data);
            throw new JsonResponseException(response.getError());
        }
    }

    private ExpressCode makeExpressNameByhkCode(String hkExpressCode) {
        ExpressCodeCriteria criteria = new ExpressCodeCriteria();
        criteria.setHkCode(hkExpressCode);
        Response<Paging<ExpressCode>> response = expressCodeReadService.pagingExpressCode(criteria);
        if (!response.isSuccess()) {
            log.error("failed to pagination expressCode with criteria:{}, error code:{}", criteria, response.getError());
            throw new JsonResponseException(response.getError());
        }
        if (response.getResult().getData().size() == 0) {
            log.error("there is not any express info by hkCode:{}", hkExpressCode);
            throw new JsonResponseException("express.info.is.not.exist");
        }
        ExpressCode expressCode = response.getResult().getData().get(0);
        return expressCode;
    }
}
