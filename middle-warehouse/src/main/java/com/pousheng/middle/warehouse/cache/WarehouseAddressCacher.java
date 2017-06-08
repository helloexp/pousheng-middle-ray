package com.pousheng.middle.warehouse.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.pousheng.middle.warehouse.impl.dao.WarehouseAddressDao;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-08
 */
@Component
@Slf4j
public class WarehouseAddressCacher {

    private final LoadingCache<Integer, List<WarehouseAddress>> levelCache;

    private final LoadingCache<Long, List<WarehouseAddress>> childrenCache;

    @Autowired
    public WarehouseAddressCacher(@Value("${cache.duration.in.minutes: 180}") final Integer duration,
                                  final WarehouseAddressDao warehouseAddressDao) {
        this.levelCache = CacheBuilder
                .newBuilder()
                .expireAfterAccess(duration, TimeUnit.MINUTES)
                .build(new CacheLoader<Integer, List<WarehouseAddress>>() {
                    @Override
                    public List<WarehouseAddress> load(Integer  level) throws Exception {
                       return  warehouseAddressDao.findByLevel(level);
                    }
                });

        this.childrenCache =  CacheBuilder
                .newBuilder()
                .expireAfterAccess(duration, TimeUnit.MINUTES)
                .build(new CacheLoader<Long, List<WarehouseAddress>>() {
                    @Override
                    public List<WarehouseAddress> load(Long  id) throws Exception {
                        return  warehouseAddressDao.findByPid(id);
                    }
                });
    }

    public List<WarehouseAddress> findByLevel(Integer level){
        return this.levelCache.getUnchecked(level);
    }

    public List<WarehouseAddress> findByPid(Long pid){
        return this.childrenCache.getUnchecked(pid);
    }
}
