package com.pousheng.middle.shop.service;

import com.google.common.base.Optional;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.shop.model.Shop;

import java.util.List;

/**
 * Created by songrenfei on 2017/12/6
 */
public interface PsShopReadService {
    /**
     * 分页店铺
     *
     * @param name     店铺名
     * @param userId   商家用户ID
     * @param type     店铺类型
     * @param status   店铺状态
     * @param status   店铺状态
     * @param outerId   店铺外码
     * @param businessId   区别编码
     * @param pageNo   页码
     * @param pageSize 数量
     * @return 分页结果
     */
    Response<Paging<Shop>> pagination(String name, Long userId, Integer type, Integer status, String outerId, Long businessId, List<String> zoneIds, Integer pageNo, Integer pageSize);

    Response<Optional<Shop>> findByOuterIdAndBusinessId(String outerId, Long businessId);

    Response<Shop> findShopById(Long id);

    /**
     * 获取所有店铺信息（不包括已删除状态）
     * @return
     */
    Response<List<Shop>> findAllShopsOn();

    Response<Shop> findByCompanyCodeAndOutId(String username);

    Response<List<Shop>> findByOuterIds(List<String> outerIds);

    Response<Shop> findShopByUserName(String userName);
}
