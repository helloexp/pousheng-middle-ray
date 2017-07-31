package com.pousheng.middle.web.order;

import com.google.common.collect.Lists;
import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.component.RefundWriteLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.sync.ecp.SyncRefundToEcpLogic;
import com.pousheng.middle.web.order.sync.hk.SyncRefundLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.RefundCriteria;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by songrenfei on 2017/6/26
 */
@RestController
@Slf4j
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
    private SyncRefundToEcpLogic syncRefundToEcpLogic;


    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();


    //逆向单分页
    @RequestMapping(value = "/api/refund/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<RefundPaging> findBy(RefundCriteria criteria) {
        if(criteria.getRefundEndAt()!=null){
            criteria.setRefundEndAt(new DateTime(criteria.getRefundEndAt().getTime()).plusDays(1).minusSeconds(1).toDate());
        }
        Response<Paging<RefundPaging>> pagingRes = refundReadLogic.refundPaging(criteria);
        if(!pagingRes.isSuccess()){
            log.error("paging refund by criteria:{} fail,error:{}",criteria,pagingRes.getError());
            throw new JsonResponseException(pagingRes.getError());
        }

        return pagingRes.getResult();
    }


    //逆向单详情
    @RequestMapping(value = "/api/refund/{id}/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public MiddleRefundDetail detail(@PathVariable(value = "id") Long refundId) {
        return makeRefundDetail(refundId);
    }

    //完善处理逆向单
    @RequestMapping(value = "/api/refund/{id}/handle", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public void completeHandle(@PathVariable(value = "id") Long refundId,@RequestBody EditSubmitRefundInfo editSubmitRefundInfo) {
        Refund refund = refundReadLogic.findRefundById(refundId);
        refundWriteLogic.completeHandle(refund,editSubmitRefundInfo);
    }

    //编辑逆向单 或 创建逆向订单
    @RequestMapping(value = "/api/refund/edit-or-create", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public EditMiddleRefund edit(@RequestParam(required = false) Long refundId) {
        if(Arguments.isNull(refundId)){
            EditMiddleRefund editMiddleRefund = new EditMiddleRefund();
            editMiddleRefund.setIsToCreate(Boolean.TRUE);
            return editMiddleRefund;
        }
        return makeEditMiddleRefund(refundId);
    }



    //删除逆向单
    @RequestMapping(value = "/api/refund/{id}/delete", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public void delete(@PathVariable(value = "id") Long refundId) {

        Refund refund = refundReadLogic.findRefundById(refundId);
        refundWriteLogic.deleteRefund(refund);
    }





    /**
     * 创建逆向单
     * @param submitRefundInfo 提交信息
     * @return 逆向单id
     */
    @RequestMapping(value = "/api/refund/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long createRefund(@RequestBody SubmitRefundInfo submitRefundInfo){

        return refundWriteLogic.createRefund(submitRefundInfo);
    }


    /**
     * 换货单待发货商品列表 for 手动生成发货单流程的选择仓库页面
     * @param refundId 换货单id
     * @return 待发货商品列表 注意：待发货数量(waitHandleNumber) = 退货数量 - 已发货数量
     */
    @RequestMapping(value = "/api/refund/{id}/wait/handle/sku", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<WaitShipItemInfo> refundWaitHandleSku(@PathVariable("id") Long refundId) {

        Refund refund = refundReadLogic.findRefundById(refundId);
        List<RefundItem> refundChangeItems = refundReadLogic.findRefundChangeItems(refund);

        List<WaitShipItemInfo> waitShipItemInfos = Lists.newArrayListWithCapacity(refundChangeItems.size());
        for (RefundItem refundItem : refundChangeItems){
            WaitShipItemInfo waitShipItemInfo = new WaitShipItemInfo();
            waitShipItemInfo.setSkuCode(refundItem.getSkuCode());
            waitShipItemInfo.setOutSkuCode(refundItem.getSkuCode());
            waitShipItemInfo.setSkuName(refundItem.getSkuName());
            waitShipItemInfo.setWaitHandleNumber(refundItem.getApplyQuantity()-refundItem.getAlreadyHandleNumber());
            waitShipItemInfos.add(waitShipItemInfo);
        }
        return waitShipItemInfos;
    }



    /**
     * 同步售后单到恒康
     * @param refundId 售后单id
     */
    @RequestMapping(value = "api/refund/{id}/sync/hk",method = RequestMethod.PUT)
    public void syncHkRefund(@PathVariable(value = "id") Long refundId){
        Refund refund = refundReadLogic.findRefundById(refundId);
        Response<Boolean> syncRes = syncRefundLogic.syncRefundToHk(refund);
        if(!syncRes.isSuccess()){
            log.error("sync refund(id:{}) to hk fail,error:{}",refundId,syncRes.getError());
            throw new JsonResponseException(syncRes.getError());
        }
    }


    /**
     * 取消售后单
     * @param refundId 售后单id
     */
    @RequestMapping(value = "api/refund/{id}/cancel",method = RequestMethod.PUT)
    public void cancleRefund(@PathVariable(value = "id") Long refundId){
        Refund refund = refundReadLogic.findRefundById(refundId);
        Response<Boolean> cancelRes = refundWriteLogic.updateStatus(refund, MiddleOrderEvent.CANCEL.toOrderOperation());
        if(!cancelRes.isSuccess()){
            log.error("cancel refund(id:{}) fail,error:{}",refundId,cancelRes.getError());
            throw new JsonResponseException(cancelRes.getError());
        }
        //回滚发货单的数量
        refundWriteLogic.rollbackRefundQuantities(refund);
    }


    /**
     * 同步售后单取消状态到恒康
     * @param refundId 售后单id
     */
    @RequestMapping(value = "api/refund/{id}/cancel/sync/hk",method = RequestMethod.PUT)
    public void syncHkCancelRefund(@PathVariable(value = "id") Long refundId){
        Refund refund = refundReadLogic.findRefundById(refundId);
        Response<Boolean> syncRes = syncRefundLogic.syncRefundCancelToHk(refund);
        if(!syncRes.isSuccess()){
            log.error("sync cancel refund(id:{}) to hk fail,error:{}",refundId,syncRes.getError());
            throw new JsonResponseException(syncRes.getError());
        }
        //回滚发货单的数量
        refundWriteLogic.rollbackRefundQuantities(refund);
    }

    /**
     *恒康同步信息到中台提示已经退货完成,此时调用该接口通知电商退款
     * @param refundId
     */
    @RequestMapping(value = "api/refund/{id}/sync/ecp",method = RequestMethod.PUT)
    public void syncECPRefund(@PathVariable(value = "id") Long refundId)
    {
        Refund refund = refundReadLogic.findRefundById(refundId);
        Response<Boolean> syncRes = syncRefundToEcpLogic.syncRefundToECP(refund);
        if(!syncRes.isSuccess()){
            log.error("sync cancel refund(id:{}) to ecp fail,error:{}",refundId,syncRes.getError());
            throw new JsonResponseException(syncRes.getError());
        }
    }



    /**
     * 运营确认收货 （换货）
     * @param refundId 售后单id
     */
    @RequestMapping(value = "api/refund/{id}/confirm/received",method = RequestMethod.PUT)
    public void confirmReceived(@PathVariable(value = "id") Long refundId){
        Refund refund = refundReadLogic.findRefundById(refundId);
        Response<Boolean> cancelRes = refundWriteLogic.updateStatus(refund, MiddleOrderEvent.CONFIRM.toOrderOperation());
        if(!cancelRes.isSuccess()){
            log.error("confirm refund(id:{}) fail,error:{}",refundId,cancelRes.getError());
            throw new JsonResponseException(cancelRes.getError());
        }
    }




    private MiddleRefundDetail makeRefundDetail(Long refundId){

        Refund refund = refundReadLogic.findRefundById(refundId);
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refundId);
        MiddleRefundDetail refundDetail = new MiddleRefundDetail();
        refundDetail.setOrderRefund(orderRefund);
        refundDetail.setRefund(refund);
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        refundDetail.setRefundItems(refundReadLogic.findRefundItems(refund));
        refundDetail.setRefundExtra(refundExtra);

        //如果为换货,则获取换货商品信息
        if(isChangeRefund(refund)){
            refundDetail.setShipmentItems(refundReadLogic.findRefundChangeItems(refund));
        }

        //如果为换货,切已经生成过发货单，则封装发货信息（换货的发货单）
        if(isChangeRefund(refund)&& refund.getStatus()> MiddleRefundStatus.WAIT_SHIP.getValue()){
            refundDetail.setOrderShipments(shipmentReadLogic.findByAfterOrderIdAndType(refundId));
        }

        return  refundDetail;

    }

    private EditMiddleRefund makeEditMiddleRefund(Long refundId){

        Refund refund = refundReadLogic.findRefundById(refundId);
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refundId);
        EditMiddleRefund editMiddleRefund = new EditMiddleRefund();
        editMiddleRefund.setIsToCreate(Boolean.FALSE);
        editMiddleRefund.setOrderRefund(orderRefund);
        editMiddleRefund.setRefund(refund);
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        editMiddleRefund.setRefundItems(makeEditRefundItemFromRefund(refund,refundExtra.getShipmentId()));
        editMiddleRefund.setRefundExtra(refundExtra);

        //如果为换货,则获取换货商品信息
        if(isChangeRefund(refund)){
            editMiddleRefund.setShipmentItems(refundReadLogic.findRefundChangeItems(refund));
        }

        return  editMiddleRefund;

    }

    //根据退货商品封装 EditRefundItem
    private List<EditRefundItem> makeEditRefundItemFromRefund(Refund refund,Long shipmentId){
        List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);

        Map<String, ShipmentItem> shipmentItemMap = shipmentItems.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(ShipmentItem::getSkuCode, it -> it));

        List<EditRefundItem> editRefundItems = Lists.newArrayListWithCapacity(shipmentItems.size());
        for (RefundItem refundItem : refundItems){
            EditRefundItem editRefundItem = new EditRefundItem();
            BeanMapper.copy(refundItem,editRefundItem);
            ShipmentItem shipmentItem = shipmentItemMap.get(refundItem.getSkuCode());
            editRefundItem.setQuantity(shipmentItem.getQuantity());
            editRefundItem.setRefundQuantity(shipmentItem.getRefundQuantity());
            editRefundItems.add(editRefundItem);
        }

        return editRefundItems;
    }


    private Warehouse findWarehouseById(Long warehouseId){

        Response<Warehouse> warehouseRes = warehouseReadService.findById(warehouseId);
        if(!warehouseRes.isSuccess()){
            log.error("find warehouse by id:{} fail,error:{}",warehouseId,warehouseRes.getError());
            throw new JsonResponseException(warehouseRes.getError());
        }
        return warehouseRes.getResult();

    }

    private Boolean isChangeRefund(Refund refund){
        return Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value());
    }
}
