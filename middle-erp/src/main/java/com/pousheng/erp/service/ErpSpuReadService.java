package com.pousheng.erp.service;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.erp.dao.mysql.ErpSpuDao;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.spu.model.Spu;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;

/**
 * Created by songrenfei on 2017/10/19
 */
@Service
@Slf4j
public class ErpSpuReadService{

    @Autowired
    private ErpSpuDao erpSpuDao;

    /**
     * 分页查询某个类目下的spu列表
     *
     * @param categoryId   类目id
     * @param keyword  名称关键字, 前缀查询, 可以为空
     * @param pageNo   起始页码, 从1开始
     * @param pageSize 分页大小
     * @return  spu列表
     */
    public Response<Paging<Spu>> findByCategoryId(Long categoryId, String keyword, Integer pageNo,
                                           Integer pageSize){
        try {
            PageInfo e = new PageInfo(pageNo, pageSize);
            HashMap params = Maps.newHashMap();
            if(StringUtils.hasText(keyword)) {
                params.put("name", keyword);
            }

            params.put("categoryId", categoryId);
            return Response.ok(this.erpSpuDao.paging(e.getOffset(), e.getLimit(), params));
        } catch (Exception var7) {
            log.error("failed to paging spus(categoryId={}, name={},pageNo={}, pageSize={}), cause: {}", new Object[]{categoryId, keyword, pageNo, pageSize, Throwables.getStackTraceAsString(var7)});
            return Response.fail("spu.find.fail");
        }
    }

}
