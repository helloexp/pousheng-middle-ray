package com.pousheng.middle.category.service;

import com.pousheng.middle.category.dto.CategoryCriteria;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.category.model.BackCategory;

/**
 * @author zhaoxw
 * @date 2018/5/4
 */
public interface PsBackCategoriesReadService {

    /**
     * 根据级别查询类目名称列表
     *
     * @param criteria 查询条件
     * @return 分页结果
     */
    Response<Paging<BackCategory>> findBy(CategoryCriteria criteria);

}
