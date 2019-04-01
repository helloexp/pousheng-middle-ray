package com.pousheng.erp.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.pousheng.erp.dao.mysql.SpuMaterialDao;
import com.pousheng.erp.model.SpuMaterial;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author zhaoxw
 * @date 2018/7/4
 */

@Component
@Slf4j
public class SpuMaterialCacher {

    private final LoadingCache<String, SpuMaterial> spuMaterialCacher;

    @Autowired
    public SpuMaterialCacher(@Value("${cache.duration.in.minutes: 60}")
                                Integer duration, final SpuMaterialDao spuMaterialDao) {
        this.spuMaterialCacher = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, TimeUnit.HOURS)
                .maximumSize(2000)
                .build(new CacheLoader<String, SpuMaterial>() {
                    @Override
                    public SpuMaterial load(String materialId)  {
                        final SpuMaterial spuMaterial = spuMaterialDao.findByMaterialId(materialId);
                        return spuMaterial;
                    }
                });
    }

    public SpuMaterial findByMaterialId(String  materialId){
        return this.spuMaterialCacher.getUnchecked(materialId);
    }

    public void refreshById(String spuMaterial) {
        this.spuMaterialCacher.refresh(spuMaterial);
    }
}
