package com.pousheng.middle.order.service;

import com.pousheng.middle.order.model.PoushengCompensateBiz;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * 中台业务处理写服务
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/28
 * pousheng-middle
 */
public interface PoushengCompensateBizWriteService {
    /**
     * 创建poushengCompensateBiz
     *
     * @param poushengCompensateBiz
     * @return 主键id
     */
    Response<Long> create(PoushengCompensateBiz poushengCompensateBiz);

    /**
     * 创建待处理的Biz任务
     * @param bizType
     * @param context
     * @param bizId
     * @return
     */
    Response<Long> create(String bizType,String context,String bizId);

    /**
     * 更新poushengCompensateBiz
     *
     * @param poushengCompensateBiz
     * @return 是否成功
     */
    Response<Boolean> update(PoushengCompensateBiz poushengCompensateBiz);

    /**
     * 根据主键id删除PoushengCompensateBiz
     *
     * @param id
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long id);

    /**
     * 以乐观锁方式更新状态
     *
     * @param id            主键
     * @param currentStatus 当前状态
     * @param newStatus     新的状态
     * @return
     */
    Response<Boolean> updateStatus(Long id, String currentStatus, String newStatus);


    /**
     * 更新上次失败原因
     *
     * @param id
     * @param lastFailedReason
     * @return
     */
    Response<Boolean> updateLastFailedReason(Long id, String lastFailedReason, Integer cnt);

    /**
     * 批量更新状态
     *
     * @param ids    id集合
     * @param status 状态
     * @return
     */
    Response<Boolean> batchUpdateStatus(List<Long> ids, String status);

    /**
     * 长时间未处理的任务置为待处理
     * @return
     */
    Response<Boolean> resetStatus();
}
