package com.pousheng.middle.web.warehouses;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.pousheng.middle.warehouse.cache.WarehouseAddressCacher;
import com.pousheng.middle.warehouse.dto.AddressTree;
import com.pousheng.middle.warehouse.dto.ThinAddress;
import com.pousheng.middle.warehouse.dto.ThinShop;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import com.pousheng.middle.warehouse.model.WarehouseShopRule;
import com.pousheng.middle.warehouse.service.*;
import com.pousheng.middle.web.warehouses.algorithm.TreeMarker;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-08
 */
@RestController
@RequestMapping("/api/warehouse/rule")
@Slf4j
public class WarehouseAddressRules {

    @RpcConsumer
    private WarehouseAddressRuleWriteService warehouseAddressRuleWriteService;

    @RpcConsumer
    private WarehouseAddressRuleReadService warehouseAddressRuleReadService;

    @RpcConsumer
    private WarehouseShopRuleWriteService warehouseShopRuleWriteService;

    @RpcConsumer
    private WarehouseShopRuleReadService warehouseShopRuleReadService;

    @RpcConsumer
    private WarehouseRuleReadService warehouseRuleReadService;

    @Autowired
    private WarehouseAddressCacher warehouseAddressCacher;

    @Autowired
    private TreeMarker treeMarker;

    /**
     * 创建规则适用的地址信息, 同时会创建仓库发货优先级规则, 并返回新创建的rule id
     *
     * @param shops 勾选的店铺
     * @return rule id 新生成的规则id
     */
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long create(@RequestBody ThinShop[] shops) {

        Response<Long> r = warehouseShopRuleWriteService.batchCreate(Lists.newArrayList(shops));
        if(!r.isSuccess()){
            log.error("failed to batchCreate warehouse rule with shops:{}, error code:{}", shops, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    /**
     * 创建规则适用的地址信息, 同时会创建仓库发货优先级规则, 并返回新创建的rule id
     *
     * @param addresses 对应的地址数组, 注意, 只需要将全选的节点提交上来即可, 部分选择和不选择的节点不要提交
     * @param  ruleId 规则id
     * @return  ruleId 规则id
     */
    @RequestMapping(value="/{ruleId}/addresses",method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long create(@PathVariable("ruleId")Long ruleId,
                       @RequestBody ThinAddress[] addresses) {
        //需要过滤掉本次提交中冗余的地址,如果父节点全选了, 那么子节点就可以过滤掉了
        List<ThinAddress> valid = refineWarehouseAddress(addresses);
        Set<Long> shopIds = findShopIdsByRuleId(ruleId);
        Response<Long> r = warehouseAddressRuleWriteService.batchCreate(Lists.newArrayList(shopIds), ruleId, valid);
        if(!r.isSuccess()){
            log.error("failed to batchCreate warehouse rule(id={}) with addresses:{}, error code:{}",
                    ruleId, valid, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    /**
     * 根据规则id查找关联的店铺id列表
     *
     * @param ruleId 规则id
     * @return 关联的店铺id列表
     */
    private Set<Long> findShopIdsByRuleId(@PathVariable("ruleId") Long ruleId) {
        Response<List<WarehouseShopRule>> rwsr = warehouseShopRuleReadService.findByRuleId(ruleId);
        if(!rwsr.isSuccess()){
            log.error("failed to find warehouseShopRules for rule(id={}),error code:{}", ruleId, rwsr.getError());
            throw new JsonResponseException(rwsr.getError());
        }
        List<WarehouseShopRule> wsrs = rwsr.getResult();
        if(CollectionUtils.isEmpty(wsrs)){
            log.error("no shops specified for rule(id={})", ruleId);
            throw new JsonResponseException("shop.missing");
        }
        Set<Long> shopIds = Sets.newHashSetWithExpectedSize(wsrs.size());
        for (WarehouseShopRule wsr : wsrs) {
            shopIds.add(wsr.getShopId());
        }
        return shopIds;
    }

    /**
     * 消除当前冗余的地址信息
     *
     * @param addresses 原始地址信息
     * @return 过滤后的地址信息
     */
    private List<ThinAddress> refineWarehouseAddress(ThinAddress[] addresses) {
        Set<Long> addressIds = Sets.newHashSetWithExpectedSize(addresses.length);
        for (ThinAddress address : addresses) {
            addressIds.add(address.getAddressId());
        }
        List<ThinAddress> valid = Lists.newArrayList();
        for (ThinAddress address : addresses) {
            Long addressId = address.getAddressId();
            List<Long> ancestorIds = findAncestorIds(addressId);
            boolean shouldIn = true;
            for (Long ancestorId : ancestorIds) {
                if(addressIds.contains(ancestorId)){
                    shouldIn = false;
                    break;
                }
            }
            if(shouldIn){
                valid.add(address);
            }

        }
        return valid;
    }

    /**
     * 查找当前地址节点的祖先id, 不包括自身id
     *
     * @param addressId 当前地址节点id
     * @return 祖先id列表
     */
    private List<Long> findAncestorIds(Long addressId) {
        if(addressId >1) {
            WarehouseAddress current = warehouseAddressCacher.findById(addressId);
            if (current == null) {
                log.error("WarehouseAddress(id={}) not exists", addressId);
                throw new JsonResponseException("warehouseAddress.not.exists");
            }
            List<Long> parentIds = Lists.newArrayListWithExpectedSize(3);
            while (current != null && current.getPid() > 1) {
                Long pid = current.getPid();
                parentIds.add(pid);
                current = warehouseAddressCacher.findById(pid);
            }
            return parentIds;
        }else{
            return Collections.emptyList();
        }
    }

    /**
     * 编辑规则时需要对应规则关联的发货地址信息
     *
     * @param ruleId 规则id
     * @return  地址树形结构, 并已标记选中状态
     */
    @RequestMapping(value = "/{ruleId}/address", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public AddressTree findAddressByRuleId(@PathVariable("ruleId") Long ruleId,
                                           @RequestParam("shopId") Long shopId) {

        //同店铺其他规则选中的地址不可编辑
        Response<List<ThinAddress>> r = warehouseAddressRuleReadService.findOtherNonDefaultAddressesByShopId(shopId, ruleId);
        if (!r.isSuccess()) {
            log.error("failed to find warehouse address for shop(id={}) , error code:{}",shopId, r.getError());
            throw new JsonResponseException(r.getError());
        }
        AddressTree addressTree = warehouseAddressCacher.buildTree(2);
        List<ThinAddress> warehouseAddresses = r.getResult();
        for (ThinAddress warehouseAddress : warehouseAddresses) {
            Long id = warehouseAddress.getAddressId();
            treeMarker.markSelected(addressTree, id, false);
        }

        //当前规则选中的地址可编辑
        Response<List<ThinAddress>> rCurrent = warehouseAddressRuleReadService.findAddressByRuleId(ruleId);
        if (!rCurrent.isSuccess()) {
            log.error("failed to find address for warehouse rule(id={}), error code:{}", ruleId, rCurrent.getError());
            throw new JsonResponseException(rCurrent.getError());
        }

        List<ThinAddress> currentRuleAddresses = rCurrent.getResult();
        for (ThinAddress warehouseAddress : currentRuleAddresses) {
            Long id = warehouseAddress.getAddressId();
            treeMarker.markSelected(addressTree, id, true);
        }
        return addressTree;
    }

    /**
     * 编辑规则关联的发货地址信息
     *
     * @param ruleId 规则id
     * @return  地址树形结构, 并已标记选中状态
     */
    @RequestMapping(value = "/{ruleId}/address", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean updateAddressByRuleId(@PathVariable Long ruleId, @RequestBody ThinAddress[] addresses){
        List<ThinAddress> valid = refineWarehouseAddress(addresses);
        Set<Long> shopIds = findShopIdsByRuleId(ruleId);
        Response<Boolean> r = warehouseAddressRuleWriteService.batchUpdate(Lists.newArrayList(shopIds), ruleId, valid);
        if(!r.isSuccess()){
            log.error("failed to update warehouse rule(id={}) with addresses:{}, error code:{}",
                    ruleId, valid, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return Boolean.TRUE;
    }

    /*@RequestMapping(value = "/address", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public AddressTree findSelectedAddress(){
        //其他规则选中的地址不可编辑
        Response<List<ThinAddress>> r = warehouseAddressRuleReadService.findOtherNonDefaultAddressesByShopId();
        if (!r.isSuccess()) {
            log.error("failed to find all warehouse address , error code:{}", r.getError());
            throw new JsonResponseException(r.getError());
        }
        AddressTree addressTree = warehouseAddressCacher.buildTree(2);
        List<ThinAddress> warehouseAddresses = r.getResult();
        for (ThinAddress warehouseAddress : warehouseAddresses) {
            Long id = warehouseAddress.getAddressId();
            treeMarker.markSelected(addressTree, id, false);
        }
        return addressTree;
    }*/


}
