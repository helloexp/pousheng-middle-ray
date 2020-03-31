package com.pousheng.erp;

import io.terminus.parana.ItemAutoConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Description: add something here
 * User: xiao
 * Date: 28/04/2017
 */
@Configuration
@ComponentScan
@Import(ItemAutoConfig.class)
public class ErpConfiguration {

}
