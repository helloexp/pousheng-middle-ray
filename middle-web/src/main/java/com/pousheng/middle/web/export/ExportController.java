package com.pousheng.middle.web.export;

import com.google.common.eventbus.EventBus;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.MiddleOrderCriteria;
import com.pousheng.middle.order.dto.MiddleRefundCriteria;
import com.pousheng.middle.order.dto.OrderShipmentCriteria;
import com.pousheng.middle.order.dto.PoushengSettlementPosCriteria;
import com.pousheng.middle.web.events.trade.ExportTradeBillEvent;
import com.pousheng.middle.web.utils.export.FileRecord;
import com.pousheng.middle.web.utils.permission.PermissionUtil;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.common.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by sunbo@terminus.io on 2017/7/20.
 */
@Slf4j
@RestController
@RequestMapping("api/")
public class ExportController {

    @Autowired
    private ExportService exportService;

    @Autowired
    private PermissionUtil permissionUtil;

    @Autowired
    private EventBus eventBus;


    /**
     * 导出订单
     *
     * @param middleOrderCriteria 查询参数
     * @return
     */
    @GetMapping("order/export")
    public void orderExport(MiddleOrderCriteria middleOrderCriteria) {
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
        eventBus.post(event);
    }

    /**
     * 导出售后单
     *
     * @param criteria
     */
    @GetMapping("refund/export")
    public void refundExport(MiddleRefundCriteria criteria) {
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
        eventBus.post(event);
    }


    /**
     * 发货单导出
     */
    @GetMapping("shipment/export")
    public void shipmentExport(OrderShipmentCriteria criteria) {
        if (criteria.getEndAt() != null) {
            criteria.setEndAt(new DateTime(criteria.getEndAt().getTime()).plusDays(1).minusSeconds(1).toDate());
        }
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
        eventBus.post(event);
    }

    /**
     * pos单导出
     * @param criteria
     */
    @GetMapping( value = "settlement/pos/export")
    public void exportSettlementPos(PoushengSettlementPosCriteria criteria){
        List<Long> shopIds =  permissionUtil.getCurrentUserCanOperateShopIDs();
        if (criteria.getShopId()!=null&&!shopIds.contains(criteria.getShopId())){
            throw new JsonResponseException("permission.check.shop.id.empty");
        }
        if (criteria.getShopId()==null){
            criteria.setShopIds(shopIds);
        }
        ExportTradeBillEvent event = new ExportTradeBillEvent();
        event.setType(TradeConstants.EXPORT_POS);
        event.setCriteria(criteria);
        event.setUserId(UserUtil.getUserId());
        eventBus.post(event);
    }

    /**
     * 导出文件记录
     * @return
     */
    @GetMapping("export/files")
    public Response<Paging<FileRecord>> exportFiles() {

        List<FileRecord> files = exportService.getExportFiles();

        return Response.ok(new Paging<FileRecord>((long) files.size(), files));
    }


}
