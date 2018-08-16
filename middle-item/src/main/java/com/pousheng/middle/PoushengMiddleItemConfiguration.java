package com.pousheng.middle;

import com.pousheng.middle.item.SearchSkuTemplateProperties;
import com.pousheng.middle.item.SearchStockLogProperties;
import com.pousheng.middle.item.impl.service.DefaultIndexedSkuTemplateFactory;
import com.pousheng.middle.item.impl.service.DefaultIndexedSkuTemplateGuarder;
import com.pousheng.middle.item.service.IndexedSkuTemplateFactory;
import com.pousheng.middle.item.service.IndexedSkuTemplateGuarder;
import io.terminus.parana.ItemAutoConfig;
import io.terminus.parana.cache.BackCategoryCacher;
import io.terminus.parana.cache.BrandCacher;
import io.terminus.search.core.ESClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Description: Item Boot Configuration
 * User: xiao
 * Date: 20/04/2017
 */
@Configuration
@ComponentScan({"com.pousheng.middle.shop.impl","com.pousheng.middle.task.impl","com.pousheng.middle.category.impl","com.pousheng.middle.group.impl","com.pousheng.middle.item","io.terminus.search.api","com.pousheng.middle.shop.cacher"})
@Import({ItemAutoConfig.class})
@EnableConfigurationProperties({
        SearchSkuTemplateProperties.class,
        SearchStockLogProperties.class
})
public class PoushengMiddleItemConfiguration {

    @Configuration
    @ConditionalOnClass(ESClient.class)
    public static class BaseSearchConfiguration{
        @Bean
        public ESClient esClient(@Value("${search.host:localhost}") String host,
                                 @Value("${search.port:9200}") Integer port) {
            return new ESClient(host, port);
        }

        @Bean
        @ConditionalOnMissingBean(IndexedSkuTemplateGuarder.class)
        public IndexedSkuTemplateGuarder indexedSkuTemplateGuarder() {
            return new DefaultIndexedSkuTemplateGuarder();
        }

        @Configuration
        @ConditionalOnMissingBean(IndexedSkuTemplateFactory.class)
        protected static class IndexItemFactoryConfiguration {
            @Bean
            public IndexedSkuTemplateFactory indexedSkuTemplateFactory(
                    BackCategoryCacher backCategoryCacher,
                    BrandCacher brandCacher) {
                return new DefaultIndexedSkuTemplateFactory(backCategoryCacher, brandCacher);
            }
        }


    }



}
