package com.pousheng.middle.item.dao;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.terminus.parana.spu.impl.dao.SpuDao;
import io.terminus.parana.spu.model.Spu;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * AUTHOR: zhangbin
 * ON: 2019/7/3
 */
@Repository
public class SpuExtDao extends SpuDao {

    public List<Spu> findByBrandId(Long brandId) {
        List<Spu> result = Lists.newArrayList();
        List<Spu> spus = findByBrandId(brandId, 0L);
        while (!spus.isEmpty()) {
            result.addAll(spus);
            spus = findByBrandId(brandId, spus.get(spus.size() - 1).getId());
        }

        return result;
    }

    private List<Spu> findByBrandId(Long brandId, Long id) {
        Map<String, Object> criteria = Maps.newHashMap();
        criteria.put("id", id);
        criteria.put("brandId", brandId);
        return this.getSqlSession().selectList(sqlId("findByBrandId"), criteria);
    }
}
