package com.pousheng.middle.order.service;

import com.pousheng.middle.order.model.ExpressCode;
import io.terminus.common.model.Response;

/**
 * Created by tony on 2017/6/28.
 *
 */
public interface ExpressCodeWriteService {

    /**
     * 该方法可用于快递管理中新增快递机构
     *
     * @param expressCode,需要判断expressName是否已经存在
     * @return id, 返回快递机构的id
     */
    public Response<Long> create(ExpressCode expressCode);


    /**
     * 该方法可以用于快递管理中更新快递机构
     *
     * @param expressCode,需要传入主键id
     * @return
     */
    public Response<Boolean> update(ExpressCode expressCode);


    /**
     * 该方法可以用于快递管理中删除快递机构
     *
     * @param expressCodeId,快递机构的主键
     * @return
     */
    public Response<Boolean> deleteById(Long expressCodeId);


}
