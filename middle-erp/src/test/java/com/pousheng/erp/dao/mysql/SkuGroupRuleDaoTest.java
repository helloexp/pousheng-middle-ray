package com.pousheng.erp.dao.mysql;

import com.pousheng.erp.dao.BaseDaoTest;
import com.pousheng.erp.model.SkuGroupRule;
import com.pousheng.erp.rules.SkuGroupRuler;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-24
 */
@ActiveProfiles("mysql")
public class SkuGroupRuleDaoTest extends BaseDaoTest {
    @Autowired
    private SkuGroupRuleDao skuGroupRuleDao;

    private SkuGroupRule skuGroupRule;

    @Before
    public void setUp() throws Exception {
        skuGroupRule = new SkuGroupRule();
        skuGroupRule.setCardId("11");
        skuGroupRule.setSplitChar('-');
        skuGroupRule.setRuleType(SkuGroupRuler.SPLIT.value);
        skuGroupRuleDao.create(skuGroupRule);
        assertThat(skuGroupRule.getId(), notNullValue());
    }

    @Test
    public void findByBrandId() throws Exception {
        List<SkuGroupRule> actual = skuGroupRuleDao.findByCardId(skuGroupRule.getCardId());
        assertThat(actual.size(), is(1));
        assertThat(actual.get(0).getId(),is(skuGroupRule.getId()));
    }

}