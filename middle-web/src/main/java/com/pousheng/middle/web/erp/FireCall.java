package com.pousheng.middle.web.erp;

import com.google.common.collect.Lists;
import com.pousheng.erp.component.BrandImporter;
import com.pousheng.erp.component.SpuImporter;
import com.pousheng.middle.open.StockPusher;
import com.pousheng.middle.web.warehouses.component.WarehouseImporter;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
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
import java.util.List;
import java.util.Objects;

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

    private final DateTimeFormatter dft;
    @RpcConsumer
    private MappingReadService mappingReadService;
    @Autowired
    private StockPusher stockPusher;

    @Autowired
    public FireCall(SpuImporter spuImporter, BrandImporter brandImporter,
                    WarehouseImporter warehouseImporter) {
        this.spuImporter = spuImporter;
        this.brandImporter = brandImporter;
        this.warehouseImporter = warehouseImporter;

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

    @RequestMapping(value = "/spu/by/sku/code", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String synchronizeSpuByBarCode(@RequestParam String skuCode){
        int spuCount =spuImporter.processPullMarterials(skuCode);
        log.info("synchronized {} spus", spuCount);
        return "ok";
    }

    /**
     * 根据店铺id拉取基础货品信息
     * @param openShopId
     * @return
     */
    @RequestMapping(value = "/sku/code/by/shop", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String synchronizeSpuByBarCode(@RequestParam Long openShopId){
        int pageNo = 0;
        int pageSize= 40;
        while(true){
            Response<Paging<ItemMapping>> r =  mappingReadService.findByOpenShopId(openShopId,pageNo,pageSize);
            Paging<ItemMapping> itemMappingPaging = r.getResult();
            List<ItemMapping> itemMappingList = itemMappingPaging.getData();
            if (itemMappingList.isEmpty()){
                break;
            }
            for (ItemMapping itemMapping:itemMappingList){
                if (!Objects.equals(itemMapping.getStatus(),-1)){
                    int spuCount =spuImporter.processPullMarterials(itemMapping.getSkuCode());
                    log.info("synchronized {} spus", spuCount);
                }
            }
            pageNo++;
        }
        return "ok";
    }

    /**
     * 根据skuCode推送库存到第三方或者官网
     * @param skuCode
     * @return
     */
    @RequestMapping(value = "/sync/stock/by/sku/code", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String synchronizeStockBySkuCode(@RequestParam String skuCode){
        stockPusher.submit(Lists.newArrayList(skuCode));
        return "ok";
    }
}


