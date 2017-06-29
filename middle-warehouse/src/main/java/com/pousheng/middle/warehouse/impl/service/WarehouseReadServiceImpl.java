package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseDao;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Author: jlchen
 * Desc: 仓库读服务实现类
 * Date: 2017-06-07
 */
@Slf4j
@Service
public class WarehouseReadServiceImpl implements WarehouseReadService {

    private final WarehouseDao warehouseDao;

    @Autowired
    public WarehouseReadServiceImpl(WarehouseDao warehouseDao) {
        this.warehouseDao = warehouseDao;
    }

    @Override
    public Response<Warehouse> findById(Long Id) {
        try {
            return Response.ok(warehouseDao.findById(Id));
        } catch (Exception e) {
            log.error("find warehouse by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.find.fail");
        }
    }

    /**
     * 仓库列表
     *
     * @param pageNo   起始页码
     * @param pageSize 每页返回数目
     * @param params   其他查询参数
     * @return 仓库列表
     */
    @Override
    public Response<Paging<Warehouse>> pagination(Integer pageNo, Integer pageSize, Map<String, Object> params) {
        try{
            PageInfo pageInfo = new PageInfo(pageNo, pageSize);
            Paging<Warehouse> p = warehouseDao.paging(pageInfo.getOffset(), pageInfo.getLimit(), params);
            return Response.ok(p);
        }catch (Exception e){
            log.error("failed to pagination warehouse with params:{}, cause:{}",
                    params, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.find.fail");
        }
    }

    /**
     * 根据仓库编码查询对应的仓库
     *
     * @param code 仓库编码
     * @return 对应的仓库
     */
    @Override
    public Response<Optional<Warehouse>> findByCode(String code) {
        try {
            Warehouse w = warehouseDao.findByCode(code);
            return Response.ok(Optional.fromNullable(w));
        } catch (Exception e) {
            log.error("failed to find warehouse by code:{}, cause:{}",
                    code, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.find.fail");
        }
    }

    /**
     * 根据仓库编码模糊查询
     *
     * @param codePart 仓库编码部分
     * @return 匹配的仓库列表
     */
    @Override
    public Response<List<Warehouse>> findByFuzzyCode(String codePart) {
        try {
            return Response.ok(warehouseDao.findByFuzzyCode(codePart));
        } catch (Exception e) {
            log.error("failed to find warehouse by fuzzy code:{}, cause:{}",
                    codePart, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.find.fail");
        }
    }
}
