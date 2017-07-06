package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.pousheng.middle.warehouse.impl.dao.WarehouseShopGroupDao;
import com.pousheng.middle.warehouse.model.WarehouseShopGroup;
import com.pousheng.middle.warehouse.service.WarehouseShopGroupReadService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-05
 */
@Service
@Slf4j
public class WarehouseShopGroupReadServiceImpl implements WarehouseShopGroupReadService {

    private final WarehouseShopGroupDao warehouseShopGroupDao;

    @Autowired
    public WarehouseShopGroupReadServiceImpl(WarehouseShopGroupDao warehouseShopGroupDao) {
        this.warehouseShopGroupDao = warehouseShopGroupDao;
    }

    /**
     * 根据店铺组找对应的店铺列表
     *
     * @param groupId 店铺组id
     * @return 对应的店铺列表
     */
    @Override
    public Response<List<WarehouseShopGroup>> findByGroupId(Long groupId) {
        try {
            return Response.ok(warehouseShopGroupDao.findByGroupId(groupId));
        } catch (Exception e) {
            log.error("failed to find warehouseShopGroups by groupId={}, cause:{}",
                    groupId, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.shopGroup.find.fail");
        }
    }

    /**
     * 获取已设置发货规则的店铺id集合
     *
     * @return 已设置发货规则的店铺id集合
     */
    @Override
    public Response<Set<Long>> findShopIds() {
        try{
            List<Long> shopIds = warehouseShopGroupDao.findDistinctShopIds();
            return Response.ok(Sets.newHashSet(shopIds));
        }catch (Exception e){
            log.error("failed to find shopIds which have warehouse rules set, cause:{} ",
                    Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.shopGroup.find.fail");
        }
    }
}
