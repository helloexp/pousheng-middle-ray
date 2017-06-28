package com.pousheng.erp.component;

import com.pousheng.erp.model.PoushengCard;
import com.pousheng.erp.model.PoushengMaterial;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.Date;
import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-26
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan({
        "com.pousheng.erp"
})
public class ServiceConfiguration {

    @Bean
    public SpuInfoFetcher<PoushengMaterial> spuInfoFetcher(){
        return new SpuInfoFetcher<PoushengMaterial>() {
            public List<PoushengMaterial> fetch(int pageNo, int pageSize, Date start, Date end) {
                return null;
            }
        };
    }

    @Bean
    public CardFetcher cardFetcher(){
        return new CardFetcher() {
            @Override
            public List<PoushengCard> fetch(int pageNo, int pageSize, Date start, Date end) {
                return null;
            }
        };
    }
}
