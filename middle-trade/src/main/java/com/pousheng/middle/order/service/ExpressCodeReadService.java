package com.pousheng.middle.order.service;

import com.pousheng.middle.order.dto.ExpressCodeCriteria;
import com.pousheng.middle.order.model.ExpressCode;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

/**
 * Created by tony on 2017/6/28.
 */
public interface ExpressCodeReadService {
    /**
     * 分页查询:供快递管理查询使用
     *
     * @param criteria(expressName:快递名称;)
     * @return
     */
    Response<Paging<ExpressCode>> pagingExpressCode(ExpressCodeCriteria criteria);

    /**
     * 单个查询:供快递管理查询使用
     *
     * @param id
     * @return
     */
    Response<ExpressCode> findById(Long id);
}
