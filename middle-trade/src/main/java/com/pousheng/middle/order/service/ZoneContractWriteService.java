package com.pousheng.middle.order.service;

import com.pousheng.middle.order.model.ZoneContract;
import io.terminus.common.model.Response;

/**
 * Author: songrenfei
 * Desc: 区部联系人表写服务
 * Date: 2018-04-04
 */

public interface ZoneContractWriteService {

    /**
     * 创建ZoneContract
     * @param zoneContract
     * @return 主键id
     */
    Response<Long> create(ZoneContract zoneContract);

    /**
     * 更新ZoneContract
     * @param zoneContract
     * @return 是否成功
     */
    Response<Boolean> update(ZoneContract zoneContract);

    /**
     * 根据主键id删除ZoneContract
     * @param zoneContractId
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long zoneContractId);
}