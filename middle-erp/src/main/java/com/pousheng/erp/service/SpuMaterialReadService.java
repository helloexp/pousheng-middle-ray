package com.pousheng.erp.service;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.erp.dao.mysql.SpuMaterialDao;
import com.pousheng.erp.model.SpuMaterial;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * Author: jlchen
 * Desc: spu与material_id的关联读服务实现类
 * Date: 2017-06-23
 */
@Slf4j
@Service
public class SpuMaterialReadService {

    private final SpuMaterialDao spuMaterialDao;

    @Autowired
    public SpuMaterialReadService(SpuMaterialDao spuMaterialDao) {
        this.spuMaterialDao = spuMaterialDao;
    }

    public Response<SpuMaterial> findById(Long Id) {
        try {
            return Response.ok(spuMaterialDao.findById(Id));
        } catch (Exception e) {
            log.error("find spuMaterial by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("spu.material.find.fail");
        }
    }

    /**
     * 根据spuId查找关联的货品id列表
     *
     * @param spuId spuId
     * @return 关联的货品id列表
     */
    public Response<List<String>> findMaterialIdsBySpuId(Long spuId){
        try {
            List<SpuMaterial> spuMaterials = spuMaterialDao.findBySpuId(spuId);
            List<String> r = Lists.newArrayListWithCapacity(spuMaterials.size());
            for (SpuMaterial spuMaterial : spuMaterials) {
                r.add(spuMaterial.getMaterialId());
            }
            return Response.ok(r);
        } catch (Exception e) {
            log.error("failed to find materialIds by spuId={}, cause:{}", spuId,
                    Throwables.getStackTraceAsString(e));
            return Response.fail("spu.material.find.fail");
        }
    }
}
