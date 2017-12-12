package com.pousheng.middle;

import io.terminus.parana.ItemAutoConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Description: Item Boot Configuration
 * User: xiao
 * Date: 20/04/2017
 */
@Configuration
@ComponentScan("com.pousheng.middle.shop.impl")
@Import({ItemAutoConfig.class})
public class PoushengMiddleItemConfiguration {

}
