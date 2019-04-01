package com.pousheng.middle.web;

import io.terminus.parana.common.banner.ParanaBanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-02
 */
@SpringBootApplication
public class MiddleApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(MiddleApplication.class);
        application.setBanner(new ParanaBanner());
        application.run(args);
    }
}
