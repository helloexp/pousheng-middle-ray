package com.pousheng.middle.web.order;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.order.service.MiddleRefundWriteService;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.ecp.SyncRefundToEcpLogic;
import com.pousheng.middle.web.order.sync.hk.SyncRefundLogic;
import com.pousheng.middle.web.order.sync.yyedi.SyncYYEdiReturnLogic;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.utils.operationlog.OperationLogParam;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import com.pousheng.middle.web.utils.permission.PermissionCheck;
import com.pousheng.middle.web.utils.permission.PermissionCheckParam;
import com.pousheng.middle.web.utils.permission.PermissionUtil;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.JsonMapper;
import io.terminus.common.utils.Splitters;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by songrenfei on 2017/6/26
 */
@RestController
@Slf4j
@OperationLogModule(OperationLogModule.Module.REFUND)
@PermissionCheck(PermissionCheck.PermissionCheckType.REFUND)
public class Refunds {

    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private WarehouseReadService warehouseReadService;
    @Autowired
    private SyncRefundLogic syncRefundLogic;
    @Autowired
    private SyncYYEdiReturnLogic syncYYEdiReturnLogic;
    @Autowired
    private SyncRefundToEcpLogic syncRefundToEcpLogic;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @Autowired
    private PermissionUtil permissionUtil;
    @Autowired
    private MiddleRefundWriteService middleRefundWriteService;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();


    //逆向单分页
    @RequestMapping(value = "/api/refund/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<RefundPaging> findBy(MiddleRefundCriteria criteria) {
        if (criteria.getRefundEndAt() != null) {
            criteria.setRefundEndAt(new DateTime(criteria.getRefundEndAt().getTime()).plusDays(1).minusSeconds(1).toDate());
        }
        criteria.setExcludeRefundType(MiddleRefundType.ON_SALES_REFUND.value());

        List<Long> currentUserCanOperateShopIds = permissionUtil.getCurrentUserCanOperateShopIDs();
        if (criteria.getShopId() == null) {
            criteria.setShopIds(currentUserCanOperateShopIds);
        } else if (!currentUserCanOperateShopIds.contains(criteria.getShopId())) {
            throw new JsonResponseException("permission.check.query.deny");
        }


        Response<Paging<RefundPaging>> pagingRes = refundReadLogic.refundPaging(criteria);
        if (!pagingRes.isSuccess()) {
            log.error("paging refund by criteria:{} fail,error:{}", criteria, pagingRes.getError());
            throw new JsonResponseException(pagingRes.getError());
        }

        return pagingRes.getResult();
    }


    //逆向单详情
    @RequestMapping(value = "/api/refund/{id}/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public MiddleRefundDetail detail(@PathVariable(value = "id") @PermissionCheckParam Long refundId) {
        return makeRefundDetail(refundId);
    }

    //完善处理逆向单
    @RequestMapping(value = "/api/refund/{id}/handle", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("完善或提交售后单")
    public void completeHandle(@PathVariable(value = "id") @PermissionCheckParam @OperationLogParam Long refundId, @RequestBody EditSubmitRefundInfo editSubmitRefundInfo) {
        Refund refund = refundReadLogic.findRefundById(refundId);
        try{
            if (Objects.equals(refund.getRefundType(),MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value())){
                refundWriteLogic.completeHandleForLostType(refund,editSubmitRefundInfo);
            }else{
                refundWriteLogic.completeHandle(refund, editSubmitRefundInfo);
                if (Objects.equals(editSubmitRefundInfo.getOperationType(),2)) {
                    //完善之后同步售后单到恒康
                    Flow flow = flowPicker.pickAfterSales();
                    Integer targetStatus = flow.target(refund.getStatus(),MiddleOrderEvent.HANDLE.toOrderOperation());
                    refund.setStatus(targetStatus);
                    Response<Boolean> syncRes = syncYYEdiReturnLogic.syncRefundToYYEdi(refund);
                    if (!syncRes.isSuccess()) {
                        log.error("sync refund(id:{}) to hk fail,error:{}", refundId, syncRes.getError());
                    }
                }
            }
        }catch (JsonResponseException e1){
            throw new JsonResponseException(e1.getMessage());
        }catch (Exception e){
            throw new JsonResponseException("complete.refund.failed");
        }
    }


    /**
     * 批量标记逆向单为已处理
     *
     * @param data 逗号隔开的逆向单id拼接
     */
    @RequestMapping(value = "/api/refund/batch/handle", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("批量处理售后单")
    public void completeHandle(@RequestParam(value = "refundIds") String data) {
        List<Long> refundIds = Splitters.splitToLong(data, Splitters.COMMA);
        List<Refund> refunds = refundReadLogic.findRefundByIds(refundIds);
        if (!Objects.equals(refundIds.size(), refunds.size())) {
            log.error("find refund by refund ids:{} result size not equal request id size:{}", refundIds, refunds.size(), refundIds.size());
            throw new JsonResponseException("refund.id.invalid");
        }
        int count=0;
        for (Refund refund:refunds){
            Map<String,String> refundExtraMap = refund.getExtra();
            if (!refundExtraMap.containsKey(TradeConstants.MIDDLE_REFUND_COMPLETE_FLAG)){
                count++;
            }
        }
        if (count>0){
            throw new JsonResponseException("uncomplete.refund.can.not.pass.check");
        }
        refunds.forEach(refund -> {
            if (Objects.equals(refund.getRefundType(),MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value())){
                OrderOperation orderOperation = MiddleOrderEvent.LOST_HANDLE.toOrderOperation();
                Response<Boolean> response = refundWriteLogic.updateStatus(refund, orderOperation);
                if (!response.isSuccess()) {
                    log.error("refund(id:{}) operation:{} fail", refund.getId(), orderOperation);
                    throw new JsonResponseException(response.getError());
                }
            }else{
                OrderOperation orderOperation = MiddleOrderEvent.HANDLE.toOrderOperation();
                Response<Boolean> response = refundWriteLogic.updateStatus(refund, orderOperation);
                if (!response.isSuccess()) {
                    log.error("refund(id:{}) operation:{} fail", refund.getId(), orderOperation);
                    throw new JsonResponseException(response.getError());
                }else{
                    //审核之后同步售后单到恒康
                    Flow flow = flowPicker.pickAfterSales();
                    Integer targetStatus = flow.target(refund.getStatus(),MiddleOrderEvent.HANDLE.toOrderOperation());
                    refund.setStatus(targetStatus);
                    Response<Boolean> syncRes = syncRefundLogic.syncRefundToHk(refund);
                    if (!syncRes.isSuccess()) {
                        log.error("sync refund(id:{}) to hk fail,error:{}", refund.getId(), syncRes.getError());
                    }
                }
            }
        });
    }


    //编辑逆向单 或 创建逆向订单
    @RequestMapping(value = "/api/refund/edit-or-create", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public EditMiddleRefund edit(@RequestParam(required = false) @PermissionCheckParam Long refundId) {
        if (Arguments.isNull(refundId)) {
            EditMiddleRefund editMiddleRefund = new EditMiddleRefund();
            editMiddleRefund.setIsToCreate(Boolean.TRUE);
            return editMiddleRefund;
        }
        return makeEditMiddleRefund(refundId);
    }


    //删除逆向单
    @RequestMapping(value = "/api/refund/{id}/delete", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("删除售后单")
    public void delete(@PathVariable(value = "id")@OperationLogParam @PermissionCheckParam Long refundId) {

        Refund refund = refundReadLogic.findRefundById(refundId);
        if (Objects.equals(refund.getRefundType(),MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value())){
            refundWriteLogic.deleteRefundForLost(refund);
        }else{
            refundWriteLogic.deleteRefund(refund);
        }
    }


    /**
     * 创建逆向单
     *
     * @param submitRefundInfo 提交信息
     * @return 逆向单id
     */
    @RequestMapping(value = "/api/refund/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PermissionCheck(PermissionCheck.PermissionCheckType.SHOP_ORDER)
    @OperationLogType("创建售后单")
    public Long createRefund(@RequestBody @PermissionCheckParam("orderId") SubmitRefundInfo submitRefundInfo) {
        if (Objects.equals(submitRefundInfo.getRefundType(),MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value())){
            return refundWriteLogic.createRefundForLost(submitRefundInfo);
        }else{
            return refundWriteLogic.createRefund(submitRefundInfo);
        }
    }


    /**
     * 换货单待发货商品列表 for 手动生成发货单流程的选择仓库页面
     *
     * @param refundId 换货单id
     * @return 待发货商品列表 注意：待发货数量(waitHandleNumber) = 退货数量 - 已发货数量
     */
    @RequestMapping(value = "/api/refund/{id}/wait/handle/sku", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<WaitShipItemInfo> refundWaitHandleSku(@PathVariable("id") @PermissionCheckParam Long refundId) {

        Refund refund = refundReadLogic.findRefundById(refundId);
        if (Objects.equals(refund.getRefundType(),MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value())){
            List<RefundItem> refundLostItems = refundReadLogic.findRefundLostItems(refund);
            List<WaitShipItemInfo> waitShipItemInfos = Lists.newArrayListWithCapacity(refundLostItems.size());
            for (RefundItem refundItem : refundLostItems) {
                if((refundItem.getApplyQuantity()-(refundItem.getAlreadyHandleNumber()==null?0:refundItem.getAlreadyHandleNumber()))>0){
                    this.waitShipItems(waitShipItemInfos, refundItem);
                }
            }
            return waitShipItemInfos;
        }else{
            List<RefundItem> refundChangeItems = refundReadLogic.findRefundChangeItems(refund);
            List<WaitShipItemInfo> waitShipItemInfos = Lists.newArrayListWithCapacity(refundChangeItems.size());
            for (RefundItem refundItem : refundChangeItems) {
                if((refundItem.getApplyQuantity()-(refundItem.getAlreadyHandleNumber()==null?0:refundItem.getAlreadyHandleNumber()))>0){
                    this.waitShipItems(waitShipItemInfos, refundItem);
                }
            }
            return waitShipItemInfos;
        }
    }

    private void waitShipItems(List<WaitShipItemInfo> waitShipItemInfos, RefundItem refundItem) {
        WaitShipItemInfo waitShipItemInfo = new WaitShipItemInfo();
        waitShipItemInfo.setSkuCode(refundItem.getSkuCode());
        waitShipItemInfo.setOutSkuCode(refundItem.getSkuCode());
        waitShipItemInfo.setSkuName(refundItem.getSkuName());
        waitShipItemInfo.setWaitHandleNumber(refundItem.getApplyQuantity()-(refundItem.getAlreadyHandleNumber()==null?0:refundItem.getAlreadyHandleNumber()));
        waitShipItemInfo.setSkuAttrs(refundItem.getAttrs());
        waitShipItemInfo.setItemId(refundItem.getItemId());
        waitShipItemInfos.add(waitShipItemInfo);
    }


    /**
     * 同步售后单到恒康
     *
     * @param refundId 售后单id
     */
    @RequestMapping(value = "api/refund/{id}/sync/hk", method = RequestMethod.PUT)
    @OperationLogType("同步售后单到恒康")
    public void syncHkRefund(@PathVariable(value = "id") @PermissionCheckParam Long refundId) {
        Refund refund = refundReadLogic.findRefundById(refundId);
        Response<Boolean> syncRes = syncRefundLogic.syncRefundToHk(refund);
        if (!syncRes.isSuccess()) {
            log.error("sync refund(id:{}) to hk fail,error:{}", refundId, syncRes.getError());
            throw new JsonResponseException(syncRes.getError());
        }
    }


    /**
     * 取消售后单
     *
     * @param refundId 售后单id
     */
    @RequestMapping(value = "api/refund/{id}/cancel", method = RequestMethod.PUT)
    @OperationLogType("取消销售单")
    public void cancleRefund(@PathVariable(value = "id") @PermissionCheckParam @OperationLogParam Long refundId) {
        Refund refund = refundReadLogic.findRefundById(refundId);
        Response<Boolean> cancelRes = refundWriteLogic.updateStatus(refund, MiddleOrderEvent.CANCEL.toOrderOperation());
        if (!cancelRes.isSuccess()) {
            log.error("cancel refund(id:{}) fail,error:{}", refundId, cancelRes.getError());
            throw new JsonResponseException(cancelRes.getError());
        }
        //回滚发货单的数量
        if (!Objects.equals(refund.getRefundType(),MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value())){
            refundWriteLogic.rollbackRefundQuantities(refund);
        }
    }


    /**
     * 同步售后单取消状态到恒康
     *
     * @param refundId 售后单id
     */
    @RequestMapping(value = "api/refund/{id}/cancel/sync/hk", method = RequestMethod.PUT)
    @OperationLogType("同步售后单取消状态")
    public void syncHkCancelRefund(@PathVariable(value = "id") @PermissionCheckParam Long refundId) {
        Refund refund = refundReadLogic.findRefundById(refundId);
        //如果是之前同步恒康失败的，不用和恒康连接直接取消失败
        if (Objects.equals(refund.getStatus(),MiddleRefundStatus.SYNC_HK_FAIL.getValue())){
            OrderOperation syncSuccessOrderOperation = MiddleOrderEvent.CANCEL_HK.toOrderOperation();
            Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatus(refund, syncSuccessOrderOperation);
            if (!updateSyncStatusRes.isSuccess()) {
                log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), syncSuccessOrderOperation.getText(), updateSyncStatusRes.getError());
                throw new JsonResponseException(updateSyncStatusRes.getError());
            }
        }else{
                Response<Boolean> syncRes = syncRefundLogic.syncRefundCancelToHk(refund);
            if (!syncRes.isSuccess()) {
                log.error("sync cancel refund(id:{}) to hk fail,error:{}", refundId, syncRes.getError());
                throw new JsonResponseException(syncRes.getError());
            }
        }
        //回滚发货单的数量
        refundWriteLogic.rollbackRefundQuantities(refund);
    }

    /**
     * 恒康同步信息到中台提示已经退货完成,此时调用该接口通知电商退款
     *
     * @param refundId
     */
    @RequestMapping(value = "api/refund/{id}/sync/ecp", method = RequestMethod.PUT)
    @OperationLogType("通知电商退款")
    public void syncECPRefund(@PathVariable(value = "id") @PermissionCheckParam Long refundId) {
        Refund refund = refundReadLogic.findRefundById(refundId);
        Response<Boolean> syncRes = syncRefundToEcpLogic.syncRefundToECP(refund);
        if (!syncRes.isSuccess()) {
            log.error("sync cancel refund(id:{}) to ecp fail,error:{}", refundId, syncRes.getError());
            throw new JsonResponseException(syncRes.getError());
        }
    }


    /**
     * 运营确认收货 （换货）
     *
     * @param refundId 售后单id
     */
    @RequestMapping(value = "api/refund/{id}/confirm/received", method = RequestMethod.PUT)
    @OperationLogType("运营商确认收货（换货）")
    public void confirmReceived(@PathVariable(value = "id") @PermissionCheckParam Long refundId) {
        Refund refund = refundReadLogic.findRefundById(refundId);
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        //塞入确认收货时间
        refundExtra.setConfirmReceivedAt(new Date());
        Map<String, String> extraMap = refund.getExtra() != null ? refund.getExtra() : Maps.newHashMap();
        extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
        refund.setExtra(extraMap);
        Response<Boolean> cancelRes = refundWriteLogic.update(refund);
        if (!cancelRes.isSuccess()) {
            log.error("confirm refund(id:{}) fail,error:{}", refundId, cancelRes.getError());
            throw new JsonResponseException(cancelRes.getError());
        }
        Response<Boolean> cancelRes1 = refundWriteLogic.updateStatus(refund,MiddleOrderEvent.CONFIRM.toOrderOperation());
        if (!cancelRes1.isSuccess()) {
            log.error("confirm refund(id:{}) fail,error:{}", refundId, cancelRes1.getError());
            throw new JsonResponseException(cancelRes1.getError());
        }
    }

    /**
     * 添加中台客服备注,各个状态均可添加
     *
     * @param id                  店铺订单主键
     * @param customerSerivceNote 客服备注
     */
    @RequestMapping(value = "/api/refund/{id}/add/customer/service/note", method = RequestMethod.PUT)
    @OperationLogType("售后单添加中台客服备注")
    public void createCustomerServiceNote(@PathVariable("id") @OperationLogParam Long id, @RequestParam("customerSerivceNote") String customerSerivceNote) {
        refundWriteLogic.addCustomerServiceNote(id, customerSerivceNote);
    }

    /**
     * 换货改退货取消售后单,必须保证这个状态已经收货
     * @param id 售后单id
     */
    @RequestMapping(value = "/api/refund/{id}/cancel/on/change",method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("换货改退货取消售后单")
    public void cancelRefundForChange(@PathVariable("id")@OperationLogParam Long id){
        Refund refund = refundReadLogic.findRefundById(id);
        if (refundReadLogic.isAfterSaleCanCancelShip(refund)){
            //如果允许取消发货则修改状态
            refundWriteLogic.updateStatus(refund,MiddleOrderEvent.AFTER_SALE_CANCEL_SHIP.toOrderOperation());
            Flow flow = flowPicker.pickAfterSales();
            Integer targetStatus = flow.target(refund.getStatus(),MiddleOrderEvent.AFTER_SALE_CANCEL_SHIP.toOrderOperation());
            RefundExtra refundExtra  = refundReadLogic.findRefundExtra(refund);
            refundExtra.setCancelShip("true");
            Map<String,String> extraMap =refund.getExtra();
            extraMap.put(TradeConstants.REFUND_EXTRA_INFO,mapper.toJson(refundExtra));
            refund.setStatus(targetStatus);
            refund.setExtra(extraMap);
            refundWriteLogic.update(refund);

        }else{
            throw new JsonResponseException("after.sale.cancel.shipment.status.invalid");
        }
    }

    /**
     * 人工确认已经退款
     * @param refundId 退款单id
     */
    @RequestMapping(value = "/api/refund/{id}/manual/confirm/refund",method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("人工确认已经退款")
    public void confirmRefund(@PathVariable("id")@OperationLogParam Long refundId){
       Refund refund =  refundReadLogic.findRefundById(refundId);
       OrderRefund orderRefund =  refundReadLogic.findOrderRefundByRefundId(refundId);
       ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderRefund.getOrderId());
       if (!Objects.equals(shopOrder.getOutFrom(), MiddleChannel.JD.getValue())){
           throw new JsonResponseException("only.channel.jd.can.manual.confirm.refund");
       }
       Integer sourceStatus = refund.getStatus();
       Flow flow = flowPicker.pickAfterSales();
       if (!flow.operationAllowed(sourceStatus, MiddleOrderEvent.REFUND.toOrderOperation())){
           log.error("refund(id:{}) current status:{} not allow operation:{}", refund.getId(), refund.getStatus(), MiddleOrderEvent.REFUND.toOrderOperation().getText());
           throw new JsonResponseException("order.status.invalid");
       }
       Response<Boolean> updateStatusRes = refundWriteLogic.updateStatus(refund,MiddleOrderEvent.REFUND.toOrderOperation());
       if(!updateStatusRes.isSuccess()){
            log.error("refund(id:{}) operation :{} fail,error:{}",refund.getId(),MiddleOrderEvent.REFUND.toOrderOperation().getText(),updateStatusRes.getError());
            throw new JsonResponseException("update.refund.error");
       }
    }

    /**
     * 人工确认已经退货
     * @param refundId 售后单id
     */
    @RequestMapping(value = "/api/refund/{id}/manual/confirm/return",method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("人工确认已经退货")
    public void confirmReturn(@PathVariable("id") @OperationLogParam Long refundId){
        Refund refund =  refundReadLogic.findRefundById(refundId);
        OrderRefund orderRefund =  refundReadLogic.findOrderRefundByRefundId(refundId);
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderRefund.getOrderId());
        if (!Objects.equals(shopOrder.getOutFrom(), MiddleChannel.JD.getValue())){
            throw new JsonResponseException("only.channel.jd.can.manual.confirm.return");
        }
        Integer sourceStatus = refund.getStatus();
        Flow flow = flowPicker.pickAfterSales();
        if (!flow.operationAllowed(sourceStatus, MiddleOrderEvent.RETURN.toOrderOperation())){
            log.error("refund(id:{}) current status:{} not allow operation:{}", refund.getId(), refund.getStatus(), MiddleOrderEvent.RETURN.toOrderOperation().getText());
            throw new JsonResponseException("order.status.invalid");
        }
        Response<Boolean> updateStatusRes = refundWriteLogic.updateStatus(refund,MiddleOrderEvent.RETURN.toOrderOperation());
        if(!updateStatusRes.isSuccess()){
            log.error("refund(id:{}) operation :{} fail,error:{}",refund.getId(),MiddleOrderEvent.RETURN.toOrderOperation().getText(),updateStatusRes.getError());
            throw new JsonResponseException("update.refund.error");
        }
    }

    /**
     * 判断售后单来源是否是京东
     * @param refundId 售后单id
     * @return true:订单来源是京东，false:订单来源非京东
     */
    @RequestMapping(value = "/api/refund/{id}/is/out/from/jd",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean isOutFromJD(@PathVariable("id") Long refundId){
        OrderRefund orderRefund =  refundReadLogic.findOrderRefundByRefundId(refundId);
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderRefund.getOrderId());
        return Objects.equals(shopOrder.getOutFrom(), MiddleChannel.JD.getValue());
    }

    /**
     * 计算最多可退金额
     * @param orderId 订单主键
     * @param shipmentId 发货单主键
     * @param refundId 退货单主键
     * @param  list skuCode-applyQuantity 集合
     * @return
     */
    @RequestMapping(value = "/api/refund/{id}/already/refund/fee",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public int getAlreadyRefundFee(@PathVariable("id") Long orderId,@RequestParam("shipmentId") Long shipmentId,@RequestParam(required = false) Long refundId,
                                   @RequestParam(value = "list") String list){
        List<RefundFeeData> refundFeeDatas = JsonMapper.nonEmptyMapper().fromJson(list, JsonMapper.nonEmptyMapper().createCollectionType(List.class,RefundFeeData.class));
        return refundReadLogic.getAlreadyRefundFee(orderId,refundId,shipmentId,refundFeeDatas);
    }

    private MiddleRefundDetail makeRefundDetail(Long refundId) {

        Refund refund = refundReadLogic.findRefundById(refundId);
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refundId);
        MiddleRefundDetail refundDetail = new MiddleRefundDetail();
        refundDetail.setOrderRefund(orderRefund);
        refundDetail.setRefund(refund);
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        refundDetail.setRefundItems(refundReadLogic.findRefundItems(refund));
        refundDetail.setRefundExtra(refundExtra);
        //如果为丢件补发，获取丢件补发的商品信息
        if (isLostRefund(refund)){
            refundDetail.setLostRefundItems(refundReadLogic.findRefundLostItems(refund));
            //获取换货的收货人地址
            refundDetail.setMiddleChangeReceiveInfo(refundReadLogic.findMiddleChangeReceiveInfo(refund));
        }
        //如果为换货,则获取换货商品信息
        if (isChangeRefund(refund)) {
            refundDetail.setShipmentItems(refundReadLogic.findRefundChangeItems(refund));
            //获取换货的收货人地址
            refundDetail.setMiddleChangeReceiveInfo(refundReadLogic.findMiddleChangeReceiveInfo(refund));
        }

        //如果为换货,切已经生成过发货单，则封装发货信息（换货的发货单）
        if (isChangeRefund(refund) && refund.getStatus() >= MiddleRefundStatus.WAIT_SHIP.getValue()) {
            refundDetail.setOrderShipments(shipmentReadLogic.findByAfterOrderIdAndType(refundId));
        }
        //添加可用操作类型
        Flow flow = flowPicker.pickAfterSales();
        Set<OrderOperation> operations = flow.availableOperations(refund.getStatus());
        refundDetail.setOperations(operations);
        return refundDetail;

    }

    /**
     * 修改订单的收货信息，主要是换货使用
     * @param id 售后单主键
     * @param middleChangeReceiveInfo
     */
    @RequestMapping(value = "/api/refund/{id}/edit/receiver/info",method = RequestMethod.PUT)
    @OperationLogType("修改换货售后单客户收货地址")
    public void editReceiverInfos(@PathVariable("id")@OperationLogParam Long id, @RequestParam(required = false) String buyerName,@RequestBody MiddleChangeReceiveInfo middleChangeReceiveInfo){
        middleRefundWriteService.updateReceiveInfos(id,middleChangeReceiveInfo);
    }



    private EditMiddleRefund makeEditMiddleRefund(Long refundId) {

        Refund refund = refundReadLogic.findRefundById(refundId);
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refundId);
        EditMiddleRefund editMiddleRefund = new EditMiddleRefund();
        editMiddleRefund.setIsToCreate(Boolean.FALSE);
        editMiddleRefund.setOrderRefund(orderRefund);
        editMiddleRefund.setRefund(refund);
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        if (refundExtra.getShipmentId()!=null){
            editMiddleRefund.setRefundItems(makeEditRefundItemFromRefund(refund, refundExtra.getShipmentId()));
        }
        editMiddleRefund.setRefundExtra(refundExtra);
        //如果为丢件补发类型
        if(isLostRefund(refund)){
            editMiddleRefund.setLostRefundItems(refundReadLogic.findRefundLostItems(refund));
            editMiddleRefund.setMiddleChangeReceiveInfo(refundReadLogic.findMiddleChangeReceiveInfo(refund));
        }
        //如果为换货,则获取换货商品信息
        if (isChangeRefund(refund)) {
            editMiddleRefund.setShipmentItems(refundReadLogic.findRefundChangeItems(refund));
            editMiddleRefund.setMiddleChangeReceiveInfo(refundReadLogic.findMiddleChangeReceiveInfo(refund));
        }

        return editMiddleRefund;

    }

    //根据退货商品封装 EditRefundItem
    private List<EditRefundItem> makeEditRefundItemFromRefund(Refund refund, Long shipmentId) {
        List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);

        Map<String, ShipmentItem> shipmentItemMap = shipmentItems.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(ShipmentItem::getSkuCode, it -> it));

        List<EditRefundItem> editRefundItems = Lists.newArrayListWithCapacity(shipmentItems.size());
        for (RefundItem refundItem : refundItems) {
            EditRefundItem editRefundItem = new EditRefundItem();
            BeanMapper.copy(refundItem, editRefundItem);
            ShipmentItem shipmentItem = shipmentItemMap.get(refundItem.getSkuCode());
            editRefundItem.setQuantity(shipmentItem.getQuantity());
            editRefundItem.setRefundQuantity(shipmentItem.getRefundQuantity());
            //商品id
            editRefundItem.setItemId(shipmentItem.getItemId());
            //商品属性
            editRefundItem.setAttrs(shipmentItem.getAttrs());
            editRefundItems.add(editRefundItem);
        }

        return editRefundItems;
    }


    private Warehouse findWarehouseById(Long warehouseId) {

        Response<Warehouse> warehouseRes = warehouseReadService.findById(warehouseId);
        if (!warehouseRes.isSuccess()) {
            log.error("find warehouse by id:{} fail,error:{}", warehouseId, warehouseRes.getError());
            throw new JsonResponseException(warehouseRes.getError());
        }
        return warehouseRes.getResult();

    }

    /**
     * 是否是换货
     * @param refund
     * @return
     */
    private Boolean isChangeRefund(Refund refund) {
        return Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value());
    }

    /**
     * 是否是丢件补发类型
     * @param refund 售后单
     * @return
     */
    private Boolean isLostRefund(Refund refund) {
        return Objects.equals(refund.getRefundType(), MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value());
    }

    /**
     * 丢件补发类型客服确认客户收货
     * @param id 售后单id
     * @return
     */
    @RequestMapping(value = "/api/refund/{id}/customer/service/confirm/done",method = RequestMethod.PUT)
    @OperationLogType("丢件补发客服确认收货")
    public Response<Boolean> confirmDoneForLost(@PathVariable("id") @OperationLogParam Long id){
        Refund refund = refundReadLogic.findRefundById(id);
        Response<Boolean> r = refundWriteLogic.updateStatus(refund, MiddleOrderEvent.LOST_CONFIRMED.toOrderOperation());
        return Response.ok(r.isSuccess());
    }

}
