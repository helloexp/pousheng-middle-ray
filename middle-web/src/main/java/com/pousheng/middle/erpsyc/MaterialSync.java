package com.pousheng.middle.erpsyc;

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
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-30
 */
@Profile({"default"})
@Component
@Slf4j
public class MaterialSync implements CommandLineRunner {

    private final SpuImporter spuImporter;

    private final ErpBrandCacher brandCacher;


    @Autowired
    public MaterialSync(SpuImporter spuImporter, ErpBrandCacher brandCacher) {
        this.spuImporter = spuImporter;
        this.brandCacher = brandCacher;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            ObjectMapper mapper = JsonMapper.nonEmptyMapper().getMapper();
            Path dir = Paths.get("/Users/jlchen/Downloads/material");
            if (Files.exists(dir)) {
                try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(
                        dir)) {
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
            }else{
                log.info(dir.toString() + " not exist");
            }
        } catch (Exception e) {
            log.error("failed to sync material from erp ", e);
        }
    }
}
