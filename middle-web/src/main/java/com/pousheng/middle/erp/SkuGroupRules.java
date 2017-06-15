package com.pousheng.middle.erp;

import com.google.common.base.Throwables;
import com.pousheng.erp.model.SkuGroupRule;
import com.pousheng.erp.service.SkuGroupRuleService;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-02
 */
@RestController
@Slf4j
@RequestMapping("/api/skuRule")
public class SkuGroupRules {

    private final SkuGroupRuleService skuGroupRuleService;

    @Autowired
    public SkuGroupRules(SkuGroupRuleService skuGroupRuleService) {
        this.skuGroupRuleService = skuGroupRuleService;
    }

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long create(@RequestBody SkuGroupRule skuGroupRule){
        if(StringUtils.isEmpty(skuGroupRule.getCardId())){
            log.error("skuGroupRule cardId cannot be null");
            throw new JsonResponseException("cardId.is.null");
        }
        try {
            Response<Long> r = skuGroupRuleService.create(skuGroupRule);
            if(!r.isSuccess()){
                log.error("failed to create {}, error code:{} ", skuGroupRule, r.getError());
                throw new JsonResponseException(r.getError());
            }
            return r.getResult();
        } catch (Exception e) {
            log.error("failed to create {}, cause:{}", skuGroupRule, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("rule.create.fail");
        }

    }

    @RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean update(@RequestBody SkuGroupRule skuGroupRule){
        if(skuGroupRule.getId() == null){
            log.error("skuGroupRule id can not be null when update");
            throw new JsonResponseException("id.is.null");
        }
        Response<Boolean> r = skuGroupRuleService.update(skuGroupRule);
        if(!r.isSuccess()){
            log.error("failed to update {}, error code:{}", skuGroupRule, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return Boolean.TRUE;
    }

    @RequestMapping(value = "/{id}",method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean delete(@PathVariable Long id){

        Response<Boolean> r = skuGroupRuleService.delete(id);
        if(!r.isSuccess()){
            log.error("failed to delete skuGroupRule(id={}), error code:{}", id, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return Boolean.TRUE;
    }


    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<SkuGroupRule> findBy(@RequestParam(required = false) Integer pageNo,
                                       @RequestParam(required = false) Integer pageSize){
        Response<Paging<SkuGroupRule>> r = skuGroupRuleService.findBy(pageNo, pageSize);
        if(!r.isSuccess()){
            log.error("failed to paging skuGroup rules, error code:{}", r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    @RequestMapping(value = "/{id}",method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public SkuGroupRule findById(@PathVariable Long id){
        Response<SkuGroupRule> r = skuGroupRuleService.findById(id);
        if(!r.isSuccess()){
            log.error("failed to find skuGroupRule(id={}), error code:{}",id,  r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }
}
