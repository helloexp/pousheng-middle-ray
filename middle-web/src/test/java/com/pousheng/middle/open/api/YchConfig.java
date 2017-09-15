package com.pousheng.middle.open.api;

import com.pousheng.middle.open.ych.token.TaobaoToken;
import com.pousheng.middle.open.ych.token.YchToken;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by cp on 9/15/17.
 */
@Configuration
@EnableConfigurationProperties({
        TaobaoToken.class,
        YchToken.class
})
@ComponentScan({"com.pousheng.middle.open.ych"})
public class YchConfig {
}
