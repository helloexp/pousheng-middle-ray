package com.pousheng.middle.web.erp;

import com.pousheng.erp.component.BrandImporter;
import com.pousheng.erp.component.MposWarehousePusher;
import com.pousheng.erp.component.SpuImporter;
import com.pousheng.middle.web.warehouses.component.WarehouseImporter;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-31
 */
@RestController
@Slf4j
@RequestMapping("/api/task")
public class FireCall {

    private final SpuImporter spuImporter;

    private final BrandImporter brandImporter;

    private final WarehouseImporter warehouseImporter;


    private final MposWarehousePusher mposWarehousePusher;


    private final DateTimeFormatter dft;


    @Autowired
    public FireCall(SpuImporter spuImporter, BrandImporter brandImporter,
                    WarehouseImporter warehouseImporter, MposWarehousePusher mposWarehousePusher) {
        this.spuImporter = spuImporter;
        this.brandImporter = brandImporter;
        this.warehouseImporter = warehouseImporter;
        this.mposWarehousePusher = mposWarehousePusher;

        DateTimeParser[] parsers = {
                DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").getParser(),
                DateTimeFormat.forPattern("yyyy-MM-dd").getParser()};
        dft = new DateTimeFormatterBuilder().append(null, parsers).toFormatter();
    }

    @RequestMapping(value = "/brand", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String synchronizeBrand(@RequestParam String start,
                                   @RequestParam(name = "end", required = false) String end){
        Date from = dft.parseDateTime(start).toDate();
        Date to = new Date();
        if (StringUtils.hasText(end)) {
            to = dft.parseDateTime(end).toDate();
        }
        int cardCount = brandImporter.process(from, to);
        log.info("synchronized {} brands", cardCount);
        return "ok";
    }


    @RequestMapping(value = "/spu", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String synchronizeSpu(@RequestParam String start,
                                 @RequestParam(name = "end", required = false) String end) {

        Date from = dft.parseDateTime(start).toDate();
        Date to = new Date();
        if (StringUtils.hasText(end)) {
            to = dft.parseDateTime(end).toDate();
        }
        //log.info("synchronize brand first");
        //int cardCount = brandImporter.process(from, to);
        //log.info("synchronized {} brands", cardCount);
        int spuCount =spuImporter.process(from, to);
        log.info("synchronized {} spus", spuCount);
        return "ok";
    }


    @RequestMapping(value="/warehouse", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String synchronizeWarehouse(@RequestParam String start,
                                       @RequestParam(name = "end", required = false) String end){
        Date from = dft.parseDateTime(start).toDate();
        Date to = new Date();
        if (StringUtils.hasText(end)) {
            to = dft.parseDateTime(end).toDate();
        }
        log.info("begin to synchronize warehouse from {} to {}", start, end);
        int warehouseCount = warehouseImporter.process(from, to);
        log.info("synchronized {} warehouses", warehouseCount);
        return "ok";

    }


    @RequestMapping(value="/add/mpos/warehouse", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String syncMposWarehouse(@RequestParam String companyId,
                                       @RequestParam String stockId){
        mposWarehousePusher.addWarehouses(companyId,stockId);
        return "ok";
    }

    @RequestMapping(value="/del/mpos/warehouse", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String delMposWarehouse(@RequestParam String companyId,
                                    @RequestParam String stockId){
        mposWarehousePusher.removeWarehouses(companyId,stockId);
        return "ok";
    }


}


