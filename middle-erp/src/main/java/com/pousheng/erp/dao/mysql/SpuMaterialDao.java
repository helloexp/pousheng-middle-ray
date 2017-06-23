package com.pousheng.erp.dao.mysql;

import com.pousheng.erp.model.SpuMaterial;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author: jlchen
 * Desc: spu与material_id的关联Dao类
 * Date: 2017-06-23
 */
@Repository
public class SpuMaterialDao extends MyBatisDao<SpuMaterial> {

    /**
     * 根据materialId查找对应的映射关系
     *
     * @param materialId 货品id
     * @return 是否存在
     */
    public SpuMaterial findByMaterialId(String materialId) {
        return getSqlSession().selectOne(sqlId("findByMaterialId"), materialId);
    }

    /**
     * 根据spuId查找对应的映射关系, 一个spuId可能对应多个货品id(归组)
     *
     * @param spuId spuId
     * @return 对应的货品映射关系
     */
    public List<SpuMaterial> findBySpuId(Long spuId){
        return getSqlSession().selectList(sqlId("findBySpuId"), spuId);
    }
}
