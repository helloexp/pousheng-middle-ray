package com.pousheng.middle.web.erp;

import com.pousheng.erp.component.BrandImporter;
import com.pousheng.erp.component.MposWarehousePusher;
import com.pousheng.erp.component.SpuImporter;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.web.warehouses.component.WarehouseImporter;
import io.terminus.common.utils.Splitters;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
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
import java.util.List;

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

    private final QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;


    private final DateTimeFormatter dft;


    @Autowired
    public FireCall(SpuImporter spuImporter, BrandImporter brandImporter,
                    WarehouseImporter warehouseImporter, MposWarehousePusher mposWarehousePusher, QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi) {
        this.spuImporter = spuImporter;
        this.brandImporter = brandImporter;
        this.warehouseImporter = warehouseImporter;
        this.mposWarehousePusher = mposWarehousePusher;
        this.queryHkWarhouseOrShopStockApi = queryHkWarhouseOrShopStockApi;

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
    public Boolean synchronizeWarehouse(@RequestParam(name = "start",required = false,defaultValue = "") String start,
                                         @RequestParam(name = "end", required = false,defaultValue = "") String end){
        Date from= DateTime.now().withTimeAtStartOfDay().toDate();
        if (StringUtils.hasText(start)){
             from = dft.parseDateTime(start).toDate();
        }
        Date to = DateTime.now().withTimeAtStartOfDay().plusDays(1).minusSeconds(1).toDate();
        if (StringUtils.hasText(end)) {
            to = dft.parseDateTime(end).toDate();
        }
        log.info("begin to synchronize warehouse from {} to {}", from, to);
        int warehouseCount = warehouseImporter.process(from, to);
        log.info("synchronized {} warehouses", warehouseCount);
        return Boolean.TRUE;

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

    @RequestMapping(value = "/spu/by/sku/code", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String synchronizeSpuByBarCode(@RequestParam String skuCode){
        int spuCount =spuImporter.processPullMarterials(skuCode);
        log.info("synchronized {} spus", spuCount);
        return "ok";
    }

    @RequestMapping(value="/query/stock", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<HkSkuStockInfo> queryStock(@RequestParam String stockCodes,
                                           @RequestParam String skuCodes,
                                           @RequestParam Integer stockType){
        List<String> stockCodesList = Splitters.COMMA.splitToList(stockCodes);
        List<String> skuCodesList = Splitters.COMMA.splitToList(skuCodes);
        return queryHkWarhouseOrShopStockApi.doQueryStockInfo(stockCodesList,skuCodesList,stockType);
    }


}


