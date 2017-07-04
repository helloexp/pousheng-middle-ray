package com.pousheng.erp.cache;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.terminus.common.exception.ServiceException;
import io.terminus.parana.brand.impl.dao.BrandDao;
import io.terminus.parana.brand.model.Brand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-26
 */
@Component
@Slf4j
public class ErpBrandCacher {

    private final LoadingCache<String, Brand> brandCache;

    @Autowired
    public ErpBrandCacher(@Value("${cache.duration.in.minutes: 60}")
                               Integer duration, final BrandDao brandDao) {
        this.brandCache = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, TimeUnit.HOURS)
                .maximumSize(2000)
                .build(new CacheLoader<String, Brand>() {
                    @Override
                    public Brand load(String outerId) throws Exception {
                        final Brand brand = brandDao.findByOuterId(outerId);
                        if (brand == null) {
                            log.error("pousheng brand(outerId={}) not found", outerId);
                            throw new ServiceException("brand.not.found");
                        }
                        return brand;
                    }
                });
    }

    /**
     * 根据外部id查找对应的品牌
     *
     * @param outerId 外部id
     * @return 对应的品牌
     */
    public Brand findByOuterId(String outerId){
        try {
            return brandCache.getUnchecked(outerId);
        } catch (Exception e) {
            Throwables.propagateIfPossible(e, ServiceException.class);
            log.error("failed to find brand by outerId({}), cause:{}", outerId, Throwables.getStackTraceAsString(e));
            throw new ServiceException(e);
        }
    }
}
