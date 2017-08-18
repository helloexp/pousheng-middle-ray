package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.impl.dao.WarehouseShopStockRuleDao;
import com.pousheng.middle.warehouse.model.WarehouseShopStockRule;
import com.pousheng.middle.warehouse.service.WarehouseShopStockRuleReadService;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-18 10:37:00
 */
@Slf4j
@Service
public class WarehouseShopStockRuleReadServiceImpl implements WarehouseShopStockRuleReadService {

    @Autowired
    private WarehouseShopStockRuleDao warehouseShopStockRuleDao;

    @Override
    public Response<WarehouseShopStockRule> findById(Long id) {
        try{
            WarehouseShopStockRule rule = warehouseShopStockRuleDao.findById(id);
            if(rule == null){
                log.error("WarehouseShopStockRule(id={}) not found", id);
                return Response.fail("warehouse.shop.stock.rule.not.found");
            }
            return Response.ok(rule);
        }catch (Exception e){
            log.error("failed to find warehouse shop stock rule by id:{}, cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.shop.stock.rule.find.fail");
        }
    }

    /**
     * 根据店铺id查询对应的库存分配规则
     *
     * @param shopId 店铺id
     * @return 对应的库存分配规则
     */
    @Override
    public Response<WarehouseShopStockRule> findByShopId(Long shopId) {
        try{
            WarehouseShopStockRule byShopId = warehouseShopStockRuleDao.findByShopId(shopId);
            if(byShopId == null){
                log.error("failed to find shop stock rule for shop(id={})", shopId);
                return Response.fail("shop.stock.rule.not.found");
            }
            return Response.ok(byShopId);
        }catch (Exception e){
            log.error("failed to find warehouse shop stock rule by shopId:{}, cause:{}",
                    shopId, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.shop.stock.rule.find.fail");
        }
    }

    /**
     * 分页列出对应店铺的库存分配规则
     *
     * @param pageNo  起始页码
     * @param pageSize 每页返回数量
     * @param shopIds 店铺列表
     * @return 对应的规则
     */
    @Override
    public Response<Paging<WarehouseShopStockRule>> pagination(Integer pageNo, Integer pageSize, List<Long> shopIds) {
        try{
            PageInfo pageInfo = new PageInfo(pageNo, pageSize);
            Map<String, Object> params = Maps.newHashMap();
            params.put("shopIds", shopIds);
            return Response.ok(warehouseShopStockRuleDao.paging(pageInfo.getOffset(), pageInfo.getLimit(),params));
        }catch (Exception e){
            log.error("failed to paging warehouse shop stock rule by pageNo:{} pageSize:{}, cause:{}",
                    pageNo, pageSize, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.shop.stock.rule.find.fail");
        }
    }

}
