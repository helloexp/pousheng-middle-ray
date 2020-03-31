package com.pousheng.middle.order.service;

import com.pousheng.middle.order.model.AddressGps;
import io.terminus.common.model.Response;

/**
 * Author: songrenfei
 * Desc: 地址定位信息表写服务
 * Date: 2017-12-15
 */

public interface AddressGpsWriteService {

    /**
     * 创建AddressGps
     * @param addressGps
     * @return 主键id
     */
    Response<Long> create(AddressGps addressGps);

    /**
     * 更新AddressGps
     * @param addressGps
     * @return 是否成功
     */
    Response<Boolean> update(AddressGps addressGps);

    /**
     * 根据主键id删除AddressGps
     * @param addressGpsId
     * @return 是否成功
     */
    Response<Boolean> deleteById(Long addressGpsId);
}