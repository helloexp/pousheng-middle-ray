package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.impl.dao.WarehouseSkuStockDao;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import com.pousheng.middle.warehouse.service.WarehouseSkuReadService;
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
 * Desc: sku在仓库的库存情况读服务实现类
 * Date: 2017-06-07
 */
@Slf4j
@Service
public class WarehouseSkuReadServiceImpl implements WarehouseSkuReadService {

    private final WarehouseSkuStockDao warehouseSkuStockDao;

    @Autowired
    public WarehouseSkuReadServiceImpl(WarehouseSkuStockDao warehouseSkuStockDao) {
        this.warehouseSkuStockDao = warehouseSkuStockDao;
    }

    @Override
    public Response<WarehouseSkuStock> findById(Long Id) {
        try {
            return Response.ok(warehouseSkuStockDao.findById(Id));
        } catch (Exception e) {
            log.error("find warehouseSku by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.sku.find.fail");
        }
    }

    /**
     * 查询某个sku在指定仓库的库存情况
     *
     * @param warehouseId 仓库id 仓库id
     * @param skuCode     skuCode
     * @return sku在仓库的库存情况
     */
    @Override
    public Response<WarehouseSkuStock> findByWarehouseIdAndSkuCode(Long warehouseId, String skuCode) {
        try {
            WarehouseSkuStock stock = warehouseSkuStockDao.findByWarehouseIdAndSkuCode(warehouseId, skuCode);
            if(stock == null){
                log.warn("no stock record found for warehouseId={} and skuCode={}", warehouseId, skuCode);
                stock = new WarehouseSkuStock();
                stock.setWarehouseId(warehouseId);
                stock.setSkuCode(skuCode);
                stock.setAvailStock(0L);
                stock.setLockedStock(0L);
                stock.setBaseStock(0L);
            }
            return Response.ok(stock);
        } catch (Exception e) {
            log.error("failed to find stock for warehouseId={} and skuCode={}, cause:{}",
                    warehouseId, skuCode, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.sku.find.fail");
        }
    }

    /**
     * 根据sku编码查找在仓库中的总的可用库存
     *
     * @param skuCode sku编码
     * @return 库存结果
     */
    @Override
    public Response<WarehouseSkuStock> findAvailStockBySkuCode(String skuCode) {

        try {
            return Response.ok(warehouseSkuStockDao.findAvailStockBySkuCode(skuCode));
        } catch (Exception e) {
            log.error("failed to find avail stock by skuCode({}), cause:{}",
                    skuCode, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.sku.find.fail");
        }
    }

    /**
     * 分页查询 sku的库存概览情况(不分仓)
     *
     * @param pageNo   起始页码
     * @param pageSize 每页返回条数
     * @param params   查询参数
     * @return 分页结果
     */
    @Override
    public Response<Paging<WarehouseSkuStock>> findBy(Integer pageNo, Integer pageSize, Map<String, Object> params) {

        try {
            PageInfo pageInfo = new PageInfo(pageNo, pageSize);
            Paging<WarehouseSkuStock> p = warehouseSkuStockDao.pagingByDistinctSkuCode(pageInfo.getOffset(), pageInfo.getLimit(), params);
            return Response.ok(p);
        } catch (Exception e) {
            log.error("failed to pagination warehouseSkuStocks by params:{}, cause:{}",
                    params, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.sku.find.fail");
        }
    }

    /**
     * 批量查询skuCodes在某个仓库中的库存情况
     *
     * @param warehouseId 仓库id
     * @param skuCodes    sku codes
     * @return skuCodes在某个仓库中的库存情况
     */
    @Override
    public Response<Map<String, Integer>> findByWarehouseIdAndSkuCodes(Long warehouseId, List<String> skuCodes) {
        try {
            List<WarehouseSkuStock> stocks = warehouseSkuStockDao.findByWarehouseIdAndSkuCodes(warehouseId, skuCodes);
            Map<String, Integer> r = Maps.newHashMapWithExpectedSize(skuCodes.size());
            for (WarehouseSkuStock stock : stocks) {
                r.put(stock.getSkuCode(), stock.getAvailStock().intValue());
            }
            return Response.ok(r);
        } catch (Exception e) {
            log.error("failed to find stock for warehouseId={} and skuCodes={}, cause:{}",
                    warehouseId, skuCodes, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.sku.find.fail");
        }
    }


}
