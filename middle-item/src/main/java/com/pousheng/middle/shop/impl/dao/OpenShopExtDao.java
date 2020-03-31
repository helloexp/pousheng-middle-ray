package com.pousheng.middle.shop.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.open.client.common.shop.model.OpenShop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author tanlongjun
 */
@Repository
@Slf4j
public class OpenShopExtDao extends MyBatisDao<OpenShop> {

    public List<OpenShop> searchByOuterIdAndBusinessId(String outerId, String businessId) {
        return getSqlSession().selectList(sqlId("searchByOuterIdAndBusinessId"),
            ImmutableMap.of("outerId", outerId, "businessId", businessId));
    }

    public Paging<OpenShop> searchByShopNameAndCompanyCode(List<String> notInChannel, String shopName, String companyCode, Integer pageNum, Integer pageSize, Set<Long> includes){
        PageInfo pageInfo = new PageInfo(pageNum, pageSize);
        // get total count
        Map<String, Object> criteria = Maps.newHashMap();
        if (StringUtils.hasText(shopName)) {
            criteria.put("shopName", shopName);
        }
        if (StringUtils.hasText(companyCode)) {
            criteria.put("companyCode", companyCode);
        }

        if (notInChannel != null && notInChannel.size() > 0) {
            criteria.put("notChannel", notInChannel);
        }
        if (includes != null && includes.size() > 0) {
            criteria.put("allowIds", includes);
        }
        criteria.put("offsetNum", pageInfo.getOffset());
        criteria.put("limitNum", pageInfo.getLimit());
        Long total = sqlSession.selectOne(sqlId("searchByNameAndCompanyCodeCount"), criteria);
        if (total <= 0){
            return new Paging<OpenShop>(0L, Collections.<OpenShop>emptyList());
        }
        // get data
        List<OpenShop> datas = sqlSession.selectList(sqlId("searchByNameAndCompanyCode"), criteria);
        return new Paging<OpenShop>(total, datas);
    }

}
