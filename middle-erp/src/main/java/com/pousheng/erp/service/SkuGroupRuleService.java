package com.pousheng.erp.service;

import com.google.common.base.Throwables;
import com.pousheng.erp.dao.mysql.SkuGroupRuleDao;
import com.pousheng.erp.model.SkuGroupRule;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-02
 */
@Service
@Slf4j
public class SkuGroupRuleService {

    private final SkuGroupRuleDao skuGroupRuleDao;

    @Autowired
    public SkuGroupRuleService(SkuGroupRuleDao skuGroupRuleDao) {
        this.skuGroupRuleDao = skuGroupRuleDao;
    }

    public Response<Long> create(SkuGroupRule skuGroupRule){
        try{
            skuGroupRuleDao.create(skuGroupRule);
            return Response.ok(skuGroupRule.getId());
        }catch (Exception e){
            log.error("failed to create {}, cause:{}", skuGroupRule, Throwables.getStackTraceAsString(e));
            return Response.fail("skuGroupRule.create.fail");
        }
    }

    public Response<Boolean> update(SkuGroupRule skuGroupRule){
        try{
            SkuGroupRule existed = skuGroupRuleDao.findById(skuGroupRule.getId());
            if(existed == null){
                log.error("skuGroupRule(id={}) not exist", skuGroupRule.getId());
                return Response.fail("skuGroupRule.update.fail");
            }
            skuGroupRuleDao.update(skuGroupRule);
            return Response.ok(Boolean.TRUE);
        }catch (Exception e){
            log.error("failed to create {}, cause:{}", skuGroupRule, Throwables.getStackTraceAsString(e));
            return Response.fail("skuGroupRule.create.fail");
        }
    }

    public Response<Paging<SkuGroupRule>> findBy(Integer pageNo, Integer pageSize){
        try {
            PageInfo pageInfo = new PageInfo(pageNo, pageSize);
            return Response.ok(skuGroupRuleDao.paging(pageInfo.getOffset(), pageInfo.getLimit()));
        } catch (Exception e) {
            log.error("failed to paging skuGroupRule, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("skuGroupRule.find.fail");
        }
    }

    public Response<SkuGroupRule> findById(Long id){

        SkuGroupRule skuGroupRule = skuGroupRuleDao.findById(id);
        if(skuGroupRule == null){
            log.error("skuGroupRule(id={}) not found", id);
            return Response.fail("skuGroupRule.not.found");
        }
        return Response.ok(skuGroupRule);
    }

}
