package com.pousheng.erp;

import io.terminus.parana.common.banner.ParanaBanner;
import org.springframework.boot.SpringApplication;

/**
 * Description: add something here
 * User: xiao
 * Date: 28/04/2017
 */
/*@SpringBootApplication
@EnableScheduling*/
public class ErpApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ErpApplication.class);
        application.setBanner(new ParanaBanner());
        application.run(args);
    }

}
