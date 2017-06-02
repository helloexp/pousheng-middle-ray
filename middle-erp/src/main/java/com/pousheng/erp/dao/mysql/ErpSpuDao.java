package com.pousheng.erp.dao.mysql;

import com.google.common.collect.ImmutableMap;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.spu.model.Spu;
import org.springframework.stereotype.Repository;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-02
 */
@Repository
public class ErpSpuDao extends MyBatisDao<Spu> {

    public Spu findByCategoryIdAndCode(Long categoryId, String code){
        return getSqlSession().selectOne(sqlId("findByCategoryIdAndCode"),
                ImmutableMap.of("categoryId", categoryId, "spuCode",code));
    }
}
