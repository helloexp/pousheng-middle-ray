package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseShopReturnDao;
import com.pousheng.middle.warehouse.model.WarehouseShopReturn;
import com.pousheng.middle.warehouse.service.WarehouseShopReturnReadService;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Author: jlchen
 * Desc: 店铺的退货仓库读服务实现类
 * Date: 2017-06-21
 */
@Slf4j
@Service
public class WarehouseShopReturnReadServiceImpl implements WarehouseShopReturnReadService {

    private final WarehouseShopReturnDao warehouseShopReturnDao;

    @Autowired
    public WarehouseShopReturnReadServiceImpl(WarehouseShopReturnDao warehouseShopReturnDao) {
        this.warehouseShopReturnDao = warehouseShopReturnDao;
    }

    @Override
    public Response<WarehouseShopReturn> findById(Long id) {
        try {
            return Response.ok(warehouseShopReturnDao.findById(id));
        } catch (Exception e) {
            log.error("find warehouseShopReturn by id :{} failed,  cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.shop.return.find.fail");
        }
    }

    /**
     * 根据店铺id查找对应的退货仓
     *
     * @param shopId 店铺id
     * @return 店铺的退货仓库
     */
    @Override
    public Response<WarehouseShopReturn> findByShopId(Long shopId) {
        try {
            return Response.ok(warehouseShopReturnDao.findByShopId(shopId));
        } catch (Exception e) {
            log.error("find warehouseShopReturn by shopId :{} failed,  cause:{}",
                    shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.shop.return.find.fail");
        }
    }

    /**
     * 分页查找退货仓库
     *
     * @param pageNo   起始页码
     * @param pageSize 每页显示条数
     * @param params   查询参数
     * @return 店铺的退货仓库列表
     */
    @Override
    public Response<Paging<WarehouseShopReturn>> pagination(Integer pageNo,
                                                            Integer pageSize,
                                                            Map<String, Object> params) {
        try {
            PageInfo pageInfo = new PageInfo(pageNo, pageSize);
            Paging<WarehouseShopReturn> p = warehouseShopReturnDao.paging(pageInfo.getOffset(), pageInfo.getLimit(), params);
            return Response.ok(p);
        } catch (Exception e) {
            log.error("failed to pagination WarehouseShopReturns, params:{}, cause:{}",
                    params, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.shop.return.find.fail");
        }
    }
}
