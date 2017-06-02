package com.pousheng.erp;

import io.terminus.parana.ItemAutoConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Description: add something here
 * User: xiao
 * Date: 28/04/2017
 */
@Configuration
@Import(ItemAutoConfig.class)
@EnableScheduling
public class PoushengErpConfiguration {

}
