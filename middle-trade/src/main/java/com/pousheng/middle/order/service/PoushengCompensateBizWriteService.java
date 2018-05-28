package com.pousheng.middle.order.service;

import com.pousheng.middle.order.model.OperationLog;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import io.terminus.common.model.Response;

/**
 * 中台业务处理写服务
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/28
 * pousheng-middle
 */
public interface PoushengCompensateBizWriteService {
    /**
     * 创建poushengCompensateBiz
     * @param poushengCompensateBiz
     * @return 主键id
     */
    Response<Long> create(PoushengCompensateBiz poushengCompensateBiz);

    /**
     * 更新poushengCompensateBiz
     * @param poushengCompensateBiz
     * @return 是否成功
     */
    Response<Boolean> update(PoushengCompensateBiz poushengCompensateBiz);

    /**
     * 根据主键id删除PoushengCompensateBiz
     * @param id
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long id);

    /**
     * 以乐观锁方式更新状态
     * @param id 主键
     * @param currentStatus 当前状态
     * @param newStatus 新的状态
     * @return
     */
    Response<Boolean> updateStatus(Long id,String currentStatus,String newStatus);
}
