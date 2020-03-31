package com.pousheng.middle.consume.index;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.pousheng.middle.consume.index.configuration.OrderSearchProperties;
import com.pousheng.middle.consume.index.configuration.RefundSearchProperties;
import com.pousheng.middle.consume.index.configuration.StockSendSearchProperties;
import io.terminus.search.core.ESClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-17 14:17<br/>
 */
@Configuration
public class SearchConfiguration {

    @Configuration
    @ConditionalOnClass(ESClient.class)
    public static class BaseSearchConfiguration {
        @Bean
        public ESClient esClient(@Value("${search.host}") String host,
                                 @Value("${search.port}") Integer port) {
            return new ESClient(host, port);
        }
    }

    @Configuration
    @ConditionalOnClass(ESClient.class)
    @ConditionalOnProperty(value = "enable.es.search", havingValue = "true", matchIfMissing = true)
    @ComponentScan({"io.terminus.search.api"})
    public static class ItemSearchConfiguration {
        @Bean
        @ConfigurationProperties(prefix = "order.search")
        public OrderSearchProperties orderSearchProperties() {
            return new OrderSearchProperties();
        }
        @Bean
        @ConfigurationProperties(prefix = "refund.search")
        public RefundSearchProperties refundSearchProperties() {
            return new RefundSearchProperties();
        }
        @Bean
        @ConfigurationProperties(prefix = "stock_send.search")
        public StockSendSearchProperties stockSendSearchProperties() {
            return new StockSendSearchProperties();
        }
    }

    @Slf4j
    @Configuration
    @ConditionalOnClass(ESClient.class)
    public static class InitialConfiguration {
        private final ESClient esClient;
        private final OrderSearchProperties orderSearchProperties;
        private final RefundSearchProperties refundSearchProperties;
        private final StockSendSearchProperties stockSendSearchProperties;

        public InitialConfiguration(ESClient esClient,
                                    OrderSearchProperties orderSearchProperties,
                                    RefundSearchProperties refundSearchProperties,
                                    StockSendSearchProperties stockSendSearchProperties) {
            this.esClient = esClient;
            this.orderSearchProperties = orderSearchProperties;
            this.refundSearchProperties = refundSearchProperties;
            this.stockSendSearchProperties = stockSendSearchProperties;
        }

        @PostConstruct
        public void setup() throws Exception {
            if (!this.esClient.health()) {
                log.warn("elasticsearch is not available ");
            } else {
                prepare(orderSearchProperties.getIndexName(), orderSearchProperties.getIndexType());
                prepare(refundSearchProperties.getIndexName(), refundSearchProperties.getIndexType());
                prepare(stockSendSearchProperties.getIndexName(), stockSendSearchProperties.getIndexType());
            }
        }

        private void prepare(String indexName, String indexType) throws IOException {
            this.esClient.createIndexIfNotExists(indexName);
            if (StringUtils.hasText(indexType)) {
                String mappingPath = indexType + "_mapping.json";
                String mapping = Resources.toString(Resources.getResource(mappingPath), Charsets.UTF_8);
                esClient.createMappingIfNotExists(indexName, indexType, mapping);
            }
        }
    }
}
