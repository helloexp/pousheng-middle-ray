package com.pousheng.middle;

import com.pousheng.erp.ErpConfiguration;
import io.terminus.parana.ItemApiConfiguration;
import io.terminus.parana.ItemAutoConfig;
//import io.terminus.parana.TradeApiConfig;
//import io.terminus.parana.TradeAutoConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Description: add something here
 * User: xiao
 * Date: 28/04/2017
 */
@Configuration
@Import({ItemAutoConfig.class,
        ErpConfiguration.class,
        ItemApiConfiguration.class,
        /*TradeApiConfig.class,
        TradeAutoConfig.class*/})
@EnableScheduling
public class MiddleConfiguration {

}
