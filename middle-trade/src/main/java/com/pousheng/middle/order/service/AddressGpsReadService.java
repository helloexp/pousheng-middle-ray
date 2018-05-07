package com.pousheng.middle.order.service;

import com.google.common.base.Optional;
import com.pousheng.middle.order.enums.AddressBusinessType;
import com.pousheng.middle.order.model.AddressGps;
import io.terminus.common.model.Response;

import java.util.List;

/**
 * Author: songrenfei
 * Desc: 地址定位信息表读服务
 * Date: 2017-12-15
 */

public interface AddressGpsReadService {

    /**
     * 根据id查询地址定位信息表
     * @param Id 主键id
     * @return 地址定位信息表
     */
    Response<AddressGps> findById(Long Id);


    Response<Optional<AddressGps>> findByBusinessIdAndType(Long businessId, AddressBusinessType type);

    /**
     * 根据省id和业务类型查询对应的门店或仓库的定位信息
     * @param provinceId 省id
     * @param businessType 类型{@link AddressBusinessType}
     * @return 地址定位信息
     */
    Response<List<AddressGps>> findByProvinceIdAndBusinessType(Long provinceId, AddressBusinessType businessType);


    /**
     * 根据区id和业务类型查询对应的门店或仓库的定位信息
     * @param regionId 区id
     * @param businessType 类型{@link AddressBusinessType}
     * @return 地址定位信息
     */
    Response<List<AddressGps>> findByRegionIdAndBusinessType(Long regionId, AddressBusinessType businessType);
}
