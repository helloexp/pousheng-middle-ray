package com.pousheng.erp.component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.pousheng.erp.dao.mysql.SpuMaterialDao;
import com.pousheng.erp.model.SpuMaterial;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.JsonMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
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
        String json = JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(new MaterialIds(Lists.newArrayList(materialIds)));
        log.info("add material to erp, data:{}", json);
        erpClient.postJson("common/erp/base/creatematerialmapper",
                json);
    }
    /**
     * 通知恒康该商品要进行实施库存同步
     */
    public void pushItemForStock(Long spuId){
        List<SpuMaterial> spuMaterials = spuMaterialDao.findBySpuId(spuId);
        Set<String> materialIds = Sets.newHashSet();
        for (SpuMaterial spuMaterial:spuMaterials){
            materialIds.add(spuMaterial.getMaterialId());
        }

        Map<String,String> map= Maps.newHashMap();
        map.put("material",Joiners.COMMA.join(materialIds));
        erpClient.get("common/erp/inv/getinstockcount",map);
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
        String json = JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(new MaterialIds(Lists.newArrayList(materialIds)));
        log.info("remove material from erp, data:{}", json);
        erpClient.postJson("common/erp/base/removematerialmapper",
                json);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MaterialIds implements Serializable{

        private static final long serialVersionUID = -5368312307537675586L;

        private List<String> material_lists;
    }
}
