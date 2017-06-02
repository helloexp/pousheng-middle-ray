package com.pousheng.middle.erp;

import com.google.common.base.Throwables;
import com.pousheng.erp.dao.mysql.SkuGroupRuleDao;
import com.pousheng.erp.model.SkuGroupRule;
import io.terminus.common.exception.JsonResponseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-02
 */
@RestController
@Slf4j
@RequestMapping("/api/skuRule")
public class SkuGroupRules {

    private final SkuGroupRuleDao skuGroupRuleDao;

    @Autowired
    public SkuGroupRules(SkuGroupRuleDao skuGroupRuleDao) {
        this.skuGroupRuleDao = skuGroupRuleDao;
    }

    @RequestMapping(value = "/seller/items", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long create(@RequestBody SkuGroupRule skuGroupRule){
        if(StringUtils.isEmpty(skuGroupRule.getCardId())){
            log.error("skuGroupRule cardId cannot be null");
            throw new JsonResponseException("cardId.is.null");
        }
        try {
            skuGroupRuleDao.create(skuGroupRule);
            return skuGroupRule.getId();
        } catch (Exception e) {
            log.error("failed to create {}, cause:{}", skuGroupRule, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("rule.create.fail");
        }

    }

    @RequestMapping(value = "/seller/items", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean update(@RequestBody SkuGroupRule skuGroupRule){
        if(skuGroupRule.getId() == null){
            log.error("skuGroupRule id can not be null when update");
            throw new JsonResponseException("id.is.null");
        }
        SkuGroupRule existed = skuGroupRuleDao.findById(skuGroupRule.getId());
        if(existed == null){

        }
        return true;
    }
}
