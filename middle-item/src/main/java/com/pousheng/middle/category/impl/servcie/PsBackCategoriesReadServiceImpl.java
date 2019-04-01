package com.pousheng.middle.category.impl.servcie;

import com.google.common.base.Throwables;
import com.pousheng.middle.category.dto.CategoryCriteria;
import com.pousheng.middle.category.impl.dao.PsBackCategoriesDao;
import com.pousheng.middle.category.service.PsBackCategoriesReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.category.model.BackCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author zhaoxw
 * @date 2018/5/4
 */

@Service
@Slf4j
@RpcProvider
public class PsBackCategoriesReadServiceImpl implements PsBackCategoriesReadService {

    @Autowired
    private PsBackCategoriesDao psBackCategoriesDao;

    @Override
    public Response<Paging<BackCategory>> findBy(CategoryCriteria criteria) {
        try {
            Paging<BackCategory> paging = psBackCategoriesDao.pagingBy(criteria.getOffset(), criteria.getLimit(), criteria.toMap());
            return Response.ok(paging);
        } catch (Exception e) {
            log.error("failed to paging item group, level={}, cause:{}", criteria.getLevel(), Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.find.fail");
        }
    }
}

