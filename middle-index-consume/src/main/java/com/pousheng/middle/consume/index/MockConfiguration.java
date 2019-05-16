package com.pousheng.middle.consume.index;

import com.pousheng.middle.consume.index.mock.ShopOrderReadServiceMock;
import io.terminus.parana.order.service.ShopOrderReadService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-17 17:45<br/>
 */
@Configuration
@Profile("mock")
public class MockConfiguration {
    @Bean
    public ShopOrderReadService shopOrderReadService() {
        return new ShopOrderReadServiceMock();
    }
}
