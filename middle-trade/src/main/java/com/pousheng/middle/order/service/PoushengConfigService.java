package com.pousheng.middle.order.service;

import com.pousheng.middle.order.model.PoushengConfig;
import io.terminus.common.model.Response;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/8/28
 */
public interface PoushengConfigService {

    /**
     * 根据id查询配置表
     *
     * @param poushengConfigId 主键id
     * @return 配置表
     */
    Response<PoushengConfig> findPoushengConfigById(Long poushengConfigId);

    /**
     * 根据类型查询配置表
     *
     * @param type 类型
     * @return 配置表
     */
    Response<PoushengConfig> findPoushengConfigByType(String type);

    /**
     * 创建PoushengConfig
     *
     * @param poushengConfig
     * @return 主键id
     */
    Response<Long> createPoushengConfig(PoushengConfig poushengConfig);

    /**
     * 更新PoushengConfig
     *
     * @param poushengConfig
     * @return 是否成功
     */
    Response<Boolean> updatePoushengConfig(PoushengConfig poushengConfig);

    /**
     * 根据主键id删除PoushengConfig
     *
     * @param poushengConfigId
     * @return 是否成功
     */
    Response<Boolean> deletePoushengConfigById(Long poushengConfigId);

}
