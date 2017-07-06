package com.pousheng.auth;

import io.terminus.parana.auth.impl.AuthAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Description: Item Boot Configuration
 * User: songrenfei
 * Date: 20/04/2017
 */
@Configuration
@ComponentScan
@Import({AuthAutoConfiguration.class})
public class AuthConfiguration {


}
