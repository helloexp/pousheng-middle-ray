package com.pousheng.erp.component;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

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
}
