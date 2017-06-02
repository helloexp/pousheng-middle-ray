package com.pousheng.erp;

import io.terminus.parana.common.banner.ParanaBanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Description: add something here
 * User: xiao
 * Date: 28/04/2017
 */
/*@SpringBootApplication
@EnableScheduling*/
public class PoushengErpApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(PoushengErpApplication.class);
        application.setBanner(new ParanaBanner());
        application.run(args);
    }

}
