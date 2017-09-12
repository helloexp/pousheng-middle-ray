package com.pousheng.erp.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.pousheng.erp.dao.mysql.ErpSpuDao;
import io.terminus.common.exception.ServiceException;
import io.terminus.parana.spu.model.Spu;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-09-12
 */
@Component
@Slf4j
public class ErpSpuCacher {
    private final LoadingCache<Long, Spu> spuCache;

    @Autowired
    public ErpSpuCacher(@Value("${cache.duration.in.minutes: 60}")
                                  Integer duration, final ErpSpuDao erpSpuDao) {
        this.spuCache = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, TimeUnit.HOURS)
                .maximumSize(2000)
                .build(new CacheLoader<Long, Spu>() {
                    @Override
                    public Spu load(Long id) throws Exception {
                        final Spu spu = erpSpuDao.findById(id);
                        if (spu == null) {
                            log.error("pousheng spu(id={}) not found", id);
                            throw new ServiceException("spu.not.found");
                        }
                        return spu;
                    }
                });
    }

    public Spu findById(Long spuId){
        return this.spuCache.getUnchecked(spuId);
    }

}
