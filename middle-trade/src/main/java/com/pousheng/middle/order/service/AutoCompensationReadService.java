package com.pousheng.middle.order.service;

import com.pousheng.middle.order.model.AutoCompensation;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.List;
import java.util.Map;

/**
 * Created by penghui on 2018/1/15
 */
public interface AutoCompensationReadService {

    /**
     * 查询自动补偿任务
     * @return
     */
    Response<List<AutoCompensation>> findAutoCompensationTask(Integer type,Integer status);

    /**
     * 分页查询自动补偿任务
     * @param pageNo
     * @param pageSize
     * @param param
     * @return
     */
    Response<Paging<AutoCompensation>> pagination(Integer pageNo, Integer pageSize, Map<String,Object> param);
}
