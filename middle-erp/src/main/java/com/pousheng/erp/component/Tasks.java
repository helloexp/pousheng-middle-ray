package com.pousheng.erp.component;

import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-31
 */
@Component
@Slf4j
public class Tasks {

    private final SpuImporter spuImporter;

    private final DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public Tasks(SpuImporter spuImporter) {
        this.spuImporter = spuImporter;
    }

    /**
     * 每天凌晨2点触发
     */
    @Scheduled(cron="0 0 2 * * ?")
    public void fire(){
        DateTime start = DateTime.now().minus(1).withTimeAtStartOfDay();
        log.info("begin to process material updated from {}", dtf.print(start));
        spuImporter.process(start.toDate(),null);
        log.info("finish to process material updated from {}", dtf.print(start));
    }
}
