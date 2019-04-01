package com.pousheng.erp.dao.mysql;

import com.pousheng.erp.model.SpuMaterial;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.Date;
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
     * 根据materialCode查找对应的映射关系
     *
     * @param materialCode 货号
     * @return 是否存在
     */
    public SpuMaterial findByMaterialCode(String materialCode) {
        return getSqlSession().selectOne(sqlId("findByMaterialCode"), materialCode);
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


    /**
     * 根据spuId列表查找对应的映射关系列表, 一个spuId可能对应多个货品id(归组)
     *
     * @param spuIds spuId 列表
     * @return 对应的货品映射关系
     */
    public List<SpuMaterial> findBySpuIds(List<Long> spuIds){
        return getSqlSession().selectList(sqlId("findBySpuIds"), spuIds);
    }

    /**
     * 根据materialId查询对应的上市日期
     * @param materialId
     * @return
     */
    public Date findSaleDate(String materialId) {
        return getSqlSession().selectOne(sqlId("findSaleDate"), materialId);

    }
}
