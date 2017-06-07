package com.pousheng.middle.spu.service;

import com.google.common.base.Throwables;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.spu.impl.dao.SpuDao;
import io.terminus.parana.spu.model.Spu;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Created by songrenfei on 2017/6/7
 */
@Slf4j
@Service
public class PoushengMiddleSpuService {
    @Autowired
    private SpuDao spuDao;

    /**
     * 分页查询spu列表
     *
     * @param pageNo   起始页码
     * @param pageSize 每页返回条数
     * @param params   参数
     * @return 分页结果
     */
    public Response<Paging<Spu>> findBy(Integer pageNo, Integer pageSize, Map<String, Object> params) {
        try {
            PageInfo pi = new PageInfo(pageNo, pageSize);
            Paging<Spu> r = spuDao.paging(pi.getOffset(), pi.getLimit(), params);
            return Response.ok(r);
        } catch (Exception e) {
            log.error("failed to find spus by {}, cause:{}", params, Throwables.getStackTraceAsString(e));
            return Response.fail("spu.find.fail");
        }
    }
}
