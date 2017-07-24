package com.pousheng.middle.web.item;

import com.pousheng.middle.web.item.rule.PoushengSpuSkuAttributeRule;
import io.terminus.parana.cache.BackCategoryCacher;
import io.terminus.parana.component.attribute.CategoryAttributeNoCacher;
import io.terminus.parana.rule.PipelineConfigurer;
import io.terminus.parana.rule.RuleExecutorRegistry;
import io.terminus.parana.rule.attribute.AttributeLiteralRule;
import io.terminus.parana.rule.attribute.SpuOtherAttributeRuleByCategoryExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-31
 */
@Component
public class PoushengPipelineConfigurer implements PipelineConfigurer {

    private final BackCategoryCacher backCategoryCacher;


    private final CategoryAttributeNoCacher categoryAttributeNoCacher;

    @Autowired
    public PoushengPipelineConfigurer(BackCategoryCacher backCategoryCacher,
                                      CategoryAttributeNoCacher categoryAttributeNoCacher) {
        this.backCategoryCacher = backCategoryCacher;
        this.categoryAttributeNoCacher = categoryAttributeNoCacher;
    }

    @Override
    public void configureRuleExecutors(RuleExecutorRegistry ruleExecutorRegistry) {
        ruleExecutorRegistry.register(new AttributeLiteralRule());
        ruleExecutorRegistry.register(new SpuOtherAttributeRuleByCategoryExecutor(backCategoryCacher,
                categoryAttributeNoCacher));
        ruleExecutorRegistry.register(new PoushengSpuSkuAttributeRule());
    }
}
