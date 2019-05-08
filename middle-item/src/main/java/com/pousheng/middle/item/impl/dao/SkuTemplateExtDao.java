package com.pousheng.middle.item.impl.dao;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import io.terminus.common.model.PageInfo;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.spu.model.SkuTemplate;
import org.springframework.stereotype.Repository;
import com.google.common.collect.Maps;
import io.terminus.common.model.Paging;
import io.terminus.common.utils.Constants;
import java.util.Collections;
import java.util.Map;


import java.util.List;

/**
 * Created by songrenfei on 2017/12/6
 */
@Repository
public class SkuTemplateExtDao extends MyBatisDao<SkuTemplate> {


    /**
     * 商品当前最大的id, 这个是dump搜素引擎用的
     *
     * @return 当前最大的id
     */
    public Long maxId() {
        Long id = getSqlSession().selectOne(sqlId("maxId"));
        return MoreObjects.firstNonNull(id, 0L);
    }

    /**
     * 查询id小于lastId内且更新时间大于since的limit个商品, 这个是dump搜素引擎用的
     *
     * @param lastId lastId 最大的店铺id
     * @param since  起始更新时间
     * @param limit  商品个数
     * @return id小于lastId内且更新时间大于since的limit个店铺
     */
    public List<SkuTemplate> listSince(Long lastId, String since, int limit) {
        return getSqlSession().selectList(sqlId("listSince"),
                ImmutableMap.of("lastId", lastId, "limit", limit, "since", since));
    }



    public Boolean updateImageByIds(List<Long> ids,String imageUrl){
        return getSqlSession().update(sqlId("updateImageByIds"),ImmutableMap.of("ids",ids,"imageUrl",imageUrl))>0;

    }


    public Boolean updateTypeByIds(List<Long> ids,Integer type){
        return getSqlSession().update(sqlId("updateTypeByIds"),ImmutableMap.of("ids",ids,"type",type))>0;

    }

    public Boolean updateTypeAndExtraById(Long id,Integer type,Integer price,String extraJson){
        return getSqlSession().update(sqlId("updateTypeAndExtraById"),ImmutableMap.of("id",id,"type",type,"price",price,"extraJson",extraJson))>0;

    }


    public Boolean updateBatch(List<SkuTemplate> skuTemplates){
        return getSqlSession().update(sqlId("updateBatch"),skuTemplates)>0;

    }

    /**
     * 根據料號過濾資料
     * @param params
     * @return
     */
    public Paging<SkuTemplate> findByMaterial(Integer offset, Integer limit, Map<String, Object> params){

        params.put("offset", (offset-1) * limit);
        params.put("limit", limit);
        // get total count
        Long total = sqlSession.selectOne(sqlId("findByMaterialCount"), params);
        if (total <= 0){
            return new Paging<SkuTemplate>(0L, Collections.<SkuTemplate>emptyList());
        }

        // get data
        List<SkuTemplate> datas = getSqlSession().selectList(sqlId("findByMaterial"),params);
        return new Paging<SkuTemplate>(total, datas);


    }


    public Paging<SkuTemplate> pagingBySpuIds(Integer pageNo, Integer pageSize, Map<String, Object> criteria) {
        PageInfo pageInfo = new PageInfo(pageNo, pageSize);
        criteria.put("offset", pageInfo.getOffset());
        criteria.put("limit", pageInfo.getLimit());
        Long total = sqlSession.selectOne(sqlId("pagingBySpuIdsCount"), criteria);
        if (total <= 0){
            return Paging.empty();
        }

        return new Paging<>(total, getSqlSession().selectList(sqlId("pagingBySpuIds"), criteria));
    }
}
