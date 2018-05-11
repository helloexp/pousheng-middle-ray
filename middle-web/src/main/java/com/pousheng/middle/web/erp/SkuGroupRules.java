package com.pousheng.middle.web.erp;

import com.google.common.base.Throwables;
import com.pousheng.erp.model.SkuGroupRule;
import com.pousheng.erp.service.SkuGroupRuleService;
import io.swagger.annotations.ApiOperation;
import io.terminus.applog.annotation.LogMe;
import io.terminus.applog.annotation.LogMeContext;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
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


    @LogMe(description = "创建SPU规则", compareTo = "skuGroupRuleDao#findById")
    @ApiOperation("创建")
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long create(@RequestBody @LogMeContext  SkuGroupRule skuGroupRule){
        String ruleStr = JsonMapper.nonEmptyMapper().toJson(skuGroupRule);
        if(log.isDebugEnabled()){
            log.debug("API-SKURULE-CREATE-START param: skuGroupRule [{}]",ruleStr);
        }
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
            if(log.isDebugEnabled()){
                log.debug("API-SKURULE-CREATE-END param: skuGroupRule [{}] ,resp: [{}]",ruleStr,r.getResult());
            }
            return r.getResult();
        } catch (Exception e) {
            log.error("failed to create {}, cause:{}", skuGroupRule, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("rule.create.fail");
        }

    }


    @LogMe(description = "更新SPU规则",ignore = true)
    @ApiOperation("更新")
    @RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean update(@RequestBody @LogMeContext  SkuGroupRule skuGroupRule){
        String ruleStr = JsonMapper.nonEmptyMapper().toJson(skuGroupRule);
        if(log.isDebugEnabled()){
            log.debug("API-SKURULE-UPDATE-START param: skuGroupRule [{}]",ruleStr);
        }
        if(skuGroupRule.getId() == null){
            log.error("skuGroupRule id can not be null when update");
            throw new JsonResponseException("id.is.null");
        }
        Response<Boolean> r = skuGroupRuleService.update(skuGroupRule);
        if(!r.isSuccess()){
            log.error("failed to update {}, error code:{}", skuGroupRule, r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-SKURULE-UPDATE-END param: skuGroupRule [{}] ,resp: [{}]",ruleStr,true);
        }
        return Boolean.TRUE;
    }

    @ApiOperation("删除SPU规则")
    @LogMe(description = "删除SPU规则",deleting = "skuGroupRuleDao#findById")
    @RequestMapping(value = "/{id}",method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean delete(@PathVariable @LogMeContext  Long id){
        if(log.isDebugEnabled()){
            log.debug("API-SKURULE-ID-START param: id [{}]",id);
        }
        Response<Boolean> r = skuGroupRuleService.delete(id);
        if(!r.isSuccess()){
            log.error("failed to delete skuGroupRule(id={}), error code:{}", id, r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-SKURULE-ID-END param: id [{}] ,resp: [{}] ",id,true);
        }
        return Boolean.TRUE;
    }


    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<SkuGroupRule> findBy(@RequestParam(required = false) Integer pageNo,
                                       @RequestParam(required = false) Integer pageSize){
        if(log.isDebugEnabled()){
            log.debug("API-SKURULE-FINDBY-START param: pageNo [{}] pageSize [{}]",pageNo,pageSize);
        }
        Response<Paging<SkuGroupRule>> r = skuGroupRuleService.findBy(pageNo, pageSize);
        if(!r.isSuccess()){
            log.error("failed to paging skuGroup rules, error code:{}", r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-SKURULE-FINDBY-END param: pageNo [{}] pageSize [{}] ,resp: [{}]",pageNo,pageSize,JsonMapper.nonEmptyMapper().toJson(r.getResult()));
        }
        return r.getResult();
    }

    @RequestMapping(value = "/{id}",method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public SkuGroupRule findById(@PathVariable Long id){
        if(log.isDebugEnabled()){
            log.debug("API-SKURULE-FINDBYID-START param: id [{}]",id);
        }
        Response<SkuGroupRule> r = skuGroupRuleService.findById(id);
        if(!r.isSuccess()){
            log.error("failed to find skuGroupRule(id={}), error code:{}",id,  r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-SKURULE-FINDBYID-END param: id [{}] ,resp: [{}]",id,JsonMapper.nonEmptyMapper().toJson(r.getResult()));
        }
        return r.getResult();
    }
}
