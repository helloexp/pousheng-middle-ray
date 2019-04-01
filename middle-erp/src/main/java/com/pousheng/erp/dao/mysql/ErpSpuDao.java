package com.pousheng.erp.dao.mysql;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.common.utils.Constants;
import io.terminus.parana.spu.model.Spu;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-02
 */
@Repository
public class ErpSpuDao extends MyBatisDao<Spu> {

    protected static final String ERPPAGING = "erpPaging";	    //分页查询


    public Spu findByCategoryIdAndCode(Long categoryId, String code){
        return getSqlSession().selectOne(sqlId("findByCategoryIdAndCode"),
                ImmutableMap.of("categoryId", categoryId, "spuCode",code));
    }



    /**
     * 查询分页对象
     * @param offset 起始偏移
     * @param limit 分页大小
     * @param criteria Map查询条件
     * @return 查询到的分页对象
     */
    public Paging<Spu> erPpaging(Integer offset, Integer limit, Map<String, Object> criteria) {
        if (criteria == null) {    //如果查询条件为空
            criteria = Maps.newHashMap();
        }
        // get total count
        Long total = sqlSession.selectOne(sqlId(COUNT), criteria);
        if (total <= 0){
            return new Paging<Spu>(0L, Collections.<Spu>emptyList());
        }
        criteria.put(Constants.VAR_OFFSET, offset);
        criteria.put(Constants.VAR_LIMIT, limit);
        // get data
        List<Spu> datas = sqlSession.selectList(sqlId(ERPPAGING), criteria);
        return new Paging<Spu>(total, datas);
    }

    /**
     * 根据spucode获取有效的spu
     * @param spuCode
     * @return
     */
    public List<Spu> findBySpuCode(String spuCode){
        return getSqlSession().selectList(sqlId("findBySpuCode"),spuCode);
    }
}
