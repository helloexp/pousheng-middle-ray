package com.pousheng.middle.web.erp;

import com.pousheng.erp.component.BrandImporter;
import com.pousheng.erp.component.SpuImporter;
import com.pousheng.middle.web.warehouses.component.WarehouseImporter;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-28
 */
@Component
@Slf4j
public class JobTriggers {

    private final SpuImporter spuImporter;

    private final BrandImporter brandImporter;

    private final WarehouseImporter warehouseImporter;

    private final HostLeader hostLeader;

    @Autowired
    public JobTriggers(SpuImporter spuImporter,
                       BrandImporter brandImporter,
                       WarehouseImporter warehouseImporter,
                       HostLeader hostLeader) {
        this.spuImporter = spuImporter;
        this.brandImporter = brandImporter;
        this.warehouseImporter = warehouseImporter;
        this.hostLeader = hostLeader;
    }

    /**
     * 每天凌晨2点触发
     */
    @Scheduled(cron="0 0 2 * * ?")
    public void synchronizeSpu(){
        if(hostLeader.isLeader()) {
            log.info("JOB -- begin to sync information");
            Date from = DateTime.now().minusDays(1).withTimeAtStartOfDay().toDate();
            Date to = DateTime.now().withTimeAtStartOfDay().toDate();
            int cardCount = brandImporter.process(from, to);
            log.info("synchronized {} brands", cardCount);
            int spuCount = spuImporter.process(from, to);
            log.info("synchronized {} spus", spuCount);

            int warehouseCount =  warehouseImporter.process(from, to);

            log.info("synchronized {} warehouses", warehouseCount);
            log.info("JOB -- finish to sync information");
        }else{
            log.info("host is not leader, so skip job");
        }
    }
}
