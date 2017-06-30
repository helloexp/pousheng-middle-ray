package com.pousheng.middle.erpsyc;

import com.pousheng.erp.component.BrandImporter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 与恒康同步品牌信息
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-30
 */
@Profile({"sync"})
@Component
@Slf4j
public class CardSync implements CommandLineRunner{

    private final BrandImporter brandImporter;

    @Autowired
    public CardSync(BrandImporter brandImporter) {
        this.brandImporter = brandImporter;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            int count = brandImporter.process(DateTime.now().minusYears(1).toDate(), DateTime.now().toDate());
            log.info("sync {} brands from erp successfully", count);
        } catch (Exception e) {
            log.error("failed to sync brands from erp ", e);
        }
    }
}
