package com.pousheng.middle.warehouses;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.pousheng.middle.warehouse.cache.WarehouseAddressCacher;
import com.pousheng.middle.warehouse.dto.AddressTree;
import com.pousheng.middle.warehouse.dto.ThinAddress;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import com.pousheng.middle.warehouse.service.WarehouseAddressRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseAddressRuleWriteService;
import com.pousheng.middle.warehouses.algrithm.TreeMarker;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

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
    private WarehouseAddressCacher warehouseAddressCacher;

    @Autowired
    private TreeMarker treeMarker;

    /**
     * 创建规则适用的地址信息, 同时会创建仓库发货优先级规则, 并返回新创建的rule id
     *
     * @param addresses 对应的地址数组, 注意, 只需要将全选的节点提交上来即可, 部分选择和不选择的节点不要提交
     * @return rule id 新生成的规则id
     */
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long create(@RequestBody ThinAddress[] addresses) {
        //需要过滤掉本次提交中冗余的地址,如果父节点全选了, 那么子节点就可以过滤掉了
        List<ThinAddress> valid = refineWarehouseAddress(addresses);
        Response<Long> r = warehouseAddressRuleWriteService.batchCreate(valid);
        if(!r.isSuccess()){
            log.error("failed to create warehouse rule with addresses:{}, error code:{}", valid, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
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
        WarehouseAddress current = warehouseAddressCacher.findById(addressId);
        if(current == null){
            log.error("WarehouseAddress(id={}) not exists", addressId);
            throw new JsonResponseException("warehouseAddress.not.exists");
        }
        List<Long> parentIds = Lists.newArrayListWithExpectedSize(3);
        while(current!=null && current.getPid()>0){
            Long pid = current.getPid();
            parentIds.add(pid);
            current =  warehouseAddressCacher.findById(pid);
        }
        return parentIds;
    }

    /**
     * 编辑规则时需要对应规则关联的发货地址信息
     *
     * @param ruleId 规则id
     * @return  地址树形结构, 并已标记选中状态
     */
    @RequestMapping(value = "/{ruleId}/address", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public AddressTree findAddressByRuleId(@PathVariable Long ruleId) {

        //其他规则选中的地址不可编辑
        Response<List<ThinAddress>> r = warehouseAddressRuleReadService.findAllNoneDefaultAddresses();
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

    @RequestMapping(value = "/address", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public AddressTree findSelectedAddress(){
        //其他规则选中的地址不可编辑
        Response<List<ThinAddress>> r = warehouseAddressRuleReadService.findAllNoneDefaultAddresses();
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
    }

}
