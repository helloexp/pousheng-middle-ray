package com.pousheng.erp.service;

import com.google.common.base.Throwables;
import com.pousheng.erp.dao.mysql.SpuMaterialDao;
import com.pousheng.erp.model.SpuMaterial;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: jlchen
 * Desc: spu与material_id的关联写服务实现类
 * Date: 2017-06-23
 */
@Slf4j
@Service
public class SpuMaterialWriteService {

    private final SpuMaterialDao spuMaterialDao;

    @Autowired
    public SpuMaterialWriteService(SpuMaterialDao spuMaterialDao) {
        this.spuMaterialDao = spuMaterialDao;
    }

    public Response<Long> create(SpuMaterial spuMaterial) {
        try {
            spuMaterialDao.create(spuMaterial);
            return Response.ok(spuMaterial.getId());
        } catch (Exception e) {
            log.error("create spuMaterial failed, spuMaterial:{}, cause:{}", spuMaterial, Throwables.getStackTraceAsString(e));
            return Response.fail("spu.material.create.fail");
        }
    }

    public Response<Boolean> update(SpuMaterial spuMaterial) {
        try {
            return Response.ok(spuMaterialDao.update(spuMaterial));
        } catch (Exception e) {
            log.error("update spuMaterial failed, spuMaterial:{}, cause:{}", spuMaterial, Throwables.getStackTraceAsString(e));
            return Response.fail("spu.material.update.fail");
        }
    }

    public Response<Boolean> deleteById(Long spuMaterialId) {
        try {
            return Response.ok(spuMaterialDao.delete(spuMaterialId));
        } catch (Exception e) {
            log.error("delete spuMaterial failed, spuMaterialId:{}, cause:{}", spuMaterialId, Throwables.getStackTraceAsString(e));
            return Response.fail("spu.material.delete.fail");
        }
    }
}
