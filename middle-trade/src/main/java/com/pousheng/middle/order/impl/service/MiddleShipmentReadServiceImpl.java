package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.MiddleShipmentCriteria;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentRejectReason;
import com.pousheng.middle.order.enums.StockRecordType;
import com.pousheng.middle.order.impl.dao.ShipmentExtDao;
import com.pousheng.middle.order.impl.dao.StockRecordLogDao;
import com.pousheng.middle.order.impl.manager.MiddleShipmentManager;
import com.pousheng.middle.order.model.StockRecordLog;
import com.pousheng.middle.order.service.MiddleShipmentReadService;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 发货单读服务
 * @author tanlongjun
 */
@Slf4j
@Service
public class MiddleShipmentReadServiceImpl implements MiddleShipmentReadService {

    @Autowired
    private ShipmentExtDao shipmentExtDao;

    @Autowired
    private MiddleShipmentManager middleShipmentManager;
    @Autowired
    private StockRecordLogDao stockRecordLogDao;
    @Autowired
    private OpenShopReadService openShopReadService;

    @Override
    public Response<Paging<Shipment>> pagingShipment(MiddleShipmentCriteria criteria){
        try {

            Paging<Shipment> paging= shipmentExtDao.pagingExt(criteria.getOffset(),criteria.getLimit(),criteria.toMap());;
            return Response.ok(paging);
        } catch (Exception e) {
            log.error("failed to paging shipment, criteria={}, cause:{}",criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("shop.order.find.fail");
        }

    }



    @Override
    public Response<Paging<Shipment>> paging(Integer offset, Integer limit,String sort, Map<String, Object> criteria) {
        try{
            return Response.ok(middleShipmentManager.paging(offset,limit,sort, criteria));
        }catch (Exception e){
            log.error("failed to paging shipment.sort:{},criteria:{}",sort,criteria,e);
        }
        return Response.fail("failed to paging shipment");
    }

    /**
     * 分页查询拒单历史原因
     *
     * @param pageNo
     * @param pageSize
     * @param criteria
     * @return
     */
    @Override
    public Response<Paging<ShipmentRejectReason>> pagingRejectReason(Integer pageNo,
                                                                     Integer pageSize,
                                                                     Map<String, Object> criteria) {
        PageInfo pageInfo = new PageInfo(pageNo, pageSize);
        criteria.putAll(pageInfo.toMap());
        criteria.put("type", StockRecordType.MPOS_REFUSE_ORDER.toString());
        Paging<StockRecordLog> paging = stockRecordLogDao.paging(criteria);
        if (paging.isEmpty()) {
            return Response.ok(Paging.empty());
        }
        return Response.ok(new Paging<>(paging.getTotal(), paging.getData().stream().map(stockRecordLog -> {
            ShipmentRejectReason reason = new ShipmentRejectReason();
            reason.setShipmentId(stockRecordLog.getShipmentId());
            //销售店铺
            Response<OpenShop> openShop = openShopReadService.findById(stockRecordLog.getShopId());
            reason.setShopId(stockRecordLog.getShopId());
            reason.setShopName(openShop.getResult().getShopName());
            reason.setShipmentId(stockRecordLog.getShipmentId());
            reason.setSkuCode(stockRecordLog.getSkuCode());
            Shipment shipment = shipmentExtDao.findById(stockRecordLog.getShipmentId());
            if (shipment != null) {
                String shipmentExtrajson = shipment.getExtra().get(TradeConstants.SHIPMENT_EXTRA_INFO);
                ShipmentExtra shipmentExtra = JsonMapper.nonEmptyMapper().fromJson(shipmentExtrajson, ShipmentExtra.class);
                reason.setRejectReason(shipmentExtra.getRejectReason());
                reason.setShipmentCode(shipment.getShipmentCode());
            }
            reason.setRejectAt(stockRecordLog.getCreatedAt());
            //接单店仓
            reason.setWarehouseId(stockRecordLog.getWarehouseId());
            return reason;
        }).collect(Collectors.toList())));
    }

    @Override
    public Response<Paging<Shipment>> findShipmentCustom(Integer status, Date updatedAtStart, Date
            updatedAtEnd, Long limit, Integer offset) {
        try {
            return Response.ok(shipmentExtDao.findShipmentCustom(status, updatedAtStart, updatedAtEnd, limit, offset));
        } catch (Exception e) {
            log.error("failed to find shipment,status:{}, updatedAtStart:{}, updatedAtEnd:{}", status, updatedAtStart, updatedAtEnd, e);
        }
        return Response.fail("failed to find shipment");
    }
}
