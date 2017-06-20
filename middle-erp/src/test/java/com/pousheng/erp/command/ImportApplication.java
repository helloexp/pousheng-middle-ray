package com.pousheng.erp.command;

import io.terminus.parana.common.banner.ParanaBanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.test.context.ActiveProfiles;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-02
 */
@SpringBootApplication
public class ImportApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ImportApplication.class);
        application.setBanner(new ParanaBanner());
        application.run(args);
    }
}
