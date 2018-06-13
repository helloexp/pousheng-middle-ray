package com.pousheng.middle.order.service;


import com.pousheng.middle.order.dto.PoushengCompensateBizCriteria;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * 中台业务处理写服务
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/28
 * pousheng-middle
 */
public interface PoushengCompensateBizReadService {
    /**
     * 根据id查询
     *
     * @param Id 主键id
     * @return
     */
    Response<PoushengCompensateBiz> findById(Long Id);


    /**
     * 操作日志分页
     *
     * @param criteria
     * @return
     */
    Response<Paging<PoushengCompensateBiz>> paging(PoushengCompensateBizCriteria criteria);

    /**
     * 根据id集合查询
     *
     * @param ids id集合
     * @return
     */
    Response<List<PoushengCompensateBiz>> findByIdsAndStatus(List<Long> ids, String status);

}
