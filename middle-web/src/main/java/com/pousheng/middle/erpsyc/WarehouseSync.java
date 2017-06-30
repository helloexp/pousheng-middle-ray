package com.pousheng.middle.erpsyc;

import com.pousheng.middle.web.warehouses.component.WarehouseImporter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 从恒康初始化同步warehouse
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-30
 */
@Profile({"sync"})
@Component
@Slf4j
public class WarehouseSync implements CommandLineRunner {

    private final WarehouseImporter warehouseImporter;

    @Autowired
    public WarehouseSync(WarehouseImporter warehouseImporter) {
        this.warehouseImporter = warehouseImporter;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            int count = warehouseImporter.process(DateTime.now().minusYears(1).toDate(), DateTime.now().toDate());
            log.info("sync {} warehouse successfully", count);
        } catch (Exception e) {
            log.error("failed to sync warehouse from erp", e);
        }
    }
}
