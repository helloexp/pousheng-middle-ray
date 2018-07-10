package com.pousheng.middle.web.item.group;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.pousheng.middle.group.model.*;
import com.pousheng.middle.item.enums.ItemRuleType;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.warehouse.enums.WarehouseType;
import com.pousheng.middle.web.item.cacher.GroupRuleCacherProxy;
import com.pousheng.middle.group.dto.ItemRuleCriteria;
import com.pousheng.middle.group.dto.ItemRuleDetail;
import com.pousheng.middle.group.service.*;
import com.pousheng.middle.web.shop.cache.ShopChannelGroupCacher;
import com.pousheng.middle.web.shop.dto.ShopChannel;
import com.pousheng.middle.web.shop.dto.ShopChannelGroup;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.Joiners;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zhaoxw
 * @date 2018/5/8
 */
@Api(description = "商品规则管理")
@RestController
@Slf4j
@RequestMapping("api/item/rule")
public class ItemRules {

    @RpcConsumer
    @Setter
    private ItemRuleWriteService itemRuleWriteService;
    @RpcConsumer
    @Setter
    private ItemRuleReadService itemRuleReadService;
    @RpcConsumer
    @Setter
    private ItemRuleShopReadService itemRuleShopReadService;
    @RpcConsumer
    @Setter
    private ItemRuleWarehouseReadService itemRuleWarehouseReadService;
    @RpcConsumer
    @Setter
    private ItemRuleGroupReadService itemRuleGroupReadService;
    @RpcConsumer
    @Setter
    private ItemGroupReadService itemGroupReadService;
    @Autowired
    @Setter
    private ShopChannelGroupCacher shopChannelGroupCacher;
    @Autowired
    @Setter
    private OpenShopCacher openShopCacher;
    @Autowired
    @Setter
    private GroupRuleCacherProxy groupRuleCacherProxy;
    @Autowired
    private WarehouseCacher warehouseCacher;


    @ApiOperation("获取商品规则分页列表")
    @GetMapping("/paging")
    public Paging<ItemRuleDetail> findBy(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                         @RequestParam(required = false, value = "pageSize") Integer pageSize,
                                         @RequestParam(required = false, value = "id") Long id,
                                         Integer type) {
        ItemRuleCriteria criteria = new ItemRuleCriteria();
        criteria.setId(id);
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);
        criteria.setType(type);
        Response<Paging<ItemRule>> r = itemRuleReadService.paging(criteria);
        if (!r.isSuccess()) {
            log.error("failed to pagination rule group, error code:{}", r.getError());
            throw new JsonResponseException(r.getError());
        }
        return transToDetail(r.getResult(),type);
    }

    @ApiOperation("创建商品规则")
    @PostMapping("/{type}/create")
    public Long create(@RequestBody Long[] ids,@PathVariable Integer type) {
        if (ids.length == 0) {
            throw new JsonResponseException("at.least.one.object");
        }
        Response<Long> createResp;
        if (type.equals(ItemRuleType.SHOP.value())) {
            checkShopIds(null, ids);
            createResp = itemRuleWriteService.createWithShop(Lists.newArrayList(ids));
        } else {
            checkWarehouseIds(null, ids);
            createResp = itemRuleWriteService.createWithWarehouse(Lists.newArrayList(ids));
        }
        if (!createResp.isSuccess()) {
            log.error("fail to create item rule,cause:{}", createResp.getError());
            throw new JsonResponseException(createResp.getError());
        }
        return createResp.getResult();
    }

    @ApiOperation("修改规则的店铺信息")
    @PutMapping("/{ruleId}/shop")
    public boolean updateShop(@PathVariable Long ruleId, @RequestBody Long[] shopIds) {
        checkShopIds(ruleId,shopIds);
        Response<Boolean> updateResp = itemRuleWriteService.updateShops(ruleId, Lists.newArrayList(shopIds));
        if (!updateResp.isSuccess()) {
            log.error("fail to update item rule shop id:{} shopIds:{} ,cause:{}",
                    ruleId, shopIds, updateResp.getError());
            throw new JsonResponseException(updateResp.getError());
        }
        shopChannelGroupCacher.refreshShopChannelGroupCache();
        groupRuleCacherProxy.refreshAll();
        return updateResp.getResult();
    }

    @ApiOperation("修改规则的仓库信息")
    @PutMapping("/{ruleId}/warehouse")
    public boolean updateWarehouse(@PathVariable Long ruleId, @RequestBody Long[] warehouseIds) {
        checkWarehouseIds(ruleId,warehouseIds);
        Response<Boolean> updateResp = itemRuleWriteService.updateWarehouses(ruleId, Lists.newArrayList(warehouseIds));
        if (!updateResp.isSuccess()) {
            log.error("fail to update item rule id:{} warehouseIds:{} ,cause:{}",
                    ruleId, warehouseIds, updateResp.getError());
            throw new JsonResponseException(updateResp.getError());
        }
        groupRuleCacherProxy.refreshAll();
        return updateResp.getResult();
    }

    @ApiOperation("修改规则的分组标签")
    @PutMapping("/{ruleId}/group")
    public boolean updateGroup(@PathVariable Long ruleId, @RequestBody Long[] groupIds) {
        Response<Boolean> updateResp = itemRuleWriteService.updateGroups(ruleId, Lists.newArrayList(groupIds));
        if (!updateResp.isSuccess()) {
            log.error("fail to update item rule group id:{} shopIds:{} ,cause:{}",
                    ruleId, groupIds, updateResp.getError());
            throw new JsonResponseException(updateResp.getError());
        }
        groupRuleCacherProxy.refreshAll();
        return updateResp.getResult();
    }


    @ApiOperation("删除商品规则")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        Response<Boolean> deleteResp = itemRuleWriteService.delete(id);
        if (!deleteResp.isSuccess()) {
            log.error("fail to delete item group by id:{},cause:{}",
                    id, deleteResp.getError());
            throw new JsonResponseException(deleteResp.getError());
        }
        shopChannelGroupCacher.refreshShopChannelGroupCache();
        groupRuleCacherProxy.refreshAll();
        return deleteResp.getResult();
    }

    @ApiOperation("获取店铺列表")
    @GetMapping(value = "/shops", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ShopChannelGroup> markShops(@RequestParam(value = "ruleId", required = false) Long ruleId) {
        //获取店铺列表集合
        List<ShopChannelGroup> channelGroups = shopChannelGroupCacher.listAllShopChannelGroupCache();
        //标记所有属于其他规则的店铺为不可被编辑
        disableRuleShops(channelGroups);
        //标记当前规则选的店铺可以编辑
        if (ruleId != null) {
            enableCurrentRuleShops(ruleId, channelGroups);
        }
        return channelGroups;
    }


    @ApiOperation("获取仓库列表")
    @GetMapping(value = "/warehouses", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<WarehouseDTO> markWarehouses(@RequestParam(value = "ruleId", required = false) Long ruleId) {
        //获取仓库列表集合
        Response<List<ItemRuleWarehouse>> resp = itemRuleWarehouseReadService.findByRuleId(ruleId);
        if (!resp.isSuccess()) {
            throw new JsonResponseException(resp.getError());
        }
        List<WarehouseDTO> list = Lists.newArrayList();
        List<Long> warehouseIds = resp.getResult().stream().map(ItemRuleWarehouse::getWarehouseId).collect(Collectors.toList());
        for (Long warehouseId : warehouseIds) {
            list.add(warehouseCacher.findById(warehouseId));
        }
        return list;
    }



    private void checkShopIds(Long ruleId,Long[] shopIds){
        Response<Boolean> checkResp = itemRuleShopReadService.checkShopIds(ruleId, Lists.newArrayList(shopIds));
        if (!checkResp.isSuccess()) {
            log.error("fail to check item rule shop,cause:{}", checkResp.getError());
            throw new JsonResponseException(checkResp.getError());
        }
        if (checkResp.getResult()) {
            throw new JsonResponseException("shop.belong.to.other.rule");
        }
    }

    @ApiOperation("检查仓库能否添加")
    @GetMapping("/check/warehouse")
    public boolean checkWarehouseIds( @RequestParam(required = false, value = "ruleId") Long ruleId,
                                   Long[] warehouseIds) {
       for (Long warehouseId : warehouseIds) {
            WarehouseDTO warehouse = warehouseCacher.findById(warehouseId);
            if (null == warehouse){
                log.warn("find warehouse by id {} is null",warehouseId);
                throw new JsonResponseException("warehouse.is.not.exist");
            }
            if (java.util.Objects.equals(WarehouseType.SHOP_WAREHOUSE.value(),warehouse.getWarehouseSubType())){
                throw new JsonResponseException("contain.shop.warehouse");
            }
        }
        Response<Boolean> checkResp = itemRuleWarehouseReadService.checkWarehouseIds(ruleId, Lists.newArrayList(warehouseIds));
        if (!checkResp.isSuccess()) {
            log.error("fail to check item rule warehouse,cause:{}", checkResp.getError());
            throw new JsonResponseException(checkResp.getError());
        }
        if (checkResp.getResult()) {
            throw new JsonResponseException("warehouse.belong.to.other.rule");
        }
        return true;
    }

    private void disableRuleShops(List<ShopChannelGroup> channelGroups) {
        //获取所有已设置规则的店铺
        Response<List<Long>> rShopIds = itemRuleShopReadService.findShopIds();
        if (!rShopIds.isSuccess()) {
            log.error("failed to find shopIds which have warehouse rules set, error code :{} ",
                    rShopIds.getError());
            throw new JsonResponseException(rShopIds.getError());
        }

        //标记所有已设置发货规则的店铺不可被编辑
        List<Long> shopIds = rShopIds.getResult();
        for (ShopChannelGroup channelGroup : channelGroups) {
            List<ShopChannel> shopChannels = channelGroup.getShopChannels();
            for (ShopChannel shopChannel : shopChannels) {
                OpenClientShop openClientShop = shopChannel.getOpenClientShop();
                if (Arguments.notNull(openClientShop)) {
                    if (shopIds.contains(openClientShop.getOpenShopId())) {
                        openClientShop.setEditable(false);
                    }
                }
                List<OpenClientShop> zoneOpenClientShops = shopChannel.getZoneOpenClientShops();
                if (!CollectionUtils.isEmpty(zoneOpenClientShops)) {
                    for (OpenClientShop openShop : zoneOpenClientShops) {
                        if (shopIds.contains(openShop.getOpenShopId())) {
                            openShop.setEditable(false);
                        }
                    }
                }

            }
        }
    }

    //标记当前规则选的店铺可以编辑
    private void enableCurrentRuleShops(Long ruleId, List<ShopChannelGroup> channelGroups) {
        Response<List<ItemRuleShop>> rwsrs = itemRuleShopReadService.findByRuleId(ruleId);
        if (!rwsrs.isSuccess()) {
            log.error("failed to find warehouseShopGroups by ruleId={}, error code:{}", ruleId, rwsrs.getError());
            throw new JsonResponseException(rwsrs.getError());
        }
        for (ItemRuleShop itemRuleShop : rwsrs.getResult()) {
            Long shopId = itemRuleShop.getShopId();
            for (ShopChannelGroup channelGroup : channelGroups) {
                List<ShopChannel> shopChannels = channelGroup.getShopChannels();
                for (ShopChannel shopChannel : shopChannels) {
                    OpenClientShop openClientShop = shopChannel.getOpenClientShop();
                    if (Arguments.notNull(openClientShop)) {
                        if (Objects.equal(openClientShop.getOpenShopId(), shopId)) {
                            openClientShop.setEditable(true);
                            openClientShop.setSelected(true);
                        }
                    }
                    List<OpenClientShop> zoneOpenClientShops = shopChannel.getZoneOpenClientShops();
                    if (!CollectionUtils.isEmpty(zoneOpenClientShops)) {
                        for (OpenClientShop openShop : zoneOpenClientShops) {
                            if (Objects.equal(openShop.getOpenShopId(), shopId)) {
                                openShop.setEditable(true);
                                openShop.setSelected(true);
                            }
                        }
                    }

                }
            }
        }
    }


    private Paging<ItemRuleDetail> transToDetail(Paging<ItemRule> paging, Integer type) {
        Paging<ItemRuleDetail> detailPaging = new Paging<>();
        detailPaging.setTotal(paging.getTotal());
        detailPaging.setData(Lists.newArrayList());
        for (ItemRule itemRule : paging.getData()) {
            ItemRuleDetail detail = new ItemRuleDetail().id(itemRule.getId());
            if(type.equals(ItemRuleType.SHOP.value())){
                Response<List<ItemRuleShop>> itemRuleShopsResp = itemRuleShopReadService.findByRuleId(itemRule.getId());
                if (!itemRuleShopsResp.isSuccess()) {
                    throw new JsonResponseException(itemRuleShopsResp.getError());
                }
                if (!CollectionUtils.isEmpty(itemRuleShopsResp.getResult())) {
                    List<String> shopNames = Lists.newArrayList();
                    for (ItemRuleShop itemRuleShop : itemRuleShopsResp.getResult()) {
                        if (openShopCacher.findById(itemRuleShop.getShopId()) != null) {
                            shopNames.add(openShopCacher.findById(itemRuleShop.getShopId()).getShopName());
                        }
                    }
                    detail.shopNames(Joiners.COMMA.join(shopNames));
                }
            }
            if(type.equals(ItemRuleType.WAREHOUSE.value())){
                Response<List<ItemRuleWarehouse>> itemRuleWarehouseResp = itemRuleWarehouseReadService.findByRuleId(itemRule.getId());
                if (!itemRuleWarehouseResp.isSuccess()) {
                    throw new JsonResponseException(itemRuleWarehouseResp.getError());
                }
                if (!CollectionUtils.isEmpty(itemRuleWarehouseResp.getResult())) {
                    List<String> warehouseNames = Lists.newArrayList();
                    for (ItemRuleWarehouse itemRuleWarehouse : itemRuleWarehouseResp.getResult()) {
                        if (warehouseCacher.findById(itemRuleWarehouse.getWarehouseId()) != null) {
                            warehouseNames.add(warehouseCacher.findById(itemRuleWarehouse.getWarehouseId()).getWarehouseName());
                        }
                    }
                    detail.setWarehouseNames(Joiners.COMMA.join(warehouseNames));
                }
            }
            Response<List<ItemRuleGroup>> itemRuleGroupsResp = itemRuleGroupReadService.findByRuleId(itemRule.getId());
            if (!itemRuleGroupsResp.isSuccess()) {
                throw new JsonResponseException(itemRuleGroupsResp.getError());
            }
            List<ItemGroup> groups = Lists.newArrayList();
            if (!CollectionUtils.isEmpty(itemRuleGroupsResp.getResult())) {
                List<Long> groupIds = itemRuleGroupsResp.getResult().stream().map(ItemRuleGroup::getGroupId).collect(Collectors.toList());
                Response<List<ItemGroup>> groupsResp = itemGroupReadService.findByIds(groupIds);
                if (!groupsResp.isSuccess()) {
                    throw new JsonResponseException(groupsResp.getError());
                }
                groups.addAll(groupsResp.getResult());
            }
            detail.groups(groups);
            detailPaging.getData().add(detail);
        }
        return detailPaging;
    }
}
