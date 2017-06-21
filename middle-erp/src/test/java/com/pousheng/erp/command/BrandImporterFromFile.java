package com.pousheng.erp.command;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.io.Files;
import com.pousheng.erp.component.BrandImporter;
import com.pousheng.erp.model.PoushengCard;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;

import java.io.File;
import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-20
 */
//@Component
@Slf4j
public class BrandImporterFromFile implements CommandLineRunner {

    public static final ObjectMapper mapper = JsonMapper.JSON_NON_EMPTY_MAPPER.getMapper();


    private final BrandImporter brandImporter;

//    private final SpuImporter spuImporter;

    @Autowired
    public BrandImporterFromFile(BrandImporter brandImporter/*, SpuImporter spuImporter*/) {
        this.brandImporter = brandImporter;
//        this.spuImporter = spuImporter;
    }

    public void run(String... args) throws Exception {
        List<String> inputs = Files.readLines(new File("/Users/jlchen/Downloads/product-data2/brand.txt"),
                Charsets.UTF_8);
        JsonNode root = mapper.readTree(Joiner.on("").join(inputs));
        boolean success = root.findPath("retCode").asInt() == 0;
        if (!success) {
            log.error(root.findPath("retMessage").textValue());
            return;
        }
        List<PoushengCard> poushengCards = mapper.readValue(root.findPath("list").toString(),
                new TypeReference<List<PoushengCard>>() {
                });
        brandImporter.doProcess(poushengCards);
    }

    // public
}
