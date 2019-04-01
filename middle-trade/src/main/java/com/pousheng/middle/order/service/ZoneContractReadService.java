package com.pousheng.middle.order.service;

import com.pousheng.middle.order.model.ZoneContract;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author: songrenfei
 * Desc: 区部联系人表读服务
 * Date: 2018-04-04
 */

public interface ZoneContractReadService {

    /**
     * 根据id查询区部联系人表
     *
     * @param Id 主键id
     * @return 区部联系人表
     */
    Response<ZoneContract> findById(Long Id);


    /**
     * 分页查询
     *
     * @param zoneName
     * @param pageNo
     * @param pageSize
     * @return
     */
    Response<Paging<ZoneContract>> pagination(String zoneName, Integer pageNo, Integer pageSize);


    /**
     * 通过zoneId查询
     *
     * @param zoneId
     * @return
     */
    Response<List<ZoneContract>> findByZoneId(String zoneId);

}
