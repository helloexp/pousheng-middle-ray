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
     * 操作日志分页(顺序,任务处理)
     *
     * @param criteria
     * @return
     */
    Response<Paging<PoushengCompensateBiz>> paging(PoushengCompensateBizCriteria criteria);

    /**
     * 操作日志分页（倒序,前端展示）
     *
     * @param criteria
     * @return
     */
    Response<Paging<PoushengCompensateBiz>> pagingForShow(PoushengCompensateBizCriteria criteria);

    /**
     * 根据id集合查询
     *
     * @param ids id集合
     * @return
     */
    Response<List<PoushengCompensateBiz>> findByIdsAndStatus(List<Long> ids, String status);



    /**
     * 分页查询
     *
     * @param criteria 查询条件
     * @return 券信息
     */
    Response<Paging<Long>> pagingIds(PoushengCompensateBizCriteria criteria);

}
