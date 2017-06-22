package com.pousheng.erp.command;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.pousheng.erp.cache.ErpBrandCacher;
import com.pousheng.erp.component.SpuImporter;
import com.pousheng.erp.model.PoushengMaterial;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.brand.model.Brand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-20
 */
@Component
@Slf4j
public class SpuImporterFromFile implements CommandLineRunner{

    public static final ObjectMapper mapper = JsonMapper.JSON_NON_EMPTY_MAPPER.getMapper();


    private final SpuImporter spuImporter;

    private final ErpBrandCacher brandCacher;

    @Autowired
    public SpuImporterFromFile(SpuImporter spuImporter, ErpBrandCacher brandCacher) {
        this.spuImporter = spuImporter;
        this.brandCacher = brandCacher;
    }

    public void run(String... args) throws Exception {
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(
                Paths.get("/Users/jlchen/Downloads/product-data/material"))) {
            for (Path path : directoryStream) {
                if (path.toString().endsWith(".txt")) {
                    log.info("process {}", path.toAbsolutePath());
                    List<String> inputs = Files.readAllLines(path, Charsets.UTF_8);

                    JsonNode root = mapper.readTree(Joiner.on("").join(inputs));
                    boolean success = root.findPath("retCode").asInt() == 0;
                    if (!success) {
                        log.error(root.findPath("retMessage").textValue());
                        return;
                    }
                    List<PoushengMaterial> poushengMaterials = mapper.readValue(root.findPath("list").toString(),
                            new TypeReference<List<PoushengMaterial>>() {
                            });
                    for (PoushengMaterial poushengMaterial : poushengMaterials) {
                        String cardId = poushengMaterial.getCard_id();
                        Brand brand = brandCacher.findByOuterId(cardId);
                        spuImporter.doProcess(poushengMaterial, brand);
                    }
                }
            }
        }
    }
}
