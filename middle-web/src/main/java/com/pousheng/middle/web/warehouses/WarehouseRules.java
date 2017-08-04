package com.pousheng.middle.web.warehouses;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.pousheng.middle.warehouse.dto.RuleGroup;
import com.pousheng.middle.warehouse.dto.ThinShop;
import com.pousheng.middle.warehouse.model.WarehouseShopGroup;
import com.pousheng.middle.warehouse.service.WarehouseRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseRuleWriteService;
import com.pousheng.middle.warehouse.service.WarehouseShopGroupReadService;
import com.pousheng.middle.warehouse.service.WarehouseShopRuleWriteService;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * 店铺规则
 * <p>
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-20
 */
@RestController
@RequestMapping("/api/warehouse/rule")
@Slf4j
@OperationLogModule(OperationLogModule.Module.WAREHOUSE_RULE)
public class WarehouseRules {

    @RpcConsumer
    private WarehouseRuleReadService warehouseRuleReadService;

    @RpcConsumer
    private WarehouseRuleWriteService warehouseRuleWriteService;

    @RpcConsumer
    private WarehouseShopRuleWriteService warehouseShopRuleWriteService;

    @RpcConsumer
    private OpenShopReadService openShopReadService;

    @RpcConsumer
    private WarehouseShopGroupReadService warehouseShopGroupReadService;


    /**
     * 创建规则适用的地址信息, 同时会创建仓库发货优先级规则, 并返回新创建的rule id
     *
     * @param shops 勾选的店铺
     * @return rule id 新生成的规则id
     */
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("创建")
    public Long create(@RequestBody ThinShop[] shops) {

        Response<Long> r = warehouseShopRuleWriteService.batchCreate(Lists.newArrayList(shops));
        if (!r.isSuccess()) {
            log.error("failed to batchCreate warehouse rule with shops:{}, error code:{}", shops, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }


    /**
     * 创建规则适用的地址信息, 同时会创建仓库发货优先级规则, 并返回新创建的rule id
     *
     * @param shops 勾选的店铺
     * @return rule id 新生成的规则id
     */
    @RequestMapping(value="/{ruleId}",method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("编辑")
    public Boolean update(@PathVariable Long ruleId,  @RequestBody ThinShop[] shops) {

        Response<Boolean> r = warehouseShopRuleWriteService.batchUpdate(ruleId, Lists.newArrayList(shops));
        if (!r.isSuccess()) {
            log.error("failed to batch update warehouse rule(id={}) with shops:{}, error code:{}",
                    ruleId, shops, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }


    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<RuleGroup> pagination(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                        @RequestParam(required = false, value = "pageSize") Integer pageSize) {

        Response<Paging<RuleGroup>> r = warehouseRuleReadService.pagination(pageNo, pageSize);
        if (!r.isSuccess()) {
            log.error("failed to pagination rule group, error code:{}", r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    /**
     * 删除单条规则
     *
     * @param ruleId 规则id
     * @return 是否删除成功
     */
    @RequestMapping(value = "/{ruleId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean delete(@PathVariable Long ruleId) {
        Response<Boolean> r = warehouseRuleWriteService.deleteById(ruleId);
        if (!r.isSuccess()) {
            log.error("failed to delete warehouse rule(id={}), error code:{}", ruleId, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    /**
     * 删除店铺组, 同时删除对应的发货规则记录
     *
     * @param groupId 店铺组id
     * @return 是否删除成功
     */
    @RequestMapping(value = "/group/{groupId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean deleteShopGroup(@PathVariable Long groupId){
        Response<Boolean> r = warehouseShopRuleWriteService.deleteByShopGroupId(groupId);
        if (!r.isSuccess()) {
            log.error("failed to delete warehouse shop group(id={}), error code:{}", groupId, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    @RequestMapping(value = "/shops", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ThinShop> markShops(@RequestParam(value = "groupId", required = false) Long groupId) {
        //获取店铺列表集合
        List<ThinShop> thinShops = findAllCandidateShops();

        //标记所有已设置发货规则的店铺不可被编辑
        disableRuleShops( thinShops);


        //标记当前规则选的店铺可以编辑
        if(groupId!=null) {
            enableCurrentRuleShops(groupId, thinShops);
        }
        return thinShops;
    }

    private List<ThinShop> findAllCandidateShops() {

        Response<List<OpenShop>> r = openShopReadService.findAll();
        if (!r.isSuccess()) {
            log.error("failed to find open shops, error code:{}", r.getError());
            throw new JsonResponseException(r.getError());
        }
        List<OpenShop> openShops = r.getResult();
        List<ThinShop> thinShops = Lists.newArrayListWithCapacity(openShops.size());
        for (OpenShop openShop : openShops) {
            ThinShop thinShop = new ThinShop();
            thinShop.setShopId(openShop.getId());
            thinShop.setShopName(openShop.getChannel()+"-"+openShop.getShopName());
            thinShops.add(thinShop);
        }
        return thinShops;
    }

    private void disableRuleShops( List<ThinShop> thinShops) {
        //获取所有已设置规则的店铺
        Response<Set<Long>> rShopIds = warehouseShopGroupReadService.findShopIds();
        if (!rShopIds.isSuccess()) {
            log.error("failed to find shopIds which have warehouse rules set, error code :{} ",
                    rShopIds.getError());
            throw new JsonResponseException(rShopIds.getError());
        }

        //标记所有已设置发货规则的店铺不可被编辑
        Set<Long> shopIds = rShopIds.getResult();

        for (ThinShop thinShop : thinShops) {
            if (shopIds.contains(thinShop.getShopId())) {
                thinShop.setEditable(false);
            }
        }
    }

    //标记当前规则选的店铺可以编辑
    private void enableCurrentRuleShops(Long shopGroupId, List<ThinShop> thinShops) {
        Response<List<WarehouseShopGroup>> rwsrs = warehouseShopGroupReadService.findByGroupId(shopGroupId);
        if (!rwsrs.isSuccess()) {
            log.error("failed to find warehouseShopGroups by shopGroupId={}, error code:{}", shopGroupId, rwsrs.getError());
            throw new JsonResponseException(rwsrs.getError());
        }
        for (WarehouseShopGroup warehouseShopGroup : rwsrs.getResult()) {
            Long shopId = warehouseShopGroup.getShopId();
            for (ThinShop thinShop : thinShops) {
                if (Objects.equal(thinShop.getShopId(), shopId)) {
                    thinShop.setEditable(true);
                    thinShop.setSelected(true);
                }
            }
        }
    }
}
