package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.dto.PoushengSettlementPosCriteria;
import com.pousheng.middle.order.impl.dao.PoushengSettlementPosDao;
import com.pousheng.middle.order.model.PoushengSettlementPos;
import com.pousheng.middle.order.service.PoushengSettlementPosReadService;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.common.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/21
 * pousheng-middle
 */

@Slf4j
@Service
public class PoushengSettlementPosReadServiceImpl implements PoushengSettlementPosReadService{
    @Autowired
    private PoushengSettlementPosDao poushengSettlementPosDao;

    @Override
    public Response<Paging<PoushengSettlementPos>> paging(PoushengSettlementPosCriteria criteria) {
        try {
            //日期处理
            handleDate(criteria);
            //分页查询
            Paging<PoushengSettlementPos> paging = poushengSettlementPosDao.paging(criteria.getOffset(),criteria.getLimit(),criteria.toMap());
            return Response.ok(paging);
        } catch (Exception e) {
            log.error("failed to paging settlement pos, criteria={}, cause:{}",criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("settlement.pos.find.fail");
        }
    }

    @Override
    public Response<PoushengSettlementPos> findByPosSerialNo(String posSerialNo) {
        try {
            return Response.ok(poushengSettlementPosDao.findByPosSerialNo(posSerialNo));
        }catch (Exception e){
            log.error("failed find settlement pos, posSerialNo={}, cause:{}",posSerialNo, Throwables.getStackTraceAsString(e));
            return Response.fail("settlement.pos.find.fail");
        }
    }

    @Override
    public Response<PoushengSettlementPos> findByShipmentId(Long shipmentId) {
        try {
            return Response.ok(poushengSettlementPosDao.findByShipmentId(shipmentId));
        }catch (Exception e){
            log.error("failed find settlement pos, shipmentId={}, cause:{}",shipmentId, Throwables.getStackTraceAsString(e));
            return Response.fail("settlement.pos.find.fail");
        }
    }

    @Override
    public Response<PoushengSettlementPos> findByRefundIdAndPosType(Long refundId, Integer posType) {
        try {
            return Response.ok(poushengSettlementPosDao.findByRefundIdAndPosType(refundId,posType));
        }catch (Exception e){
            log.error("failed find settlement pos, refundId={}, cause:{}",refundId, Throwables.getStackTraceAsString(e));
            return Response.fail("settlement.pos.find.fail");
        }
    }


    private void handleDate(PoushengSettlementPosCriteria criteria) {
        if (criteria.getStartAt() != null) {
            criteria.setStartAt(DateUtil.withTimeAtStartOfDay(criteria.getStartAt()));
        }
        if (criteria.getEndAt() != null) {
            criteria.setEndAt(DateUtil.withTimeAtEndOfDay(criteria.getEndAt()));
        }
    }
}
