package com.pousheng.middle.erp;

import com.google.common.collect.Lists;
import com.pousheng.erp.component.SpuImporter;
import io.terminus.common.model.Response;
import io.terminus.open.client.item.dto.OpenClientBrand;
import io.terminus.open.client.parana.item.ParanaItemService;
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

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-31
 */
@RestController
@Slf4j
@RequestMapping("/api/task")
public class FireCall {

    private final SpuImporter spuImporter;

    private final DateTimeFormatter dft;

    private final ParanaItemService paranaItemService;

    @Autowired
    public FireCall(SpuImporter spuImporter,ParanaItemService paranaItemService) {
        this.spuImporter = spuImporter;
        this.paranaItemService = paranaItemService;

        DateTimeParser[] parsers = {
                DateTimeFormat.forPattern( "yyyy-MM-dd HH:mm:ss" ).getParser(),
                DateTimeFormat.forPattern( "yyyy-MM-dd" ).getParser() };
       dft = new DateTimeFormatterBuilder().append( null, parsers ).toFormatter();
    }

    @RequestMapping(value = "/spu", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String synchronizeSpu(@RequestParam String start,
                                 @RequestParam(name = "end", required = false)String end){

        log.info("begin to synchronize spu modified from {} to {}", start, end);
        Date from = dft.parseDateTime(start).toDate();
        Date to = null;
        if(StringUtils.hasText(end)){
            to = dft.parseDateTime(end).toDate();
        }
        spuImporter.process(from, to);
        log.info("finished to synchronize spu modified from {} to {} ", start, end);

        return "ok";
    }


    @RequestMapping(value = "/sync-brand", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> synBrand(){


        List<OpenClientBrand> openClientBrands = Lists.newArrayList();
        OpenClientBrand brand = new OpenClientBrand();
        brand.setId(2L);
        brand.setStatus(1);
        brand.setDescription("测试品牌描述322222");
        brand.setName("测试品牌3");
        brand.setLogo("logo");
        brand.setUniqueName("测试品牌3");
        openClientBrands.add(brand);
        return paranaItemService.syncBrands(openClientBrands);


    }
}
