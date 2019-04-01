package com.pousheng.middle.web.order;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.open.ych.logger.events.OrderOpEvent;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.MiddleOrderCriteria;
import com.pousheng.middle.order.dto.ShopOrderPagingInfo;
import com.pousheng.middle.order.dto.ShopOrderWithReceiveInfo;
import com.pousheng.middle.order.dto.WaitShipItemInfo;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.enums.OrderWaitHandleType;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.web.order.component.MiddleOrderFlowPicker;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.utils.permission.PermissionCheck;
import com.pousheng.middle.web.utils.permission.PermissionCheckParam;
import com.pousheng.middle.web.utils.permission.PermissionUtil;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.channel.OpenClientChannel;
import io.terminus.parana.common.constants.JitConsts;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.order.dto.OrderDetail;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.ReceiverInfoReadService;
import io.terminus.parana.order.service.ShopOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.mockito.internal.util.collections.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mail: F@terminus.io
 * Data: 16/6/28
 * Author: yangzefeng
 */
@RestController
@Slf4j
@PermissionCheck(PermissionCheck.PermissionCheckType.SHOP_ORDER)
@OperationLogModule(OperationLogModule.Module.ORDER)
public class AdminOrderReader {

    @Autowired
    private OrderReadLogic orderReadLogic;
    @RpcConsumer
    private MiddleOrderReadService middleOrderReadService;
    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @Autowired
    private PermissionUtil permissionUtil;
    @RpcConsumer
    private ReceiverInfoReadService receiverInfoReadService;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    @Autowired
    private EventBus eventBus;

    /**
     * 交易订单分页
     * @param middleOrderCriteria 查询参数
     * @return 订单分页结果
     */
    @RequestMapping(value = "/api/order/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Paging<ShopOrderPagingInfo>> findBy(MiddleOrderCriteria middleOrderCriteria) {
        String criteriaStr = JsonMapper.nonEmptyMapper().toJson(middleOrderCriteria);
        if(log.isDebugEnabled()){
            log.debug("API-ORDER-PAGING-START param: middleOrderCriteria [{}] ",criteriaStr);
        }
        if(middleOrderCriteria.getOutCreatedEndAt()!=null){
            middleOrderCriteria.setOutCreatedEndAt(new DateTime(middleOrderCriteria.getOutCreatedEndAt().getTime()).plusDays(1).minusSeconds(1).toDate());
        }

        List<Long> currentUserCanOperatShopIds = permissionUtil.getCurrentUserCanOperateShopIDs();
        if (middleOrderCriteria.getShopId() == null) {
            middleOrderCriteria.setShopIds(currentUserCanOperatShopIds);
        } else if (!currentUserCanOperatShopIds.contains(middleOrderCriteria.getShopId())) {
            throw new JsonResponseException("permission.check.query.deny");
        }
        if (StringUtils.isNotEmpty(middleOrderCriteria.getMobile())){
          middleOrderCriteria.setOutBuyerId(middleOrderCriteria.getMobile());
        }
        //不显示jit时效订单来源
        middleOrderCriteria.setExcludeOutFrom(JitConsts.YUNJU_REALTIME);

        Response<Paging<ShopOrder>> pagingRes;
        if (middleOrderCriteria.getStatus() != null && middleOrderCriteria.getStatus().contains(99)) {
        	try {
        		SimpleDateFormat formatter  = new SimpleDateFormat("yyyy-MM-dd");
        		if(middleOrderCriteria.getOutCreatedStartAt()==null){
            		Calendar caStart = Calendar.getInstance();
            		caStart.setTime(formatter.parse(formatter.format(new Date())));
            		caStart.add(Calendar.MONTH, -1);
                    middleOrderCriteria.setOutCreatedStartAt(new DateTime(caStart.getTime()).toDate());
                }
        		if(middleOrderCriteria.getOutCreatedEndAt()==null){
        			Calendar caEnd = Calendar.getInstance();
        			caEnd.setTime(formatter.parse(formatter.format(new Date())));
                    caEnd.add(Calendar.DATE, 1);
                    caEnd.add(Calendar.SECOND,-1);
                    middleOrderCriteria.setOutCreatedEndAt(new DateTime(caEnd.getTime()).toDate());
        		}
        		// compare 
        		
        		long diff = middleOrderCriteria.getOutCreatedEndAt().getTime() - middleOrderCriteria.getOutCreatedStartAt().getTime();
        		diff = diff/1000/60/60/24;
        		if (diff > 31) {
        			return Response.fail("over 30 days");
        		}
        	}catch(Exception e) {
        		return Response.fail(e.getMessage());
        	}
        	
        	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        	pagingRes = middleOrderReadService.findByecpOrderStatus((middleOrderCriteria.getPageNo()-1)*middleOrderCriteria.getPageSize(),middleOrderCriteria.getPageSize(),sdf.format(middleOrderCriteria.getOutCreatedStartAt()),sdf.format(middleOrderCriteria.getOutCreatedEndAt()));
        }else {
        	pagingRes =  middleOrderReadService.pagingShopOrder(middleOrderCriteria); 
        }
         
        if(!pagingRes.isSuccess()){
            return Response.fail(pagingRes.getError());
        }
        Flow flow = flowPicker.pickOrder();
        List<ShopOrder> shopOrders = pagingRes.getResult().getData();
        Paging<ShopOrderPagingInfo> pagingInfoPaging = Paging.empty();
        List<ShopOrderPagingInfo> pagingInfos = Lists.newArrayListWithCapacity(shopOrders.size());
        shopOrders.forEach(shopOrder -> {
            ShopOrderPagingInfo shopOrderPagingInfo = new ShopOrderPagingInfo();
            shopOrderPagingInfo.setShopOrder(shopOrder);
            //如果是mpos订单，不允许有其他操作。
            if(!shopOrder.getExtra().containsKey(TradeConstants.IS_ASSIGN_SHOP)){
                String ecpOrderStatus = orderReadLogic.getOrderExtraMapValueByKey(TradeConstants.ECP_ORDER_STATUS,shopOrder);
                shopOrderPagingInfo.setShopOrderOperations(shipmentReadLogic.isShopOrderCanRevoke(shopOrder)
                        ?flow.availableOperations(shopOrder.getStatus())
                        :flow.availableOperations(shopOrder.getStatus()).stream().filter(it->it.getValue()!=MiddleOrderEvent.REVOKE.getValue()).collect(Collectors.toSet()));
            }
            //待处理的单子如果是派单失败的 允许出现 不包含有备注订单
            if (shopOrder.getOutFrom().equals(MiddleChannel.VIPOXO.getValue()) && shopOrder.getStatus().equals(MiddleOrderStatus.WAIT_HANDLE.getValue())) {
                if (shopOrder.getHandleStatus() != null && shopOrder.getHandleStatus() > OrderWaitHandleType.ORDER_HAS_NOTE.value()) {
                    Set<OrderOperation> orderOperations = Sets.newSet();
                    orderOperations.addAll(shopOrderPagingInfo.getShopOrderOperations());
                    orderOperations.add(MiddleOrderEvent.NOTICE_VIP_UNDERCARRIAGE.toOrderOperation());
                    shopOrderPagingInfo.setShopOrderOperations(orderOperations);
                }
            }
            pagingInfos.add(shopOrderPagingInfo);
        });
        //撤销时必须保证订单没有发货
        pagingInfoPaging.setData(pagingInfos);
        pagingInfoPaging.setTotal(pagingRes.getResult().getTotal());
        if(log.isDebugEnabled()){
            log.debug("API-ORDER-PAGING-END param: middleOrderCriteria [{}] ,resp: [{}]",criteriaStr,JsonMapper.nonEmptyMapper().toJson(pagingInfoPaging));
        }
        return Response.ok(pagingInfoPaging);

    }


    /**
     * 交易订单详情
     * @param id 交易订单id
     * @return 订单详情DTO
     */
    @RequestMapping(value = "/api/order/{id}/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<OrderDetail> detail(@PathVariable("id") @PermissionCheckParam Long id,
                                        HttpServletRequest request) {
        if(log.isDebugEnabled()){
            log.debug("API-ORDER-DETAIL-START param: id [{}] ",id);
        }
        Response<OrderDetail> response=  orderReadLogic.orderDetail(id);
        sendLogForTaobao(response,request);
        if(log.isDebugEnabled()){
            log.debug("API-ORDER-DETAIL-END param: id [{}] ,resp: [{}]",id,JsonMapper.nonEmptyMapper().toJson(response));
        }
        return response;
    }


    /**
     * 交易订单待处理商品列表 for 手动生成发货单流程的选择仓库页面
     * @param id 交易订单id
     * @return 待发货商品列表 注意：待发货数量(waitHandleNumber) = 下单数量 - 已发货数量 ,waitHandleNumber为skuOrder.extraMap中的一个key，value为待发货数量
     */
    @RequestMapping(value = "/api/order/{id}/wait/handle/sku", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<WaitShipItemInfo> orderWaitHandleSku(@PathVariable("id") @PermissionCheckParam Long id) {
        if(log.isDebugEnabled()){
            log.debug("API-ORDER-ORDERWAITHANDLESKU-START param: id [{}] ",id);
        }
        List<SkuOrder> skuOrders =  orderReadLogic.findSkuOrderByShopOrderIdAndStatus(id, MiddleOrderStatus.WAIT_HANDLE.getValue(),MiddleOrderStatus.WAIT_ALL_HANDLE_DONE.getValue());
        List<WaitShipItemInfo> waitShipItemInfos = Lists.newArrayListWithCapacity(skuOrders.size());
        for (SkuOrder skuOrder : skuOrders){
            WaitShipItemInfo waitShipItemInfo = new WaitShipItemInfo();
            waitShipItemInfo.setSkuOrderId(skuOrder.getId());
            waitShipItemInfo.setSkuCode(skuOrder.getSkuCode());
            waitShipItemInfo.setOutSkuCode(skuOrder.getOutSkuId());
            waitShipItemInfo.setSkuName(skuOrder.getItemName());
            if (skuOrder.getShipmentType()!=null&&Objects.equals(skuOrder.getShipmentType(),1)){
                waitShipItemInfo.setIsGift(true);
            }else{
                waitShipItemInfo.setIsGift(false);
            }
            String outItemId="";
            try{
                outItemId =  orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.MIDDLE_OUT_ITEM_ID,skuOrder);
            }catch (Exception e){
                log.info("outItemmId is not exist");
            }
            waitShipItemInfo.setItemId(outItemId);
            waitShipItemInfo.setSkuAttrs(skuOrder.getSkuAttrs());
            waitShipItemInfo.setWaitHandleNumber(Integer.valueOf(orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.WAIT_HANDLE_NUMBER,skuOrder)));
            waitShipItemInfos.add(waitShipItemInfo);
        }
        if(log.isDebugEnabled()){
            log.debug("API-ORDER-ORDERWAITHANDLESKU-END param: id [{}] ,resp: [{}]",id,JsonMapper.nonEmptyMapper().toJson(waitShipItemInfos));
        }
        return waitShipItemInfos;
    }


    /**
     * 判断交易订单是否存在
     * @param id 交易订单id
     * @return boolean类型 ，true为存在，false为不存在
     */
    @RequestMapping(value = "/api/order/{id}/exist", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean checkExist(@PathVariable("id") @PermissionCheckParam Long id) {
        if(log.isDebugEnabled()){
            log.debug("API-ORDER-CHECKEXIST-START param: id [{}] ",id);
        }
        Response<ShopOrder> shopOrderRes = shopOrderReadService.findById(id);
        if(!shopOrderRes.isSuccess()){
            log.error("find shop order by id:{} fail,error:{}",id,shopOrderRes.getError());
            if(Objects.equals(shopOrderRes.getError(),"order.not.found")){
                return Boolean.FALSE;
            }
            throw new JsonResponseException(shopOrderRes.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-ORDER-CHECKEXIST-END param: id [{}] ",id);
        }
        return Boolean.TRUE;

    }

    /**
     * 订单信息和收货地址信息封装 for 新建售后订单展示订单信息
     * @param code 交易订单id
     * @return 订单信息和收货地址信息封装DTO
     */
    @RequestMapping(value = "/api/order/{code}/for/after/sale", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<ShopOrderWithReceiveInfo> afterSaleOrderInfo(@PathVariable("code") @PermissionCheckParam String code) {
        if(log.isDebugEnabled()){
            log.debug("API-ORDER-AFTERSALEORDERINFO-START param: code [{}] ",code);
        }
        Response<ShopOrder> shopOrderRes = shopOrderReadService.findByOrderCode(code);
        if(!shopOrderRes.isSuccess()){
            log.error("find shop order by code:{} fail,error:{}",code,shopOrderRes.getError());
            return Response.fail(shopOrderRes.getError());
        }
        ShopOrder shopOrder = shopOrderRes.getResult();

        Response<List<ReceiverInfo>> response = receiverInfoReadService.findByOrderId(shopOrder.getId(), OrderLevel.SHOP);
        if(!response.isSuccess()){
            log.error("find order receive info by order id:{} fial,error:{}",shopOrder.getId(),response.getError());
            return Response.fail(response.getError());
        }
        List<ReceiverInfo> receiverInfos = response.getResult();
        if(CollectionUtils.isEmpty(receiverInfos)){
            log.error("not find receive info by order id:{}",shopOrder.getId());
            return Response.fail("order.receive.info.not.exist");
        }

        ShopOrderWithReceiveInfo withReceiveInfo = new ShopOrderWithReceiveInfo();
        withReceiveInfo.setShopOrder(shopOrder);
        withReceiveInfo.setReceiverInfo(receiverInfos.get(0));
        if(log.isDebugEnabled()){
            log.debug("API-ORDER-AFTERSALEORDERINFO-END param: code [{}] ,resp: [{}]",code,JsonMapper.nonEmptyMapper().toJson(withReceiveInfo));
        }
        return Response.ok(withReceiveInfo);
    }

    /**
     * 根据店铺订单id判断是否生成过发货单
     * @param id  店铺订单主键
     * @return
     */
    @RequestMapping(value = "/api/order/{id}/is/shipment/created",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean isShipmentCreated(@PathVariable("id") Long id){
        if(log.isDebugEnabled()){
           log.debug("API-ORDER-ISSHIPMENTCREATED-START param: id [{}] ",id);
        }
        Boolean result = orderReadLogic.isShipmentCreatedForShopOrder(id);
        if(log.isDebugEnabled()){
           log.debug("API-ORDER-ISSHIPMENTCREATED-END param: id [{}] ,resp: [{}]",id,result);
        }
        return result;
    }

    /**
     * 判断待处理,处理中的子单是否有条码没有关联的
     * @param id
     * @return
     */
    @RequestMapping(value = "/api/order/{id}/is/handle/legal",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean isLegalHandleShopOrder(@PathVariable("id") Long id){
        if(log.isDebugEnabled()){
           log.debug("API-ORDER-ISLEGALHANDLESHOPORDER-START param: id [{}] ",id);
        }
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(id,
                MiddleOrderStatus.WAIT_HANDLE.getValue(),MiddleOrderStatus.WAIT_ALL_HANDLE_DONE.getValue());
        int count = 0;
        for (SkuOrder skuOrder:skuOrders){
            if (StringUtils.isEmpty(skuOrder.getSkuCode())){
                count++;
            }
        }
        if(log.isDebugEnabled()){
           log.debug("API-ORDER-ISLEGALHANDLESHOPORDER-END param: id [{}] ,resp: [{}]",id,count);
        }
        return count <= 0;

    }


    /**
     * 根据店铺订单id获取所关联的发货单id(不包括已取消)
     * @param shopOrderId 店铺订单id
     * @return
     */
    @RequestMapping(value = "/api/order/shipment/{id}/list",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<List<Long>> findShipmentIdsByShopOrderId(@PathVariable("id")Long shopOrderId){
        if(log.isDebugEnabled()){
            log.debug("API-ORDER-FINDSHIPMENTIDSBYSHOPORDERID-START param: shopOrderId [{}] ",shopOrderId);
        }
        List<OrderShipment> orderShipments = shipmentReadLogic.findByOrderIdAndType(shopOrderId);
        List<Long> shipmentIds = orderShipments.stream().filter(Objects::nonNull).
                filter(it->!Objects.equals(it.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()) && !Objects.equals(it.getStatus(),MiddleShipmentsStatus.REJECTED.getValue())).map(OrderShipment::getShipmentId).collect(Collectors.toList());;
        if(log.isDebugEnabled()){
            log.debug("API-ORDER-FINDSHIPMENTIDSBYSHOPORDERID-END param: shopOrderId [{}] ,resp: [{}]",shopOrderId,shipmentIds);
        }
        return  Response.ok(shipmentIds);
    }

    private void sendLogForTaobao(Response<OrderDetail> response, HttpServletRequest request) {
        if (!response.isSuccess()) {
            return;
        }
        ShopOrder shopOrder = response.getResult().getShopOrder();
        if (OpenClientChannel.from(shopOrder.getOutFrom()) == OpenClientChannel.TAOBAO) {
            eventBus.post(new OrderOpEvent(request, UserUtil.getCurrentUser(), shopOrder, "查看订单"));
        }
    }
 }
