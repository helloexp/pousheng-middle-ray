package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.RefundWarehouseRulesDao;
import com.pousheng.middle.order.model.RefundWarehouseRules;
import com.pousheng.middle.order.service.RefundWarehouseRulesWriteService;
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
public class RefundWarehouseRulesWriteServiceImpl implements RefundWarehouseRulesWriteService {

    @Autowired
    private RefundWarehouseRulesDao refundWarehouseRulesDao;

    @Override
    public Response<Boolean> deleteRulesById(Long id) {
        try{
            return Response.ok(refundWarehouseRulesDao.delete(id));
        }catch (Exception e){
            log.error("delete refundWarehouseRules failed, id:{}, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("delete.refundWarehouseRules.fail");
        }
    }

    @Override
    public Response<Long> createRefundWarehouseRules(RefundWarehouseRules rules) {
        try{
            refundWarehouseRulesDao.create(rules);
            return Response.ok(rules.getId());
        }catch (Exception e){
            log.error("create refundWarehouseRules failed, RefundWarehouseRules:{}, cause:{}", rules, Throwables.getStackTraceAsString(e));
            return Response.fail("create.refundWarehouseRules.fail");
        }
    }

    @Override
    public Response<Boolean> updateRefundWarehouseRules(RefundWarehouseRules rules) {
        try{
            return Response.ok(refundWarehouseRulesDao.update(rules));
        }catch (Exception e){
            log.error("update refundWarehouseRules failed, RefundWarehouseRules:{}, cause:{}", rules, Throwables.getStackTraceAsString(e));
            return Response.fail("update.refundWarehouseRules.fail");
        }
    }
}
