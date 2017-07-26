package com.pousheng.middle.web.warehouses;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.warehouse.model.WarehouseShopStockRule;
import com.pousheng.middle.warehouse.service.WarehouseShopStockRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseShopStockRuleWriteService;
import com.pousheng.middle.web.events.warehouse.PushEvent;
import com.pousheng.middle.web.user.component.UserManageShopReader;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-18
 */
@RestController
@RequestMapping("/api/warehouse/shop-rule")
@Slf4j
public class WarehouseShopStockRules {

    @RpcConsumer
    private WarehouseShopStockRuleReadService shopStockRuleReadService;

    @RpcConsumer
    private WarehouseShopStockRuleWriteService shopStockRuleWriteService;

    @Autowired
    private UserManageShopReader userManageShopReader;

    @Autowired
    private EventBus eventBus;

    /**
     * 创建店铺库存发货规则
     *
     * @param warehouseShopStockRule 店铺库存发货规则
     * @return 新创建的规则id
     */
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long create(@RequestBody WarehouseShopStockRule warehouseShopStockRule){
        authCheck(warehouseShopStockRule.getShopId());
        Response<Long> r = shopStockRuleWriteService.create(warehouseShopStockRule);
        if(!r.isSuccess()){
            log.error("failed to create {}, error code:{}", warehouseShopStockRule, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }


    /**
     * 检查当前登录用户是否有操作对应店铺的权限
     *
     * @param shopId 店铺id
     */
    private void authCheck(Long shopId) {
        ParanaUser user = UserUtil.getCurrentUser();
        List<OpenClientShop> shops =  userManageShopReader.findManageShops(user);
        List<Long> shopIds = Lists.newArrayListWithCapacity(shops.size());
        for (OpenClientShop shop : shops) {
            shopIds.add(shop.getOpenShopId());
        }
        if(!shopIds.contains(shopId)){
            log.error("user({}) can not create shopStockRule for  shop(id={})", user, shopId);
            throw new JsonResponseException("shop.not.allowed");
        }
    }

    /**
     * 列出当前用户能查看的店铺规则
     *
     * @param shopId 店铺id, 可选参数
     * @param pageNo 起始页面, 可选参数
     * @param pageSize 每页返回条数, 可选参数
     * @return 对应的店铺库存分配规则
     */
    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<WarehouseShopStockRule> pagination(@RequestParam(required = false, value = "shopId") Long shopId,
                                        @RequestParam(required = false, value = "pageNo") Integer pageNo,
                                        @RequestParam(required = false, value = "pageSize") Integer pageSize) {

        ParanaUser user = UserUtil.getCurrentUser();

        List<OpenClientShop> shops =  userManageShopReader.findManageShops(user);
        List<Long> shopIds = Lists.newArrayListWithCapacity(shops.size());
        for (OpenClientShop shop : shops) {
            shopIds.add(shop.getOpenShopId());
        }
        if(shopId!=null){
            if(!shopIds.contains(shopId)){
                return Paging.empty();
            }
            Response<WarehouseShopStockRule> r = shopStockRuleReadService.findByShopId(shopId);
            if(!r.isSuccess()){
                log.error("failed to find shopStockRule where id={}, error code:{}", shopId, r.getError());
                throw new JsonResponseException(r.getError());
            }
            if(r.getResult() == null){
                return Paging.empty();
            }
            return new Paging<>(1L, Lists.newArrayList(r.getResult()));
        }

        Response<Paging<WarehouseShopStockRule>> r =  shopStockRuleReadService.pagination(pageNo, pageSize,shopIds);
        if(!r.isSuccess()){
            log.error("failed to find WarehouseShopStockRule for {}, error code:{}", user, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    /**
     * 更新店铺推送规则
     *
     * @param id 规则id
     * @param warehouseShopStockRule 店铺推送规则
     * @return 是否成功
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean update(@PathVariable Long id, @RequestBody WarehouseShopStockRule warehouseShopStockRule){
        Response<WarehouseShopStockRule> r = shopStockRuleReadService.findById(id);
        if(!r.isSuccess()){
            log.error("failed to find WarehouseShopStockRule(id={}), error code:{}", id, r.getError());
            throw new JsonResponseException(r.getError());
        }
        Long shopId = r.getResult().getShopId();
        authCheck(shopId);
        warehouseShopStockRule.setId(id);
        warehouseShopStockRule.setShopId(shopId);
        Response<Boolean> rU = shopStockRuleWriteService.update(warehouseShopStockRule);
        if(!rU.isSuccess()){
            log.error("failed to update {}, error code:{}", warehouseShopStockRule, rU.getError());
            throw new JsonResponseException(rU.getError());
        }
        return rU.getResult();
    }

    /**
     * 读取店铺推送规则
     *
     * @param id 规则id
     * @return 是否成功
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public WarehouseShopStockRule findById(@PathVariable Long id){
        Response<WarehouseShopStockRule> r = shopStockRuleReadService.findById(id);
        if(!r.isSuccess()){
            log.error("failed to find WarehouseShopStockRule(id={}), error code:{}", id, r.getError());
            throw new JsonResponseException(r.getError());
        }
        //检查用户是否有读取这个规则的权限
        Long shopId = r.getResult().getShopId();
        authCheck(shopId);
        return r.getResult();
    }

    /**
     * 推送指定店铺的库存
     *
     * @param shopId 店铺id
     * @return 是否发送请求
     */
    @RequestMapping(value = "/push", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean push(@RequestParam("shopId")Long shopId){
        eventBus.post(new PushEvent(shopId));
        return Boolean.TRUE;
    }


}
