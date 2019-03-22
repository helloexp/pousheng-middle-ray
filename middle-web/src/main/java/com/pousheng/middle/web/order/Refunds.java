package com.pousheng.middle.web.order;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dispatch.component.MposSkuStockLogic;
import com.pousheng.middle.order.dto.*;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.*;
import com.pousheng.middle.order.service.MiddleRefundWriteService;
import com.pousheng.middle.warehouse.companent.WarehouseClient;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.ecp.SyncRefundToEcpLogic;
import com.pousheng.middle.web.order.sync.erp.SyncErpReturnLogic;
import com.pousheng.middle.web.order.sync.hk.SyncRefundPosLogic;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.utils.operationlog.OperationLogParam;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import com.pousheng.middle.web.utils.permission.PermissionCheckParam;
import com.pousheng.middle.web.utils.permission.PermissionUtil;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.JsonMapper;
import io.terminus.common.utils.Splitters;
import io.terminus.open.client.center.AfterSaleExchangeServiceRegistryCenter;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.order.dto.OpenClientAfterSale;
import io.terminus.open.client.order.enums.OpenClientAfterSaleStatus;
import io.terminus.open.client.order.service.OpenClientAfterSaleExchangeService;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.common.exception.InvalidException;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.enums.ShipmentOccupyType;
import io.terminus.parana.order.model.*;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by songrenfei on 2017/6/26
 */
@RestController
@Slf4j
@OperationLogModule(OperationLogModule.Module.REFUND)
public class Refunds {

    @Autowired
    private RefundReadLogic refundReadLogic;
    @Autowired
    private RefundWriteLogic refundWriteLogic;
    @Autowired
    private SyncRefundPosLogic syncRefundPosLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private WarehouseClient warehouseClient;
    @Autowired
    private SyncErpReturnLogic syncErpReturnLogic;
    @Autowired
    private SyncShipmentLogic syncShipmentLogic;
    @Autowired
    private SyncRefundToEcpLogic syncRefundToEcpLogic;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @Autowired
    private PermissionUtil permissionUtil;
    @Autowired
    private MiddleRefundWriteService middleRefundWriteService;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private AfterSaleExchangeServiceRegistryCenter afterSaleExchangeServiceRegistryCenter;
    @Autowired
    private MposSkuStockLogic mposSkuStockLogic;
    @RpcConsumer
    private OpenShopReadService openShopReadService;
    @org.springframework.beans.factory.annotation.Value("${skx.open.shop.id}")
    private Long skxOpenShopId;
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();


    //逆向单分页
    @RequestMapping(value = "/api/refund/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<RefundPaging> findBy(MiddleRefundCriteria criteria) {
        String criteriaStr = JsonMapper.nonEmptyMapper().toJson(criteria);
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-PAGING-START param: criteria [{}]", criteriaStr);
        }
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
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-PAGING-END param: criteria [{}] ,resp: [{}]", criteriaStr, JsonMapper.nonEmptyMapper().toJson(pagingRes));
        }
        return pagingRes.getResult();
    }


    //逆向单详情
    @RequestMapping(value = "/api/refund/{id}/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public MiddleRefundDetail detail(@PathVariable(value = "id") @PermissionCheckParam Long refundId) {
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-DETAIL-START param: refundId [{}]", refundId);
        }
        MiddleRefundDetail detail = makeRefundDetail(refundId);
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-DETAIL-END param: refundId [{}] ,resp: [{}]", refundId, JsonMapper.nonEmptyMapper().toJson(detail));
        }
        return detail;
    }

    //编辑逆向单 或 创建逆向订单
    @RequestMapping(value = "/api/refund/edit-or-create", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public EditMiddleRefund edit(@RequestParam(required = false) @PermissionCheckParam Long refundId) {
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-EDIT-OR-CREATE-START param: refundId [{}]", refundId);
        }
        if (Arguments.isNull(refundId)) {
            EditMiddleRefund editMiddleRefund = new EditMiddleRefund();
            editMiddleRefund.setIsToCreate(Boolean.TRUE);
            return editMiddleRefund;
        }
        EditMiddleRefund refund = makeEditMiddleRefund(refundId);
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-EDIT-OR-CREATE-END param: refundId [{}] ,resp: [{}]", refundId, JsonMapper.nonEmptyMapper().toJson(refund));
        }
        return refund;
    }


    /**
     * 创建逆向单
     *
     * @param submitRefundInfo 提交信息
     * @return 逆向单id
     */
    @RequestMapping(value = "/api/refund/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long createRefund(@RequestBody SubmitRefundInfo submitRefundInfo) {
        if (Objects.equals(submitRefundInfo.getRefundType(), MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value())) {
            //创建丢件补发类型的售后单
            return refundWriteLogic.createRefundForLost(submitRefundInfo);
        } else {
            if (!refundReadLogic.checkShipInfoUnique(submitRefundInfo.getShipmentSerialNo())) {
                throw new JsonResponseException("submit.ship.info.exists");
            }
            //创建仅退款，退货退款，换货的售后单
            return refundWriteLogic.createRefund(submitRefundInfo);
        }
    }

    /**
     * 完善逆向单
     *
     * @param refundId
     * @param editSubmitRefundInfo
     */
    @RequestMapping(value = "/api/refund/{id}/handle", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("完善或提交售后单")
    public void completeHandle(@PathVariable(value = "id") @OperationLogParam Long refundId, @RequestBody EditSubmitRefundInfo editSubmitRefundInfo) {
        String editSubmitRefundInfoStr = JsonMapper.nonEmptyMapper().toJson(editSubmitRefundInfo);
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-HANDLE-START param: refundId [{}] editSubmitRefundInfo [{}]", refundId, editSubmitRefundInfoStr);
        }
        Refund refund = refundReadLogic.findRefundById(refundId);
        log.info("refund extra extra info is {}, id is {}", refund.getExtraJson(), refund.getId());
        try {
            if (Objects.equals(refund.getRefundType(), MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value())) {
                //丢件补发售后单完善
                refundWriteLogic.completeHandleForLostType(refund, editSubmitRefundInfo);
            } else {
                if (!refundReadLogic.checkShipInfoUnique(editSubmitRefundInfo.getShipmentSerialNo())) {
                    throw new JsonResponseException("submit.ship.info.exists");
                }                
                // 退货退款，换货，的发货仓和退货仓账套是否匹配325的校验
                if(Objects.equals(refund.getRefundType(),MiddleRefundType.AFTER_SALES_RETURN.value()) || Objects.equals(refund.getRefundType(),MiddleRefundType.AFTER_SALES_CHANGE.value())){
                    String oriShipmentCode = editSubmitRefundInfo.getShipmentCode();
                    if (StringUtils.hasText(oriShipmentCode)) {
                        Shipment oriShipment = shipmentReadLogic.findShipmentByShipmentCode(oriShipmentCode);
                        String refundCompanyIsNot325 = checkShopOrWarehouse(oriShipment, refund, editSubmitRefundInfo.getWarehouseId());
                        if (StringUtils.hasText(refundCompanyIsNot325)) {
                            throw new JsonResponseException("submit.refund.companyCode.not.match");
                        }
                    }
                }                
                //进退款，退货退款，换货的售后单的完善
                refundWriteLogic.completeHandle(refund, editSubmitRefundInfo);
                if (Objects.equals(editSubmitRefundInfo.getOperationType(), 2)) {

                    //对于换货售后单来讲必须等到占用库存成功之后更新售后单售后单状态
                    boolean result = true;
                    if (Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value())) {
                        result = refundWriteLogic.createOccupyShipments(editSubmitRefundInfo.getEditSubmitChangeItems(), refund.getId());
                    }
                    if (result) {
                        //完善之后同步售后单到订单派发中心
                        //Flow flow = flowPicker.pickAfterSales();
                        refund = refundReadLogic.findRefundById(refundId);
                        log.info("sync refund extra extra info is {}, id is {}", refund.getExtraJson(), refund.getId());
                        //Integer targetStatus = flow.target(refund.getStatus(),MiddleOrderEvent.HANDLE.toOrderOperation());
                        //refund.setStatus(targetStatus);
                        Response<Boolean> syncRes = syncErpReturnLogic.syncReturn(refund);
                        if (!syncRes.isSuccess()) {
                            log.error("sync refund(id:{}) to hk fail,error:{}", refundId, syncRes.getError());
                        } else {
                            //挂起操作
                            if (Objects.equals(refund.getShopId(), skxOpenShopId)) {
                                refundWriteLogic.syncFreezeSkxShipment(refundId);
                            }
                        }
                    } else {
                        throw new JsonResponseException("complete.refund.failed");
                    }
                }
            }
        } catch (JsonResponseException e1) {
            log.error("Refunds completeHandle failed,error by {}", e1.getMessage());
            throw new JsonResponseException(e1.getMessage());
        } catch (Exception e) {
            log.error("Refunds completeHandle failed,casused by {}", Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("complete.refund.failed");
        }
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-HANDLE-END param: refundId [{}] editSubmitRefundInfo [{}]", refundId, editSubmitRefundInfoStr);
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
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-BATCH-HANDLE-START param: data [{}]", data);
        }
        //获取需要批量处理的售后单id集合
        List<Long> refundIds = Splitters.splitToLong(data, Splitters.COMMA);
        //获取售后单集合
        List<Refund> refunds = refundReadLogic.findRefundByIds(refundIds);
        if (!Objects.equals(refundIds.size(), refunds.size())) {
            log.error("find refund by refund ids:{} result size not equal request id size:{}", refundIds, refunds.size(), refundIds.size());
            throw new JsonResponseException("refund.id.invalid");
        }

        List<String> checkFailedRefundCodes = Lists.newArrayList();
        List<String> handleFailedRefundCodes = Lists.newArrayList();
        List<String> syncFailedRefundCodes = Lists.newArrayList();
        List<String> refundCompanyCodeIsNot325 = Lists.newArrayList(); 
        //判断是否存在没有完善的售后单
        refunds = refunds.stream().filter(refund -> {
            Map<String, String> refundExtraMap = refund.getExtra();
            if (!refundExtraMap.containsKey(TradeConstants.MIDDLE_REFUND_COMPLETE_FLAG)) {
                checkFailedRefundCodes.add(refund.getRefundCode());
                log.error("refund(id:{}) check fail", refund.getId());
                return false;
            }else if(Objects.equals(refund.getRefundType(),MiddleRefundType.AFTER_SALES_RETURN.value()) || Objects.equals(refund.getRefundType(),MiddleRefundType.AFTER_SALES_CHANGE.value())){
                String isNot325company = getisNot325CompanyCode(refund, refundExtraMap);
                if(StringUtils.hasText(isNot325company)){ 
                    refundCompanyCodeIsNot325.add(isNot325company);
                    log.error("refund(id:{}) check fail companyCode is not 325", refund.getId());
                    return false;
                }
            }            
            return true;
        }).collect(Collectors.toList());
       
        refunds.forEach(refund -> {
            log.info("refund extra extra info is {}, id is {}", refund.getExtraJson(), refund.getId());
            if (Objects.equals(refund.getRefundType(), MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value())) {
                OrderOperation orderOperation = MiddleOrderEvent.LOST_HANDLE.toOrderOperation();
                Response<Boolean> response = refundWriteLogic.updateStatusLocking(refund, orderOperation);
                if (!response.isSuccess()) {
                    log.error("refund(id:{}) operation:{} fail", refund.getId(), orderOperation);
                    //throw new JsonResponseException(response.getError());
                    handleFailedRefundCodes.add(refund.getRefundCode());
                }
            } else {
                OrderOperation orderOperation = MiddleOrderEvent.HANDLE.toOrderOperation();
                Response<Boolean> response = refundWriteLogic.updateStatusLocking(refund, orderOperation);
                if (!response.isSuccess()) {
                    log.error("refund(id:{}) operation:{} fail", refund.getId(), orderOperation);
                    //throw new JsonResponseException(response.getError());
                    handleFailedRefundCodes.add(refund.getRefundCode());
                } else {
                    //审核之后同步售后单到恒康
                    if (Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value())) {
                        List<RefundItem> refundChangeItems = refundReadLogic.findRefundChangeItems(refund);
                        List<EditSubmitChangeItem> editSubmitChangeItems = Lists.newArrayList();
                        for (RefundItem refundItem : refundChangeItems) {
                            EditSubmitChangeItem editSubmitChangeItem = new EditSubmitChangeItem();
                            editSubmitChangeItem.setExchangeWarehouseId(refundItem.getExchangeWarehouseId());
                            editSubmitChangeItem.setExchangeWarehouseName(refundItem.getExchangeWarehouseName());
                            editSubmitChangeItem.setChangeSkuCode(refundItem.getSkuCode());
                            editSubmitChangeItem.setChangeQuantity(refundItem.getApplyQuantity());
                            editSubmitChangeItems.add(editSubmitChangeItem);
                        }
                        boolean result = refundWriteLogic.createOccupyShipments(editSubmitChangeItems, refund.getId());
                        if (result) {
                            Flow flow = flowPicker.pickAfterSales();
                            Integer targetStatus = flow.target(refund.getStatus(), MiddleOrderEvent.HANDLE.toOrderOperation());
                            refund = refundReadLogic.findRefundById(refund.getId());
                            log.info("sync refund extra extra info is {}, id is {}", refund.getExtraJson(), refund.getId());
                            refund.setStatus(targetStatus);
                            Response<Boolean> syncRes = syncErpReturnLogic.syncReturn(refund);
                            if (!syncRes.isSuccess()) {
                                log.error("sync refund(id:{}) to hk fail,error:{}", refund.getId(), syncRes.getError());
                                syncFailedRefundCodes.add(refund.getRefundCode());
                            } else {
                                //挂起操作
                                if (Objects.equals(refund.getShopId(), skxOpenShopId)) {
                                    refundWriteLogic.syncFreezeSkxShipment(refund.getId());
                                }
                            }
                        }
                    } else {
                        Flow flow = flowPicker.pickAfterSales();
                        Integer targetStatus = flow.target(refund.getStatus(), MiddleOrderEvent.HANDLE.toOrderOperation());
                        refund.setStatus(targetStatus);
                        Response<Boolean> syncRes = syncErpReturnLogic.syncReturn(refund);
                        if (!syncRes.isSuccess()) {
                            log.error("sync refund(id:{}) to hk fail,error:{}", refund.getId(), syncRes.getError());
                            syncFailedRefundCodes.add(refund.getRefundCode());
                        }
                    }
                }
            }
        });

        if (!CollectionUtils.isEmpty(checkFailedRefundCodes)
                || !CollectionUtils.isEmpty(handleFailedRefundCodes)
                || !CollectionUtils.isEmpty(syncFailedRefundCodes)) {
            throw new InvalidException("refund.batch.deal.fail.info(check.incomplete={0},handle.failed={1},sync.failed={2},not.match.companyCode={3})",
                    checkFailedRefundCodes.toString(),
                    handleFailedRefundCodes.toString(),
                    syncFailedRefundCodes.toString(),
                    refundCompanyCodeIsNot325.toString());
        }

        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-BATCH-HANDLE-END param: data [{}]", data);
        }

    }
    
    //删除逆向单

    @RequestMapping(value = "/api/refund/{id}/delete", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("删除售后单")
    public void delete(@PathVariable(value = "id") @OperationLogParam @PermissionCheckParam Long refundId) {
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-DELETE-START param: refundId [{}]", refundId);
        }
        Refund refund = refundReadLogic.findRefundById(refundId);
        if (Objects.equals(refund.getRefundType(), MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value())) {
            refundWriteLogic.deleteRefundForLost(refund);
        } else {
            //第三方同步过来的售后单不允许被删除
            refundWriteLogic.deleteRefund(refund);
        }
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-DELETE-END param: refundId [{}]", refundId);
        }
    }


    /**
     * 换货单,丢件补发单---待发货商品列表 for 手动生成发货单流程的选择仓库页面
     *
     * @param refundId 换货单id
     * @return 待发货商品列表 注意：待发货数量(waitHandleNumber) = 退货数量 - 已发货数量
     */
    @RequestMapping(value = "/api/refund/{id}/wait/handle/sku", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<WaitShipItemInfo> refundWaitHandleSku(@PathVariable("id") @PermissionCheckParam Long refundId) {
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-WAIT-HANDLE-SKU-START param: refundId [{}]", refundId);
        }
        Refund refund = refundReadLogic.findRefundById(refundId);
        if (Objects.equals(refund.getRefundType(), MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value())) {
            //获取丢件补发代发货商品列表
            List<RefundItem> refundLostItems = refundReadLogic.findRefundLostItems(refund);
            List<WaitShipItemInfo> waitShipItemInfos = Lists.newArrayListWithCapacity(refundLostItems.size());
            for (RefundItem refundItem : refundLostItems) {
                if ((refundItem.getApplyQuantity() - (refundItem.getAlreadyHandleNumber() == null ? 0 : refundItem.getAlreadyHandleNumber())) > 0) {
                    this.waitShipItems(waitShipItemInfos, refundItem);
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("API-REFUND-WAIT-HANDLE-SKU-END param: refundId [{}] ,resp: [{}]", refundId, waitShipItemInfos);
            }
            return waitShipItemInfos;
        } else {
            //获取换货单待发货商品列表
            List<RefundItem> refundChangeItems = refundReadLogic.findRefundChangeItems(refund);
            List<WaitShipItemInfo> waitShipItemInfos = Lists.newArrayListWithCapacity(refundChangeItems.size());
            for (RefundItem refundItem : refundChangeItems) {
                if ((refundItem.getApplyQuantity() - (refundItem.getAlreadyHandleNumber() == null ? 0 : refundItem.getAlreadyHandleNumber())) > 0) {
                    this.waitShipItems(waitShipItemInfos, refundItem);
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("API-REFUND-WAIT-HANDLE-SKU-END param: refundId [{}] ,resp: [{}]", refundId, waitShipItemInfos);
            }
            return waitShipItemInfos;
        }
    }


    /**
     * 同步售后单到订单派发中心
     *
     * @param refundId 售后单id
     */
    @RequestMapping(value = "api/refund/{id}/sync/hk", method = RequestMethod.PUT)
    public void syncHkRefund(@PathVariable(value = "id") @PermissionCheckParam Long refundId) {
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-SYNCHKREFUND-START param: refundId [{}] ", refundId);
        }
        Refund refund = refundReadLogic.findRefundById(refundId);
        Response<Boolean> syncRes = syncErpReturnLogic.syncReturn(refund);
        if (!syncRes.isSuccess()) {
            log.error("sync refund(id:{}) to hk fail,error:{}", refundId, syncRes.getError());
            throw new JsonResponseException(syncRes.getError());
        } else {
            //挂起操作
            if (Objects.equals(refund.getShopId(), skxOpenShopId)) {
                refundWriteLogic.syncFreezeSkxShipment(refundId);
            }
        }
        //取消成功之后，换货售后单需要取消占用库存发货单(异步操作即可)

        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-SYNCHKREFUND-END param: refundId [{}] ", refundId);
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
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-CANCLEREFUND-START param: refundId [{}] ", refundId);
        }
        Refund refund = refundReadLogic.findRefundById(refundId);
        Response<Boolean> cancelRes = refundWriteLogic.updateStatusLocking(refund, MiddleOrderEvent.CANCEL.toOrderOperation());
        if (!cancelRes.isSuccess()) {
            log.error("cancel refund(id:{}) fail,error:{}", refundId, cancelRes.getError());
            throw new JsonResponseException(cancelRes.getError());
        }
        //回滚发货单的数量
        if (!Objects.equals(refund.getRefundType(), MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value())) {
            refundWriteLogic.rollbackRefundQuantities(refund);
        }
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-CANCLEREFUND-END param: refundId [{}] ", refundId);
        }
    }


    /**
     * 同步售后单取消状态到恒康（订单派发中心）
     *
     * @param refundId 售后单id
     */
    @RequestMapping(value = "api/refund/{id}/cancel/sync/hk", method = RequestMethod.PUT)
    public void syncHkCancelRefund(@PathVariable(value = "id") @PermissionCheckParam Long refundId) {
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-SYNCHKCANCELREFUND-START param: refundId [{}] ", refundId);
        }
        Refund refund = refundReadLogic.findRefundById(refundId);
        if (!Objects.equals(refund.getRefundType(), MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value())) {
            RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
            Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(refundExtra.getShipmentId());
            //店发拒收单不能被取消
            if (Objects.equals(refund.getRefundType(), MiddleRefundType.REJECT_GOODS.value())
                    && (Objects.equals(shipment.getShipWay(), 1))) {
                throw new JsonResponseException("reject.goods.can.not.be.canceled");
            }
            if (Objects.equals(refund.getShopId(), skxOpenShopId)) {
                boolean result = refundWriteLogic.validateSkxAfterSaleShipmentStatus(refundId);
                if (result) {
                    throw new JsonResponseException("skx.refund.shipment.handling");
                }
            }
            //如果是之前同步恒康失败的，不用和恒康连接直接取消失败
            if (Objects.equals(refund.getStatus(), MiddleRefundStatus.SYNC_HK_FAIL.getValue())) {
                OrderOperation syncSuccessOrderOperation = MiddleOrderEvent.CANCEL_HK.toOrderOperation();
                Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatusLocking(refund, syncSuccessOrderOperation);
                if (!updateSyncStatusRes.isSuccess()) {
                    log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), syncSuccessOrderOperation.getText(), updateSyncStatusRes.getError());
                    throw new JsonResponseException(updateSyncStatusRes.getError());
                }
            } else {
                Response<Boolean> syncRes = syncErpReturnLogic.syncReturnCancel(refund);
                if (!syncRes.isSuccess()) {
                    log.error("sync cancel refund(id:{}) to hk fail,error:{}", refundId, syncRes.getError());
                    throw new JsonResponseException(syncRes.getError());
                }
            }
            //回滚发货单的数量
            refundWriteLogic.rollbackRefundQuantities(refund);
            if (Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value())) {
                //非skx占库单取消使用原来占库单取消的逻辑
                //skx占库单取消需要到skx取消一把
                if (!Objects.equals(refund.getShopId(), skxOpenShopId)) {
                    refundWriteLogic.cancelAfterSaleOccupyShipments(refundId);
                } else {
                    refundWriteLogic.cancelSkxAfterSaleOccupyShipments(refundId);
                }
            }
        } else {
            //拒收单取消
            OrderOperation syncSuccessOrderOperation = MiddleOrderEvent.CANCEL_HK.toOrderOperation();
            Response<Boolean> updateSyncStatusRes = refundWriteLogic.updateStatusLocking(refund, syncSuccessOrderOperation);
            if (!updateSyncStatusRes.isSuccess()) {
                log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), syncSuccessOrderOperation.getText(), updateSyncStatusRes.getError());
                throw new JsonResponseException(updateSyncStatusRes.getError());
            }
            refundWriteLogic.releaseRejectShipmentOccupyStock(refundId);
        }
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-SYNCHKCANCELREFUND-END param: refundId [{}] ", refundId);
        }
    }

    /**
     * 恒康同步信息到中台提示已经退货完成,此时调用该接口通知电商退款
     *
     * @param refundId
     */
    @RequestMapping(value = "api/refund/{id}/sync/ecp", method = RequestMethod.PUT)
    public void syncECPRefund(@PathVariable(value = "id") @PermissionCheckParam Long refundId) {
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-SYNCECPREFUND-START param: refundId [{}] ", refundId);
        }
        Refund refund = refundReadLogic.findRefundById(refundId);
        Response<Boolean> syncRes = syncRefundToEcpLogic.syncRefundToECP(refund);
        if (!syncRes.isSuccess()) {
            log.error("sync cancel refund(id:{}) to ecp fail,error:{}", refundId, syncRes.getError());
            throw new JsonResponseException(syncRes.getError());
        }
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-SYNCECPREFUND-END param: refundId [{}] ", refundId);
        }
    }


    /**
     * 运营确认收货 （换货）
     *
     * @param refundId 售后单id
     */
    @RequestMapping(value = "api/refund/{id}/confirm/received", method = RequestMethod.PUT)
    public void confirmReceived(@PathVariable(value = "id") @PermissionCheckParam Long refundId) {
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-CONFIRMRECEIVED-START param: refundId [{}] ", refundId);
        }
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
        Response<Boolean> cancelRes1 = refundWriteLogic.updateStatusLocking(refund, MiddleOrderEvent.CONFIRM.toOrderOperation());
        if (!cancelRes1.isSuccess()) {
            log.error("confirm refund(id:{}) fail,error:{}", refundId, cancelRes1.getError());
            throw new JsonResponseException(cancelRes1.getError());
        }
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-CONFIRMRECEIVED-END param: refundId [{}] ", refundId);
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
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-CREATECUSTOMERSERVICENOTE-START param: id [{}] ,customerSerivceNote [{}]", id, customerSerivceNote);
        }
        refundWriteLogic.addCustomerServiceNote(id, customerSerivceNote);
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-CREATECUSTOMERSERVICENOTE-END param: id [{}] ,customerSerivceNote [{}]", id, customerSerivceNote);
        }
    }

    /**
     * 换货改退货取消售后单,必须保证这个状态已经收货
     *
     * @param id 售后单id
     */
    @RequestMapping(value = "/api/refund/{id}/cancel/on/change", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("换货改退货取消售后单")
    public void cancelRefundForChange(@PathVariable("id") @OperationLogParam Long id) {
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-CANCELREFUNDFORCHANGE-START param: id [{}]", id);
        }
        Refund originRefund = refundReadLogic.findRefundById(id);
        if (refundReadLogic.isAfterSaleCanCancelShip(originRefund)) {
            //如果允许取消发货则修改状态，退货改换货需要判断一下是否存在有库存没有释放的情况
            refundWriteLogic.releaseRejectShipmentOccupyStock(id);

            Refund refund = refundReadLogic.findRefundById(id);
            //如果允许取消，则将占库发货单取消即可
            if (!Objects.equals(refund.getShopId(), skxOpenShopId)) {
                refundWriteLogic.cancelAfterSaleOccupyShipments(id);
            } else {
                refundWriteLogic.syncCancelSkxShipment(id);
            }
            refundWriteLogic.updateStatusLocking(refund, MiddleOrderEvent.AFTER_SALE_CANCEL_SHIP.toOrderOperation());
            Flow flow = flowPicker.pickAfterSales();
            Integer targetStatus = flow.target(refund.getStatus(), MiddleOrderEvent.AFTER_SALE_CANCEL_SHIP.toOrderOperation());
            RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
            refundExtra.setCancelShip("true");
            Map<String, String> extraMap = refund.getExtra();
            extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));
            refund.setStatus(targetStatus);
            refund.setExtra(extraMap);
            refundWriteLogic.update(refund);

        } else {
            throw new JsonResponseException("after.sale.cancel.shipment.status.invalid");
        }
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-CANCELREFUNDFORCHANGE-END param: id [{}]", id);
        }
    }

    /**
     * 人工确认已经退款
     *
     * @param refundId 退款单id
     */
    @RequestMapping(value = "/api/refund/{id}/manual/confirm/refund", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("人工确认已经退款")
    public void confirmRefund(@PathVariable("id") @OperationLogParam Long refundId) {
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-CONFIRMREFUND-START param: refundId [{}]", refundId);
        }
        Refund refund = refundReadLogic.findRefundById(refundId);
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refundId);
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderRefund.getOrderId());
        if (!Objects.equals(shopOrder.getOutFrom(), MiddleChannel.JD.getValue())) {
            throw new JsonResponseException("only.channel.jd.can.manual.confirm.refund");
        }
        Integer sourceStatus = refund.getStatus();
        Flow flow = flowPicker.pickAfterSales();
        if (!flow.operationAllowed(sourceStatus, MiddleOrderEvent.REFUND.toOrderOperation())) {
            log.error("refund(id:{}) current status:{} not allow operation:{}", refund.getId(), refund.getStatus(), MiddleOrderEvent.REFUND.toOrderOperation().getText());
            throw new JsonResponseException("order.status.invalid");
        }
        Response<Boolean> updateStatusRes = refundWriteLogic.updateStatusLocking(refund, MiddleOrderEvent.REFUND.toOrderOperation());
        if (!updateStatusRes.isSuccess()) {
            log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), MiddleOrderEvent.REFUND.toOrderOperation().getText(), updateStatusRes.getError());
            throw new JsonResponseException("update.refund.error");
        }
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-CONFIRMREFUND-END param: refundId [{}]", refundId);
        }
    }

    /**
     * 人工确认已经退货
     *
     * @param refundId 售后单id
     */
    @RequestMapping(value = "/api/refund/{id}/manual/confirm/return", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("人工确认已经退货")
    public void confirmReturn(@PathVariable("id") @OperationLogParam Long refundId) {
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-CONFIRMRETURN-START param: refundId [{}]", refundId);
        }
        Refund refund = refundReadLogic.findRefundById(refundId);
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refundId);
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderRefund.getOrderId());
        if (Objects.equals(refund.getRefundType(), MiddleRefundType.REJECT_GOODS.value())) {
            throw new JsonResponseException("reject.goods.can.not.be.confirm.refunds");
        }
        if (!Objects.equals(shopOrder.getOutFrom(), MiddleChannel.JD.getValue())) {
            throw new JsonResponseException("only.channel.jd.can.manual.confirm.return");
        }
        Integer sourceStatus = refund.getStatus();
        Flow flow = flowPicker.pickAfterSales();
        if (!flow.operationAllowed(sourceStatus, MiddleOrderEvent.RETURN.toOrderOperation())) {
            log.error("refund(id:{}) current status:{} not allow operation:{}", refund.getId(), refund.getStatus(), MiddleOrderEvent.RETURN.toOrderOperation().getText());
            throw new JsonResponseException("order.status.invalid");
        }
        Response<Boolean> updateStatusRes = refundWriteLogic.updateStatusLocking(refund, MiddleOrderEvent.RETURN.toOrderOperation());
        if (!updateStatusRes.isSuccess()) {
            log.error("refund(id:{}) operation :{} fail,error:{}", refund.getId(), MiddleOrderEvent.RETURN.toOrderOperation().getText(), updateStatusRes.getError());
            throw new JsonResponseException("update.refund.error");
        }
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-CONFIRMRETURN-END param: refundId [{}]", refundId);
        }
    }

    /**
     * 判断售后单来源是否是京东
     *
     * @param refundId 售后单id
     * @return true:订单来源是京东，false:订单来源非京东
     */
    @RequestMapping(value = "/api/refund/{id}/is/out/from/jd", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean isOutFromJD(@PathVariable("id") Long refundId) {
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-ISOUTFROMJD-START param: refundId [{}]", refundId);
        }
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refundId);
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderRefund.getOrderId());
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-ISOUTFROMJD-END param: refundId [{}] ,resp: [{}]", refundId, Objects.equals(shopOrder.getOutFrom(), MiddleChannel.JD.getValue()));
        }
        return Objects.equals(shopOrder.getOutFrom(), MiddleChannel.JD.getValue());
    }

    /**
     * 计算最多可退金额
     *
     * @param orderCode    订单主键
     * @param shipmentCode 发货单主键
     * @param refundId     退货单主键
     * @param list         skuCode-applyQuantity 集合
     * @return
     */
    @RequestMapping(value = "/api/refund/{code}/already/refund/fee", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public int getAlreadyRefundFee(@PathVariable("code") String orderCode, @RequestParam("shipmentId") String shipmentCode, @RequestParam(required = false) Long refundId,
                                   @RequestParam(value = "list") String list) {
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-GETALREADYREFUNDFEE-START param: orderCode [{}] shipmentCode [{}] refundId [{}] list [{}]", orderCode, shipmentCode, refundId, list);
        }
        List<RefundFeeData> refundFeeDatas = JsonMapper.nonEmptyMapper().fromJson(list, JsonMapper.nonEmptyMapper().createCollectionType(List.class, RefundFeeData.class));
        int num = refundReadLogic.getAlreadyRefundFee(orderCode, refundId, shipmentCode, refundFeeDatas);
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-GETALREADYREFUNDFEEx-END param: orderCode [{}] shipmentCode [{}] refundId [{}] list [{}] ,resp: [{}]", orderCode, shipmentCode, refundId, list, num);
        }
        return num;
    }

    @RequestMapping(value = "/api/refund/max/refund/fee", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public int maxRefundFee(@RequestParam(value = "list") String list) {
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-MAXREFUNDFEE-START param: list [{}]", list);
        }
        List<RefundFeeData> refundFeeDatas = JsonMapper.nonEmptyMapper().fromJson(list, JsonMapper.nonEmptyMapper().createCollectionType(List.class, RefundFeeData.class));
        int maxRefundFee = 0;
        for (RefundFeeData refundFeeData : refundFeeDatas) {
            maxRefundFee += refundFeeData.getApplyQuantity() * refundFeeData.getCleanPrice();
        }
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-MAXREFUNDFEE-START param: list [{}] maxFee[{}]", list, maxRefundFee);
        }
        return maxRefundFee;
    }

    /**
     * 修改订单的收货信息，主要是换货使用
     *
     * @param id                      售后单主键
     * @param middleChangeReceiveInfo
     */
    @RequestMapping(value = "/api/refund/{id}/edit/receiver/info", method = RequestMethod.PUT)
    @OperationLogType("修改换货售后单客户收货地址")
    public void editReceiverInfos(@PathVariable("id") @OperationLogParam Long id, @RequestParam(required = false) String buyerName, @RequestBody MiddleChangeReceiveInfo middleChangeReceiveInfo) {
        String middleChangeReceiveInfoStr = JsonMapper.nonEmptyMapper().toJson(middleChangeReceiveInfo);
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-EDITRECEIVERINFOS-START param: id [{}] buyerName [{}] middleChangeReceiveInfo [{}]", id, buyerName, middleChangeReceiveInfoStr);
        }
        middleRefundWriteService.updateReceiveInfos(id, middleChangeReceiveInfo);
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-EDITRECEIVERINFOS-END param: id [{}] buyerName [{}] middleChangeReceiveInfo [{}]", id, buyerName, middleChangeReceiveInfoStr);
        }
    }


    /**
     * 丢件补发类型客服确认客户收货
     *
     * @param id 售后单id
     * @return
     */
    @RequestMapping(value = "/api/refund/{id}/customer/service/confirm/done", method = RequestMethod.PUT)
    @OperationLogType("丢件补发客服确认收货")
    public Response<Boolean> confirmDoneForLost(@PathVariable("id") @OperationLogParam Long id) {
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-CONFIRMDONEFORLOST-START param: id [{}]", id);
        }
        Refund refund = refundReadLogic.findRefundById(id);
        Response<Boolean> r = refundWriteLogic.updateStatusLocking(refund, MiddleOrderEvent.LOST_CONFIRMED.toOrderOperation());
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-CONFIRMDONEFORLOST-END param: id [{}] ,resp: [{}]", id, r.getResult());
        }
        return Response.ok(r.isSuccess());
    }


    /**
     * 同步售后单到恒康开pos
     *
     * @param id 售后单id
     * @return
     */
    @RequestMapping(value = "/api/refund/{id}/sync/hk/pos", method = RequestMethod.GET)
    @OperationLogType("同步售后单到恒康开pos")
    public Response<Boolean> syncRefundToHkPos(@PathVariable("id") @OperationLogParam Long id) {
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-SYNCREFUNDTOHKPOS-START param: id [{}]", id);
        }
        Refund refund = refundReadLogic.findRefundById(id);
        Response<Boolean> resp = syncRefundPosLogic.syncRefundPosToHk(refund);
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-SYNCREFUNDTOHKPOS-END param: id [{}] ,resp: [{}]", id, resp.getResult());
        }
        return resp;
    }

    /**
     * 售后占用库存发货单确认后同步mpos或者yyedi
     *
     * @param refundId 售后单id
     * @return
     */
    @RequestMapping(value = "/api/refund/{refundId}/occupy/shipment/confirm", method = RequestMethod.PUT)
    @OperationLogType("售后单占库发货单确认")
    public Response<Boolean> confirmAfterSaleOccupyShipments(@PathVariable("refundId") @OperationLogParam Long refundId) {
        Refund refund = refundReadLogic.findRefundById(refundId);
        List<OrderShipment> orderShipments = shipmentReadLogic.findByAfterOrderIdAndType(refundId);
        if (orderShipments.isEmpty()) {
            throw new JsonResponseException("no.exist.occupy.shipments");
        } else {
            //判断是否存在有效的发货单
            int count = 0;
            for (OrderShipment orderShipment : orderShipments) {
                if (Objects.equals(orderShipment.getStatus(), MiddleShipmentsStatus.CANCELED.getValue())) {
                    count++;
                }
            }
            if (count == orderShipments.size()) {
                throw new JsonResponseException("no.exist.occupy.shipments");
            }
        }

        for (OrderShipment orderShipment : orderShipments) {
            //修改发货单类型，并且同步订单派发中心或者mpos
            shipmentWiteLogic.updateOccupyShipmentTypeByShipmentId(orderShipment.getShipmentId(), ShipmentOccupyType.CHANGE_N.name());

            if (!Objects.equals(refund.getShopId(), skxOpenShopId)) {
                //订单派发中心直接同步过去
                shipmentWiteLogic.syncExchangeShipment(orderShipment.getShipmentId());
            } else {
                //skx挂起即可
                refundWriteLogic.syncUnFreezeSkxShipment(refundId, refund.getRefundCode());
            }
        }
        //修改售后单状态为待发货
        refundWriteLogic.updateStatus(refund, MiddleOrderEvent.CONFIRM_OCCUPY_SHIPMENT.toOrderOperation());
        return Response.ok(Boolean.TRUE);
    }

    /**
     * 占库发货单取消
     *
     * @param refundId 售后单id
     * @return
     */
    @RequestMapping(value = "/api/refund/{refundId}/occupy/shipment/cancel", method = RequestMethod.PUT)
    @OperationLogType("售后单占库发货单取消")
    public Response<Boolean> cancelAfterSaleOccupyShipments(@PathVariable("refundId") @OperationLogParam Long refundId) {
        Refund refund = refundReadLogic.findRefundById(refundId);
        //非skx占库单取消使用原来占库单取消的逻辑
        //skx占库单取消需要到skx取消一把
        if (!Objects.equals(refund.getShopId(), skxOpenShopId)) {
            refundWriteLogic.cancelAfterSaleOccupyShipments(refundId);
        } else {
            refundWriteLogic.syncCancelSkxShipment(refundId);
        }

        refundWriteLogic.updateStatus(refund, MiddleOrderEvent.AFTER_SALE_CHANGE_RE_CREATE_SHIPMENT.toOrderOperation());
        //售后单已经申请售后的数量设置为0
        List<RefundItem> exchangeItems = refundReadLogic.findRefundChangeItems(refund);
        List<RefundItem> newExchangeItems = Lists.newArrayList();
        for (RefundItem refundItem : exchangeItems) {
            refundItem.setAlreadyHandleNumber(0);
            newExchangeItems.add(refundItem);
        }
        Refund newRefund = refundReadLogic.findRefundById(refund.getId());
        Map<String, String> refundExtra = newRefund.getExtra();
        refundExtra.put(TradeConstants.REFUND_CHANGE_ITEM_INFO, JsonMapper.nonEmptyMapper().toJson(newExchangeItems));
        newRefund.setExtra(refundExtra);
        refundWriteLogic.update(newRefund);

        return Response.ok(Boolean.TRUE);
    }

    /**
     * 根据订单号获取所关联的售后单号,包括订单号自身，需要判断哪些状态的售后单不允许售后（负面清单）
     *
     * @param code
     * @return
     */
    @RequestMapping(value = "/api/refund/order/{code}/after/sale", method = RequestMethod.GET)
    public Response<List<MiddleAfterSaleInfo>> findAfterSaleIds(@PathVariable("code") String code) {
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-FINDAFTERSALEIDS-START param: code [{}]", code);
        }
        ShopOrder shopOrder = orderReadLogic.findShopOrderByCode(code);
        List<MiddleAfterSaleInfo> middleAfterSaleInfos = Lists.newArrayList();
        //查询售后单信息
        List<Refund> refunds = refundReadLogic.findRefundsByOrderCode(code);
        //添加过滤售后单id,状态，类型(丢件补发，和换货才会可以继续申请售后)
        List<Refund> afterSales = refunds.stream().filter(Objects::nonNull)
                .filter(it -> !Objects.equals(it.getStatus(), MiddleRefundStatus.DELETED.getValue()))
                .filter(it -> !Objects.equals(it.getStatus(), MiddleRefundStatus.CANCELED.getValue()))
                .filter(it -> !Objects.equals(it.getStatus(), MiddleRefundStatus.SYNC_HK_FAIL.getValue()))
                .filter(it -> !Objects.equals(it.getStatus(), MiddleRefundStatus.WAIT_HANDLE.getValue()))
                .filter(it -> !Objects.equals(it.getStatus(), MiddleRefundStatus.RETURN_DONE_WAIT_CREATE_SHIPMENT.getValue()))
                .filter(it -> !Objects.equals(it.getStatus(), MiddleRefundStatus.RETURN_DONE_WAIT_CONFIRM_OCCUPY_SHIPMENT.getValue()))
                .filter(it -> !Objects.equals(it.getStatus(), MiddleRefundStatus.WAIT_SHIP.getValue()))
                .filter(it -> !Objects.equals(it.getStatus(), MiddleRefundStatus.LOST_WAIT_CREATE_SHIPMENT.getValue()))
                .filter(it -> !Objects.equals(it.getStatus(), MiddleRefundStatus.LOST_WAIT_SHIP.getValue()))
                .filter(it -> (Objects.equals(it.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value()) || Objects.equals(it.getRefundType(), MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value())))
                .collect(Collectors.toList());
        //添加售后单信息
        afterSales.forEach(refund -> {
            MiddleAfterSaleInfo middleAfterSaleInfo = new MiddleAfterSaleInfo();
            middleAfterSaleInfo.setId(refund.getId());
            middleAfterSaleInfo.setCode(refund.getRefundCode());
            middleAfterSaleInfo.setType(2);
            middleAfterSaleInfo.setDesc("售后单");
            middleAfterSaleInfos.add(middleAfterSaleInfo);
        });
        //添加订单信息
        MiddleAfterSaleInfo middleAfterSaleInfo = new MiddleAfterSaleInfo();
        middleAfterSaleInfo.setId(shopOrder.getId());
        middleAfterSaleInfo.setCode(shopOrder.getOrderCode());
        middleAfterSaleInfo.setType(1);
        middleAfterSaleInfo.setDesc("订单");
        middleAfterSaleInfos.add(middleAfterSaleInfo);
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-FINDAFTERSALEIDS-END param: code [{}] ,resp: [{}]", code, JsonMapper.nonEmptyMapper().toJson(middleAfterSaleInfos));
        }
        return Response.ok(middleAfterSaleInfos);
    }


    /**
     * 售后单类型修改 售后仅退款-->拒收单
     * 修改售后单类型需要去判断当前售后单类型（MiddleRefundType.AFTER_SALES_REFUND）是否为售后仅退款
     * 并且需要判断当前售后单状态是否为待完善的状态（MiddleRefundStatus.WAIT_HANDLE）
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/api/refund/{id}/edit/refund/type", method = RequestMethod.PUT)
    @ApiOperation(value = "修改售后单类型")
    public Response<Boolean> updateRefundType(@PathVariable("id") Long id) {
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-UPDATEREFUNDTYPE-START param: id [{}]", id);
        }
        Refund refund = refundReadLogic.findRefundById(id);
        if (!Objects.equals(refund.getStatus(), MiddleRefundStatus.WAIT_HANDLE.getValue()) ||
                !Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_REFUND.value())) {
            log.error("current refund id {}, refund type {} can not edit", id, refund.getRefundType());
            throw new JsonResponseException("refund.status.or.type.not.allow.current.operation");
        }
        refund.setRefundType(MiddleRefundType.REJECT_GOODS.value());

        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);

        List<RefundItem> refundItemList = refundReadLogic.findRefundItems(refund);
        //发货单是否有效
        Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(refundExtra.getShipmentId());

        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);

        //获取发货单中商品信息
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);
        refundItemList.stream().forEach(x -> {
            for (ShipmentItem shipmentItem : shipmentItems) {
                if (Objects.equals(x.getSkuOrderId(), shipmentItem.getSkuOrderId())) {
                    shipmentItem.setRefundQuantity((shipmentItem.getRefundQuantity() == null ? 0 : shipmentItem.getRefundQuantity()) + x.getApplyQuantity());
                }
            }
        });

        //更新发货单商品中的已退货数量
        //TODO 更新发货单明细
        shipmentWiteLogic.updateShipmentItem(shipment, shipmentItems);
        //        Map<String, String> shipmentExtraMap = shipment.getExtra();
        //        shipmentExtraMap.put(TradeConstants.SHIPMENT_ITEM_INFO, JsonMapper.nonEmptyMapper().toJson(shipmentItems));
        //        shipmentWiteLogic.updateExtra(shipment.getId(), shipmentExtraMap);


        Map<String, String> extraMap = refund.getExtra();

        //填入售后单仓库id
        refundExtra.setWarehouseId(shipmentExtra.getWarehouseId());
        //填入售后单仓库名称
        refundExtra.setWarehouseName(shipmentExtra.getWarehouseName());

        extraMap.put(TradeConstants.REFUND_EXTRA_INFO, mapper.toJson(refundExtra));

        refund.setExtra(extraMap);

        // 更新售后单
        Response<Boolean> res = refundWriteLogic.update(refund);
        if (!res.isSuccess()) {
            log.error("update refund id ({}) fail,error:{}", id, res.getError());
            return Response.fail(res.getError());
        }
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-UPDATEREFUNDTYPE-END param: id [{}] ,resp: [{}]", id, res.getResult());
        }
        return res;
    }

    @RequestMapping(value = "/api/refund/express/unique/check", method = RequestMethod.GET)
    @ApiOperation(value = "检查退货物流信息唯一性")
    public Response<Boolean> expressUniqueCheck(@RequestParam String shipmentSerialNo) {
        if (log.isDebugEnabled()) {
            log.debug("CHECK REFUND EXPRESS UNIQUE by shipmentSerialNo:{}",
                    shipmentSerialNo);
        }
        Boolean result = refundReadLogic.checkShipInfoUnique(shipmentSerialNo);
        if (log.isDebugEnabled()) {
            log.debug("CHECK REFUND EXPRESS UNIQUE by shipmentSerialNo:{}, res: {}",
                    shipmentSerialNo, result);
        }
        return Response.ok(result);
    }

    /**
     * 修改换货数量
     *
     * @param editRefundShipmentItemsInfo 请求参数
     * @return true修改成功
     */
    @RequestMapping(value = "/api/refund/edit/change/items/quantity", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> editRefundExchangeItems(@RequestBody EditRefundShipmentItemsInfo editRefundShipmentItemsInfo) {
        Refund refund = refundReadLogic.findRefundById(editRefundShipmentItemsInfo.getRefundId());
        //获取当前的售后换货列表集合
        List<RefundItem> existRefundChangeItems = refundReadLogic.findRefundChangeItems(refund);
        //获取前端传过来的skuCode以及applyQuantity
        List<RefundItem> editRefundChangeItems = editRefundShipmentItemsInfo.getChangeItems();
        //组装成map
        Map<String, Integer> editSkuCodeAndApplyQuantityMap = editRefundChangeItems.stream().
                filter(Objects::nonNull).collect(Collectors.toMap(RefundItem::getSkuCode, RefundItem::getApplyQuantity));
        //将申请数量修改
        List<RefundItem> newRefundChangeItems = Lists.newArrayList();
        for (RefundItem refundItem : existRefundChangeItems) {
            if (editSkuCodeAndApplyQuantityMap.containsKey(refundItem.getSkuCode())) {
                refundItem.setApplyQuantity(editSkuCodeAndApplyQuantityMap.get(refundItem.getSkuCode()));
                //重新计算商品总净价，商品总进价等于商品净价*数量
                refundItem.setCleanFee(editSkuCodeAndApplyQuantityMap.get(refundItem.getSkuCode()) * refundItem.getCleanPrice());
            }
            newRefundChangeItems.add(refundItem);
        }
        Map<String, String> refundExtra = refund.getExtra();
        refundExtra.put(TradeConstants.REFUND_CHANGE_ITEM_INFO, JsonMapper.nonEmptyMapper().toJson(newRefundChangeItems));
        refund.setExtra(refundExtra);
        return refundWriteLogic.update(refund);
    }

    /**
     * 校验换货退货金额与换货金额是否相等，为了兼容历史数据，有可能历史数据不存在finalRefundQuantity，这个时候
     *
     * @param refundId
     * @return true表示金额一致，false表示金额不一致
     */
    @RequestMapping(value = "/api/refund/validate/fee/for/change", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> validateRefundFeeAndExchangeFee(@RequestParam Long refundId) {
        //如果是换货售后单，判断实际退货数量*价格是否等于换货数量*价格
        Refund refund = refundReadLogic.findRefundById(refundId);
        if (Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value())) {
            List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
            Integer refundFee = 0;
            //count计数器对于历史单据做兼容，因为历史单据可能不会存在finalRefundQuantity这个字段,此时就不需要校验实际退货金额与换货金额是否相等
            int count = 0;
            for (RefundItem refundItem : refundItems) {
                if (Objects.isNull(refundItem.getFinalRefundQuantity())) {
                    count++;
                    continue;
                }
                refundFee += refundItem.getFinalRefundQuantity() * refundItem.getCleanPrice();
            }
            if (count == 0) {
                List<RefundItem> refundChangeItems = refundReadLogic.findRefundChangeItems(refund);
                Integer changeFee = 0;
                for (RefundItem refundItem : refundChangeItems) {
                    changeFee += refundItem.getApplyQuantity() * refundItem.getCleanPrice();
                }
                if (!Objects.equals(refundFee, changeFee)) {
                    return Response.ok(Boolean.FALSE);
                }
            }
        }
        return Response.ok(Boolean.TRUE);
    }

    @RequestMapping(value = "/api/refund/express/fix", method = RequestMethod.GET)
    @ApiOperation(value = "检查退货物流信息修复")
    public void expressFix() {
        refundWriteLogic.expressFix();
    }

    /**
     * 换转退 换货改退货取消售后单
     *
     * @param id 售后单id
     */
    @RequestMapping(value = "/api/refund/{id}/exchange/to/refund", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("换货改退货")
    public Response<Boolean> exchangeToRefund(@PathVariable("id") @OperationLogParam Long id) {
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-CANCELREFUNDFORCHANGE-START param: id [{}]", id);
        }
        Refund originRefund = refundReadLogic.findRefundById(id);
        OpenShop openShop = orderReadLogic.findOpenShopByShopId(originRefund.getShopId());
        Map<String, String> openShopExtraMap = openShop.getExtra();
        RefundSource refundSource = refundReadLogic.findRefundSource(originRefund);
        if (!Objects.equals(refundSource.value(), RefundSource.THIRD.value())) {
            log.error("refund(id:{}) is manual party refund so cant not exchange to return", originRefund.getId());
            throw new JsonResponseException("manual.party.refund.can.not.exchange.to.return");
        }
        if (!Objects.equals(openShop.getChannel(), MiddleChannel.TAOBAO.getValue())) {
            log.error("openshop(channel:{}) is not taobao so cant not exchange to return", openShop.getChannel());
            throw new JsonResponseException("not.taobao.refund.can.not.exchange.to.return");
        }
        if (!openShopExtraMap.containsKey(TradeConstants.EXCHANGE_PULL) || !Objects.equals(openShopExtraMap.get(TradeConstants.EXCHANGE_PULL), "Y")) {
            log.error("openshop(id:{}) is not support exchange pull so cant not exchange to return", openShop.getId());
            throw new JsonResponseException("not.support.exchange.pull.can.not.exchange.to.return");
        }
        //查询天猫单个换货单详情
        OpenClientAfterSaleExchangeService afterSaleExchangeService = afterSaleExchangeServiceRegistryCenter.getAfterSaleExchangeService(openShop.getChannel());
        String outerId = originRefund.getOutId().substring(originRefund.getOutId().indexOf("_") + 1);
        Response<OpenClientAfterSale> openClientAfterSaleResponse = afterSaleExchangeService.findByAfterSaleId(originRefund.getShopId(), outerId);
        if (!openClientAfterSaleResponse.isSuccess()) {
            log.error("sync after sale by open shop id:{} outer after after id:{} fail,error:{}", originRefund.getShopId(), originRefund.getOutId(), openClientAfterSaleResponse.getError());
            throw new JsonResponseException(openClientAfterSaleResponse.getError());
        }
        OpenClientAfterSale openClientAfterSale = openClientAfterSaleResponse.getResult();

        if (Objects.equals(originRefund.getStatus(), MiddleRefundStatus.CHANGE_SYNC_HK_SUCCESS.getValue()) && !Objects.equals(openClientAfterSale.getStatus(), OpenClientAfterSaleStatus.EXCHANGE_CLOSED)) {
            throw new JsonResponseException("tmall.refund.not.close");
        }
        if (!Objects.equals(openClientAfterSale.getStatus(), OpenClientAfterSaleStatus.EXCHANGE_TO_REFUND) && !Objects.equals(openClientAfterSale.getStatus(), OpenClientAfterSaleStatus.EXCHANGE_CLOSED)) {//天猫换货状态不是 换货关闭，转退货退款（请退款）
            throw new JsonResponseException("tmall.refund.status.not.exchange.to.refund");
        }
        //售后单待发货
        if (Objects.equals(originRefund.getStatus(), MiddleRefundStatus.WAIT_SHIP.getValue())) {
            List<OrderShipment> orderShipments = shipmentReadLogic.findByAfterOrderIdAndType(id);
            List<OrderShipment> cancelIngList = orderShipments.stream().filter(Objects::nonNull)
                    .filter(orderShipment -> Objects.equals(orderShipment.getStatus(), MiddleShipmentsStatus.SYNC_HK_CANCEL_ING.getValue()))
                    .collect(Collectors.toList());//取消中的
            if (cancelIngList.size() > 0) {//发货单取消中
                throw new JsonResponseException("shipment.in.deal");
            }
            //售后单上记录换转退标记 便于后面做换转退操作
            Map<String, String> extraMap = originRefund.getExtra();
            extraMap.put(TradeConstants.EXCHANGE_REFUND, "Y");
            Refund update = new Refund();
            update.setId(originRefund.getId());
            update.setExtra(extraMap);
            Response<Boolean> updateExtraRes = refundWriteLogic.update(update);
            if (!updateExtraRes.isSuccess()) {
                log.error("update refund extraMap(refundId:{}) fail,error:{}", originRefund.getId(), updateExtraRes.getError());
            }
            List<OrderShipment> orderShipmentList = orderShipments.stream().filter(Objects::nonNull)
                    .filter(orderShipment -> !Objects.equals(orderShipment.getStatus(), MiddleShipmentsStatus.CANCELED.getValue()))
                    .collect(Collectors.toList());
            for (OrderShipment orderShipment : orderShipmentList) {
                log.info("try to cancel shipemnt, shipmentId is {}", orderShipment.getShipmentId());
                Response<Boolean> response = shipmentWiteLogic.rollbackShipment(orderShipment.getShipmentId());
                if (!response.isSuccess()) {
                    log.info("try to cancel shipment, shipmentId is {}", orderShipment.getShipmentId());
                    throw new JsonResponseException(response.getError());
                }
            }

            return Response.ok(Boolean.TRUE);
        }
        if (refundReadLogic.isExchangeTorefund(originRefund)) {
            //如果允许，则将占库发货单取消即可
            refundWriteLogic.cancelAfterSaleOccupyShipments(id);
            //换转退
            Response<Boolean> response = refundWriteLogic.exchangeToRefund(id);
            if (!response.isSuccess()) {
                log.info("try to exchange to refund fail, refundId is {}", id);
                throw new JsonResponseException(response.getError());
            }
        } else {
            throw new JsonResponseException("exchange.to.return.status.invalid");
        }
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-CANCELREFUNDFORCHANGE-END param: id [{}]", id);
        }
        return Response.ok(Boolean.TRUE);
    }


    //编辑售后单

    private EditMiddleRefund makeEditMiddleRefund(Long refundId) {
        //根据售后单id获取售后单信息
        Refund refund = refundReadLogic.findRefundById(refundId);
        //获取售后单订单关联信息
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refundId);
        //获取需要编辑的信息
        EditMiddleRefund editMiddleRefund = new EditMiddleRefund();
        editMiddleRefund.setIsToCreate(Boolean.FALSE);
        editMiddleRefund.setOrderRefund(orderRefund);
        editMiddleRefund.setRefund(refund);
        //获取售后单extra信息
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        if (refundExtra.getShipmentId() != null) {
            Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(refundExtra.getShipmentId());
            editMiddleRefund.setRefundItems(makeEditRefundItemFromRefund(refund, shipment.getId()));
        }
        editMiddleRefund.setRefundExtra(refundExtra);
        //如果为丢件补发类型
        if (isLostRefund(refund)) {
            //获取丢件补发需要发货的信息
            editMiddleRefund.setLostRefundItems(refundReadLogic.findRefundLostItems(refund));
            //获取丢件补发需要生成发货单的联系人信息
            editMiddleRefund.setMiddleChangeReceiveInfo(refundReadLogic.findMiddleChangeReceiveInfo(refund));
        }
        //如果为换货,则获取换货商品信息
        if (isChangeRefund(refund)) {
            //获取换货商品信息
            editMiddleRefund.setShipmentItems(refundReadLogic.findRefundChangeItems(refund));
            //获取换货的需要生成发货单的联系人信息
            editMiddleRefund.setMiddleChangeReceiveInfo(refundReadLogic.findMiddleChangeReceiveInfo(refund));
        }

        return editMiddleRefund;

    }

    private MiddleRefundDetail makeRefundDetail(Long refundId) {
        //查询售后单以及售后单关联表
        Refund refund = refundReadLogic.findRefundById(refundId);
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refundId);
        MiddleRefundDetail refundDetail = new MiddleRefundDetail();
        refundDetail.setOrderRefund(orderRefund);
        refundDetail.setRefund(refund);
        //查询售后单extra信息
        RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
        //查询售后商品
        refundDetail.setRefundItems(refundReadLogic.findRefundItems(refund));
        refundDetail.setRefundExtra(refundExtra);
        //如果为丢件补发，获取丢件补发的商品信息
        if (isLostRefund(refund)) {
            //获取补发商品资料
            refundDetail.setLostRefundItems(refundReadLogic.findRefundLostItems(refund));
            //获取换货的收货人地址
            refundDetail.setMiddleChangeReceiveInfo(refundReadLogic.findMiddleChangeReceiveInfo(refund));
        }
        //如果为换货,则获取换货商品信息
        if (isChangeRefund(refund)) {
            //查询换货商品资料
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

    //根据退货商品封装 EditRefundItem

    private List<EditRefundItem> makeEditRefundItemFromRefund(Refund refund, Long shipmentId) {
        List<RefundItem> refundItems = refundReadLogic.findRefundItems(refund);
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        List<ShipmentItem> shipmentItems = shipmentReadLogic.getShipmentItems(shipment);

        Map<Long, ShipmentItem> shipmentItemMap = shipmentItems.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(ShipmentItem::getSkuOrderId, it -> it));

        List<EditRefundItem> editRefundItems = Lists.newArrayListWithCapacity(shipmentItems.size());
        for (RefundItem refundItem : refundItems) {
            EditRefundItem editRefundItem = new EditRefundItem();
            BeanMapper.copy(refundItem, editRefundItem);
            ShipmentItem shipmentItem = shipmentItemMap.get(refundItem.getSkuOrderId());
            editRefundItem.setQuantity(shipmentItem.getQuantity());
            editRefundItem.setRefundQuantity(shipmentItem.getRefundQuantity());
            //商品id
            editRefundItem.setItemId(shipmentItem.getItemId());
            //商品属性
            List<SkuAttribute> attrs = shipmentItem.getAttrs();

            editRefundItem.setAttrs(attrs);
            editRefundItems.add(editRefundItem);
        }

        return editRefundItems;
    }

    private void waitShipItems(List<WaitShipItemInfo> waitShipItemInfos, RefundItem refundItem) {
        WaitShipItemInfo waitShipItemInfo = new WaitShipItemInfo();
        waitShipItemInfo.setSkuCode(refundItem.getSkuCode());
        waitShipItemInfo.setOutSkuCode(refundItem.getSkuCode());
        waitShipItemInfo.setSkuName(refundItem.getSkuName());
        waitShipItemInfo.setWaitHandleNumber(refundItem.getApplyQuantity() - (refundItem.getAlreadyHandleNumber() == null ? 0 : refundItem.getAlreadyHandleNumber()));
        waitShipItemInfo.setSkuAttrs(refundItem.getAttrs());
        waitShipItemInfo.setItemId(refundItem.getItemId());
        waitShipItemInfos.add(waitShipItemInfo);
    }

    private WarehouseDTO findWarehouseById(Long warehouseId) {

        Response<WarehouseDTO> warehouseRes = warehouseClient.findById(warehouseId);
        if (!warehouseRes.isSuccess()) {
            log.error("find warehouse by id:{} fail,error:{}", warehouseId, warehouseRes.getError());
            throw new JsonResponseException(warehouseRes.getError());
        }
        return warehouseRes.getResult();

    }

    private String getisNot325CompanyCode(Refund refund, Map<String,String> refundExtraMap){
        String refundCompanyIsNot325 = null;
        if(refundExtraMap.containsKey(TradeConstants.REFUND_EXTRA_INFO)){
            //获取售后单扩展信息
            String refundExtrajson = refundExtraMap.get(TradeConstants.REFUND_EXTRA_INFO);
            RefundExtra refundExtra = JsonMapper.nonEmptyMapper().fromJson(refundExtrajson,RefundExtra.class);
            // 发货单code(坑：字段定义为id，实际数据存的是SHP的code)
            String shipmentCode = refundExtra.getShipmentId();
            // 退货仓id
            Long warehouseid = refundExtra.getWarehouseId();
            // 获取该售后单对应发货单信息
            Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(shipmentCode);
            if (shipment != null){
                refundCompanyIsNot325 = checkShopOrWarehouse(shipment,refund,warehouseid);
            }
        }
        return refundCompanyIsNot325;
    }
    
    private String checkShopOrWarehouse(Shipment shipment,Refund refund,Long warehouseid){
            String refundCompanyIsNot325 = null;
            String shipmentExtrajson = shipment.getExtra().get(TradeConstants.SHIPMENT_EXTRA_INFO);
            ShipmentExtra shipmentExtra = JsonMapper.nonEmptyMapper().fromJson(shipmentExtrajson,ShipmentExtra.class);
            Long oriDeliverid = shipmentExtra.getWarehouseId();
            if(Objects.equals(shipment.getShipWay(), TradeConstants.MPOS_SHOP_DELIVER)){
                // 校验店发账套
                Response<OpenShop> openshopRes = openShopReadService.findById(oriDeliverid);
                if(openshopRes.isSuccess() && openshopRes.getResult() != null){
                    Map<String,String> shopExtraMap = openshopRes.getResult().getExtra();
                    if(!CollectionUtils.isEmpty(shopExtraMap) && shopExtraMap.containsKey(TradeConstants.HK_COMPANY_CODE)){
                       String companyCode = shopExtraMap.get(TradeConstants.HK_COMPANY_CODE);
                        if(Objects.equals(companyCode,TradeConstants.BAO_WEI_COMPANY_ID.toString())){
                            refundCompanyIsNot325 = addIsNot325Company(refund, warehouseid);
                        }
                    }
                }
                //查询失败，就过去吧 不处理
            }else if(Objects.equals(shipment.getShipWay(), TradeConstants.MPOS_WAREHOUSE_DELIVER)){
                // 校验仓发账套
                WarehouseDTO checkWarehousedto = findWarehouseById(oriDeliverid);
                if(checkWarehousedto != null && Objects.equals(checkWarehousedto.getCompanyId(),TradeConstants.BAO_WEI_COMPANY_ID.toString())){
                    refundCompanyIsNot325 = addIsNot325Company(refund, warehouseid);
                }
            }
        return refundCompanyIsNot325;
    }

    private String addIsNot325Company(Refund refund, Long warehouseid){
        String addrefundCompanyIsNot325 = null;
        WarehouseDTO checkWarehousedto = findWarehouseById(warehouseid);
        if (checkWarehousedto != null && !Objects.equals(checkWarehousedto.getCompanyId(),TradeConstants.BAO_WEI_COMPANY_ID.toString())){
            addrefundCompanyIsNot325 = refund.getRefundCode();
            log.error("warehouse of refund(id:{}) is not belong to 325", refund.getId());
        }        
        return addrefundCompanyIsNot325;
    }

    /**
     * 是否是换货
     *
     * @param refund
     * @return
     */
    private Boolean isChangeRefund(Refund refund) {
        return Objects.equals(refund.getRefundType(), MiddleRefundType.AFTER_SALES_CHANGE.value());
    }

    /**
     * 是否是丢件补发类型
     *
     * @param refund 售后单
     * @return
     */
    private Boolean isLostRefund(Refund refund) {
        return Objects.equals(refund.getRefundType(), MiddleRefundType.LOST_ORDER_RE_SHIPMENT.value());
    }


}
