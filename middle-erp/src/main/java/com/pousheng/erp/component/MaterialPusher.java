package com.pousheng.erp.component;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.pousheng.erp.dao.mysql.SpuMaterialDao;
import com.pousheng.erp.model.SpuMaterial;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-17
 */
@Component
@Slf4j
public class MaterialPusher {

    private final ErpClient erpClient;

    private final SpuMaterialDao spuMaterialDao;

    @Autowired
    public MaterialPusher(ErpClient erpClient, SpuMaterialDao spuMaterialDao) {
        this.erpClient = erpClient;
        this.spuMaterialDao = spuMaterialDao;
    }

    /**
     * 添加新推送的spu
     *
     * @param spuIds spu id列表
     */
    public void addSpus(List<Long> spuIds){
        List<SpuMaterial> spuMaterials = spuMaterialDao.findBySpuIds(spuIds);
        Set<String> materialIds = Sets.newHashSet();
        for (SpuMaterial spuMaterial : spuMaterials) {
            materialIds.add(spuMaterial.getMaterialId());
        }
        erpClient.postJson("e-commerce-api/v1/create-material-mapper",
                JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(Lists.newArrayList(materialIds)));
    }

    /**
     * 不再推送这个spu
     *
     * @param spuIds spu id 列表
     */
    public void removeSpus(List<Long> spuIds){
        List<SpuMaterial> spuMaterials = spuMaterialDao.findBySpuIds(spuIds);
        Set<String> materialIds = Sets.newHashSet();
        for (SpuMaterial spuMaterial : spuMaterials) {
            materialIds.add(spuMaterial.getMaterialId());
        }
        erpClient.postJson("e-commerce-api/v1/remove-material-mapper",
                JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(Lists.newArrayList(materialIds)) );
    }
}
