package com.pousheng.middle.warehouse;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-09
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan({
        "com.pousheng.middle.warehouse"
})
public class ServiceConfiguration {
}
