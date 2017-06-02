package com.pousheng.erp.rules;

import com.pousheng.erp.model.SkuGroupRule;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-23
 */
public class SkuGroupRulerTest {

    @Test
    public void splitSpuCode() throws Exception {
        SkuGroupRule skuGroupRule = new SkuGroupRule();
        skuGroupRule.setCardId("1");
        skuGroupRule.setRuleType(1);
        skuGroupRule.setSplitChar('-');

        boolean support = SkuGroupRuler.SPLIT.support(skuGroupRule, skuGroupRule.getCardId(), "11");

        assertThat(support,is(true));

        String spuCode = SkuGroupRuler.SPLIT.spuCode(skuGroupRule, "10001019-A03");
        assertThat(spuCode, is("10001019"));
    }

    @Test
    public void indexSpuCode() throws Exception {
        SkuGroupRule skuGroupRule = new SkuGroupRule();
        skuGroupRule.setCardId("1");
        skuGroupRule.setRuleType(2);
        skuGroupRule.setLastStart(3);

        boolean support = SkuGroupRuler.INDEX.support(skuGroupRule, skuGroupRule.getCardId(), "11");
        assertThat(support, is(true));

        String spuCode = SkuGroupRuler.INDEX.spuCode(skuGroupRule, "1228803100");
        assertThat(spuCode, is("1228803"));
    }
}