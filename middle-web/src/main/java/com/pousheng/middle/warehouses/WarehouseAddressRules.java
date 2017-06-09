package com.pousheng.middle.warehouses;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.pousheng.middle.warehouse.cache.WarehouseAddressCacher;
import com.pousheng.middle.warehouse.dto.WarehouseAddressTree;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import com.pousheng.middle.warehouse.service.WarehouseAddressRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseAddressRuleWriteService;
import com.pousheng.middle.warehouses.tree.TreeMarker;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * 创建规则适用的地址信息, 返回rule id
     *
     * @param addresses 对应的地址数组, 注意, 只需要将全选的节点提交上来即可, 如果父节点全选, 那么子节点也不必提交了, 默认全选
     * @return rule id 新生成的规则id
     */
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long create(@RequestBody WarehouseAddress[] addresses) {


        return null;
    }

    @RequestMapping(value = "/address", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public WarehouseAddressTree findAddressByRuleId(@RequestParam Long ruleId) {
        Response<List<WarehouseAddress>> r = warehouseAddressRuleReadService.findAddressByRuleId(ruleId);
        if (!r.isSuccess()) {
            log.error("failed to find address for warehouse rule(id={}), error code:{}", ruleId, r.getError());
            throw new JsonResponseException(r.getError());
        }
        WarehouseAddressTree warehouseAddressTree = warehouseAddressCacher.buildTree(2);
        List<WarehouseAddress> warehouseAddresses = r.getResult();
        for (WarehouseAddress warehouseAddress : warehouseAddresses) {
            Long id = warehouseAddress.getId();
            treeMarker.markSelected(warehouseAddressTree, id);
        }
        return warehouseAddressTree;
    }

}
