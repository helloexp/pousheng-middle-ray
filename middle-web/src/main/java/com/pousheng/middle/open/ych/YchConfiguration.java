package com.pousheng.middle.open.ych;

import com.pousheng.middle.open.ych.events.YchLoginListener;
import com.pousheng.middle.open.ych.logger.LogSender;
import com.pousheng.middle.open.ych.logger.events.LogListener;
import com.pousheng.middle.open.ych.token.TaobaoToken;
import com.pousheng.middle.open.ych.token.YchToken;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by cp on 9/16/17.
 */
@Configuration
@ConditionalOnProperty(value = "ych.enable", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({
        YchToken.class,
        TaobaoToken.class
})
@ComponentScan({"com.pousheng.middle.open.ych"})
public class YchConfiguration {

    @Bean
    public YchClient ychClient(YchToken token) {
        return new YchClient(token);
    }

    @Bean
    public YchApi ychApi(YchClient ychClient, TaobaoToken taobaoToken) {
        return new YchApi(ychClient, taobaoToken);
    }

    @Bean
    public YchLoginListener ychLoginListener() {
        return new YchLoginListener();
    }

    @Bean
    public LogSender ychLogSender(YchToken token){
        return new LogSender(token);
    }

    @Bean
    public LogListener ychLogListener(){
        return new LogListener();
    }

}
