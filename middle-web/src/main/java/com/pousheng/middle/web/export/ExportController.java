package com.pousheng.middle.web.export;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseHeadlessCriteria;
import com.pousheng.middle.order.dto.reverseLogistic.ReverseLogisticsDto;
import com.pousheng.middle.order.enums.HeadlessProcessTypeEnum;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import com.pousheng.middle.web.biz.impl.ExportTradeBillService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.eventbus.EventBus;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.MiddleOrderCriteria;
import com.pousheng.middle.order.dto.MiddleRefundCriteria;
import com.pousheng.middle.order.dto.OrderShipmentCriteria;
import com.pousheng.middle.order.dto.PoushengSettlementPosCriteria;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.impl.service.ShipmentReadExtraServiceImpl;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.events.trade.ExportTradeBillEvent;
import com.pousheng.middle.web.utils.export.FileRecord;
import com.pousheng.middle.web.utils.permission.PermissionUtil;

import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.service.ShipmentReadService;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by sunbo@terminus.io on 2017/7/20.
 */
@Slf4j
@RestController
@RequestMapping("api/")
@Api(description = "数据导出")
public class ExportController {

    @Autowired
    private ExportService exportService;

    @Autowired
    private PermissionUtil permissionUtil;

    @Autowired
    private EventBus eventBus;

    private static final JsonMapper MAPPER = JsonMapper.JSON_NON_EMPTY_MAPPER;
    @Autowired
    private CompensateBizLogic compensateBizLogic;
    @Autowired
    private ShipmentReadExtraServiceImpl shipmentReadExtraServiceImpl;
    @RpcConsumer
    private ShipmentReadService shipmentReadService;
    @Autowired
    private ExportTradeBillService exportTradeBillService;

    /**
     * 快递单号
     */
    private static final Integer MIN_LENGTH = 8;

    /**
     * 导出订单
     *
     * @param middleOrderCriteria 查询参数
     * @return
     */
    @GetMapping("order/export")
    public void orderExport(MiddleOrderCriteria middleOrderCriteria) {
        String criteriaStr = JsonMapper.nonEmptyMapper().toJson(middleOrderCriteria);
        if (log.isDebugEnabled()) {
            log.debug("API-ORDER-EXPORT-START param: middleOrderCriteria [{}] ]", criteriaStr);
        }

        if (middleOrderCriteria.getStatus() != null && middleOrderCriteria.getStatus().contains(99)) {
            throw new JsonResponseException("this status not support export");
        }

        //获取当前用户负责的商铺id
        List<Long> currentUserCanOperatShopIds = permissionUtil.getCurrentUserCanOperateShopIDs();
        if (middleOrderCriteria.getShopId() == null) {
            middleOrderCriteria.setShopIds(currentUserCanOperatShopIds);
        } else if (!currentUserCanOperatShopIds.contains(middleOrderCriteria.getShopId())) {
            throw new JsonResponseException("permission.check.query.deny");
        }
        ExportTradeBillEvent event = new ExportTradeBillEvent();
        event.setType(TradeConstants.EXPORT_ORDER);
        event.setCriteria(middleOrderCriteria);
        event.setUserId(UserUtil.getUserId());
        //换成Biz任务形式 modified by longjun.tlj
        String context = MAPPER.toJson(event);
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.EXPORT_TRADE_BILL.name());
        biz.setContext(context);
        biz.setBizId(TradeConstants.EXPORT_ORDER);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.name());
        compensateBizLogic.createBizAndSendMq(biz, MqConstants.POSHENG_MIDDLE_EXPORT_COMPENSATE_BIZ_TOPIC);
        if (log.isDebugEnabled()) {
            log.debug("API-ORDER-EXPORT-END param: middleOrderCriteria [{}] ]", criteriaStr);
        }
    }

    /**
     * 导出售后单
     *
     * @param criteria
     */
    @GetMapping("refund/export")
    public void refundExport(MiddleRefundCriteria criteria) {
        String criteriaStr = JsonMapper.nonEmptyMapper().toJson(criteria);
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-EXPORT-START param: criteria [{}] ]", criteriaStr);
        }
        //获取当前用户负责的商铺id
        List<Long> currentUserCanOperatShopIds = permissionUtil.getCurrentUserCanOperateShopIDs();
        if (criteria.getShopId() == null) {
            criteria.setShopIds(currentUserCanOperatShopIds);
        } else if (!currentUserCanOperatShopIds.contains(criteria.getShopId())) {
            throw new JsonResponseException("permission.check.query.deny");
        }
        ExportTradeBillEvent event = new ExportTradeBillEvent();
        event.setType(TradeConstants.EXPORT_REFUND);
        event.setCriteria(criteria);
        event.setUserId(UserUtil.getUserId());
        //换成Biz任务形式 modified by longjun.tlj
        String context = MAPPER.toJson(event);
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.EXPORT_TRADE_BILL.name());
        biz.setContext(context);
        biz.setBizId(TradeConstants.EXPORT_REFUND);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.name());
        compensateBizLogic.createBizAndSendMq(biz, MqConstants.POSHENG_MIDDLE_EXPORT_COMPENSATE_BIZ_TOPIC);
        if (log.isDebugEnabled()) {
            log.debug("API-REFUND-EXPORT-END param: criteria [{}] ]", criteriaStr);
        }
    }

    /**
     * 发货单导出
     */
    @GetMapping("shipment/export")
    public void shipmentExport(OrderShipmentCriteria criteria) {
        String criteriaStr = JsonMapper.nonEmptyMapper().toJson(criteria);
        if (log.isDebugEnabled()) {
            log.debug("API-SHIPMENT-EXPORT-START param: criteria [{}] ]", criteriaStr);
        }
        if (criteria.getEndAt() != null) {
            criteria.setEndAt(new DateTime(criteria.getEndAt().getTime()).plusDays(1).minusSeconds(1).toDate());
        }
        //补充 过滤条件 start
        if (!StringUtils.isEmpty(criteria.getShipmentSerialNo()) || criteria.getDispatchType() != null) {
            if (!StringUtils.isEmpty(criteria.getShipmentSerialNo()) && criteria.getDispatchType() != null) {
                if (criteria.getShipmentSerialNo().length() < MIN_LENGTH) {
                    throw new JsonResponseException("shipment.serial.no.too.short");
                }
                Response<List<Shipment>> response = shipmentReadExtraServiceImpl.findBySerialNoAndDispatchType(
                    criteria.getShipmentSerialNo(), criteria.getDispatchType());
                if (!response.isSuccess()) {
                    throw new JsonResponseException(response.getError());
                }
                List<Shipment> list = response.getResult();
                if (!list.isEmpty()) {
                    criteria.setShipmentIds(list.stream().map(Shipment::getId).collect(Collectors.toList()));
                }
            } else if (!StringUtils.isEmpty(criteria.getShipmentSerialNo())) {
                if (criteria.getShipmentSerialNo().length() < MIN_LENGTH) {
                    throw new JsonResponseException("shipment.serial.no.too.short");
                }
                Response<List<Shipment>> response = shipmentReadService.findBySerialNo(criteria.getShipmentSerialNo());
                if (!response.isSuccess()) {
                    throw new JsonResponseException(response.getError());
                }
                List<Shipment> list = response.getResult();
                if (!list.isEmpty()) {
                    criteria.setShipmentIds(list.stream().map(Shipment::getId).collect(Collectors.toList()));
                }
            } else {
                Response<List<Shipment>> response = shipmentReadExtraServiceImpl.findByDispatchType(
                    criteria.getDispatchType());
                if (!response.isSuccess()) {
                    throw new JsonResponseException(response.getError());
                }
                List<Shipment> list = response.getResult();
                if (!list.isEmpty()) {
                    criteria.setShipmentIds(list.stream().map(Shipment::getId).collect(Collectors.toList()));
                }
            }
        }
        if (!StringUtils.isEmpty(criteria.getWarehouseNameOrOutCode())) {
            Response<List<Shipment>> response;
            if (!StringUtils.isEmpty(criteria.getShipmentSerialNo()) || criteria.getDispatchType() != null) {
                response = shipmentReadExtraServiceImpl.findByWHNameAndWHOutCodeWithShipmentIds(
                    criteria.getWarehouseNameOrOutCode(), criteria.getShipmentIds());
            } else {
                response = shipmentReadExtraServiceImpl.findByWHNameAndWHOutCodeWithShipmentIds(
                    criteria.getWarehouseNameOrOutCode());
            }
            if (!response.isSuccess()) {
                throw new JsonResponseException(response.getError());
            }
            List<Shipment> list = response.getResult();
            if (!list.isEmpty()) {
                criteria.setShipmentIds(list.stream().map(Shipment::getId).collect(Collectors.toList()));
            }
        }
        //补充 过滤条件 end
        //获取当前用户负责的商铺id
        List<Long> currentUserCanOperatShopIds = permissionUtil.getCurrentUserCanOperateShopIDs();
        if (criteria.getShopId() == null) {
            criteria.setShopIds(currentUserCanOperatShopIds);
        } else if (!currentUserCanOperatShopIds.contains(criteria.getShopId())) {
            throw new JsonResponseException("permission.check.query.deny");
        }
        ExportTradeBillEvent event = new ExportTradeBillEvent();
        event.setType(TradeConstants.EXPORT_SHIPMENT);
        event.setCriteria(criteria);
        event.setUserId(UserUtil.getUserId());
        //换成Biz任务形式 modified by longjun.tlj
        String context = MAPPER.toJson(event);
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.EXPORT_TRADE_BILL.name());
        biz.setContext(context);
        biz.setBizId(TradeConstants.EXPORT_SHIPMENT);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.name());
        compensateBizLogic.createBizAndSendMq(biz, MqConstants.POSHENG_MIDDLE_EXPORT_COMPENSATE_BIZ_TOPIC);
        if (log.isDebugEnabled()) {
            log.debug("API-SHIPMENT-EXPORT-END param: criteria [{}] ]", criteriaStr);
        }
    }

    /**
     * pos单导出
     *
     * @param criteria
     */
    @GetMapping(value = "settlement/pos/export")
    public void exportSettlementPos(PoushengSettlementPosCriteria criteria) {
        String criteriaStr = JsonMapper.nonEmptyMapper().toJson(criteria);
        if (log.isDebugEnabled()) {
            log.debug("API-SETTLEMENT-POS-EXPORT-START param: criteria [{}] ]", criteriaStr);
        }
        List<Long> shopIds = permissionUtil.getCurrentUserCanOperateShopIDs();
        if (criteria.getShopId() != null && !shopIds.contains(criteria.getShopId())) {
            throw new JsonResponseException("permission.check.shop.id.empty");
        }
        if (criteria.getShopId() == null) {
            criteria.setShopIds(shopIds);
        }
        ExportTradeBillEvent event = new ExportTradeBillEvent();
        event.setType(TradeConstants.EXPORT_POS);
        event.setCriteria(criteria);
        event.setUserId(UserUtil.getUserId());
        //换成Biz任务形式 modified by longjun.tlj
        String context = MAPPER.toJson(event);
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.EXPORT_TRADE_BILL.name());
        biz.setContext(context);
        biz.setBizId(TradeConstants.EXPORT_POS);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.name());
        compensateBizLogic.createBizAndSendMq(biz, MqConstants.POSHENG_MIDDLE_EXPORT_COMPENSATE_BIZ_TOPIC);


        if (log.isDebugEnabled()) {
            log.debug("API-SETTLEMENT-POS-EXPORT-END param: criteria [{}] ]", criteriaStr);
        }
    }

    /**
     * 无头件pos单导出
     *
     * @param exportType 0 今日待处理 1 总共待处理   2.无头件退回 3.无头件盘盈
     * @param exportDate
     */
    @ApiOperation("无头件导出")
    @GetMapping(value = "headless/export")
    public void exportReverseHeadless(Integer exportType, Date exportDate) {

        if (log.isDebugEnabled()) {
            log.debug("API-REVERSE-HEADLESS-EXPORT-START param: exportType={},exportDate={}", exportType, exportDate);
        }
        ExportTradeBillEvent event = new ExportTradeBillEvent();
        event.setType(TradeConstants.EXPORT_REVERSE_HEADLESS);

        ReverseHeadlessCriteria reverseHeadlessCriteria = bulidReverseHeadlessCriteria(exportType, exportDate);
        event.setCriteria(reverseHeadlessCriteria);
        event.setUserId(UserUtil.getUserId());
        String context = MAPPER.toJson(event);
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.EXPORT_TRADE_BILL.name());
        biz.setContext(context);
        biz.setBizId(TradeConstants.EXPORT_REVERSE_HEADLESS);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.name());
        compensateBizLogic.createBizAndSendMq(biz, MqConstants.POSHENG_MIDDLE_EXPORT_COMPENSATE_BIZ_TOPIC);
        if (log.isDebugEnabled()) {
            log.debug("API-REVERSE-HEADLESS-EXPORT-END param: criteria [{}] ]", reverseHeadlessCriteria);
        }
    }

    /**
     * 无头件导出条件
     *
     * @return
     */
    private ReverseHeadlessCriteria bulidReverseHeadlessCriteria(Integer exportType, Date exportDate) {

        ReverseHeadlessCriteria reverseHeadlessCriteria = new ReverseHeadlessCriteria();
        Date startAt = null;
        Date endAt = null;
        if (exportType == 0) {
            startAt = DateTime.now().withTimeAtStartOfDay().toDate();
            endAt = null;
            reverseHeadlessCriteria.setStartAt(startAt);
            reverseHeadlessCriteria.setEndAt(endAt);
        } else {
            if(Objects.isNull(exportDate)){
                exportDate=new Date();
            }
            DateTime startTime = new DateTime(exportDate);
            startTime = startTime.withDayOfMonth(1).withTimeAtStartOfDay();
            DateTime endTime = startTime.plusMonths(1);
            startAt = startTime.toDate();
            endAt = endTime.toDate();
            if(exportType==1){
                reverseHeadlessCriteria.setStartAt(startAt);
                reverseHeadlessCriteria.setEndAt(endAt);
            }else{
            reverseHeadlessCriteria.setCloseEndAt(endAt);
            reverseHeadlessCriteria.setCloseStartAt(startAt);
            }
        }

        if (exportType == 0) {
            //今日新增无头件
            reverseHeadlessCriteria.setProcessType(HeadlessProcessTypeEnum.INIT.name());
        } else if (exportType == 1) {
            //待处理无头件总计
            reverseHeadlessCriteria.setProcessType(HeadlessProcessTypeEnum.INIT.name());
        } else if (exportType == 2) {
            //无头件退回
            reverseHeadlessCriteria.setProcessType(HeadlessProcessTypeEnum.REFUSE.name());
        } else if (exportType == 3) {
            //无头件盘盈
            reverseHeadlessCriteria.setProcessType(HeadlessProcessTypeEnum.PROFIT.name());
        }
        return reverseHeadlessCriteria;
    }

    /**
     * 导出文件记录
     *
     * @return
     */
    @GetMapping("export/files")
    public Response<Paging<FileRecord>> exportFiles() {
        if (log.isDebugEnabled()) {
            log.debug("API-EXPORT-FILES-START noparam: ");
        }
        List<FileRecord> files = exportService.getExportFiles();
        if (log.isDebugEnabled()) {
            log.debug("API-EXPORT-FILES-END noparam: ,resp: [{}]", JsonMapper.nonEmptyMapper().toJson(files));
        }
        return Response.ok(new Paging<FileRecord>((long)files.size(), files));
    }
}
