package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.RefundWarehouseRulesDao;
import com.pousheng.middle.order.model.RefundWarehouseRules;
import com.pousheng.middle.order.service.RefundWarehouseRulesReadService;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: wenchao.he
 * Desc: 售后退货指定退货仓规则
 * Date: 2019/8/27
 */
@Slf4j
@Service
public class RefundWarehouseRulesReadServiceImpl implements RefundWarehouseRulesReadService {
    
    @Autowired
    private RefundWarehouseRulesDao refundWarehouseRulesDao;

    @Override
    public Response<RefundWarehouseRules> findRulesById(Long id) {
        try {
            return Response.ok(refundWarehouseRulesDao.findById(id));
        } catch (Exception e){
            log.error("find refundWarehouseRules by id failed, refundWarehouseRulesId:{}, cause:{}", id,
                    Throwables.getStackTraceAsString(e));
            return Response.fail("refundWarehouseRules.find.fail");
        }       
    }

    @Override
    public Response<RefundWarehouseRules> findByShopIdAndShipmentCompanyId(Long shopId, String shipmentCompanyId) {
        try {
            return Response.ok(refundWarehouseRulesDao.findByShopIdAndShipmentCompanyId(shopId,shipmentCompanyId));
        }catch (Exception e){
            log.error("find refundWarehouseRules by companyId failed, shopId:{}, ShipmentCompanyId:{}, cause:{}", shopId,
                    shipmentCompanyId, Throwables.getStackTraceAsString(e));
            return Response.fail("refundWarehouseRules.find.fail.by.companyId");
        }        
    }

    @Override
    public Response<Paging<RefundWarehouseRules>> pagingRules(Integer offset, Integer limit, RefundWarehouseRules criteria) {
        try {
            return Response.ok(refundWarehouseRulesDao.paging(offset,limit,criteria));    
        }catch (Exception e){
            log.error("find pagingRefundWarehouseRules failed, RefundWarehouseRules:{}, cause:{}", criteria,
                    Throwables.getStackTraceAsString(e));
            return Response.fail("pagingRefundWarehouseRules.find.fail");
        }        
    }
}
