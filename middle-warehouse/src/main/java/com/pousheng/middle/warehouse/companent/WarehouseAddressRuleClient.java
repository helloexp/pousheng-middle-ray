package com.pousheng.middle.warehouse.companent;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.warehouse.dto.Warehouses4Address;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;

/**
 * API调用 - 派单规则中有关配货地址范围的操作
 *
 * @auther feisheng.ch
 * @time 2018/5/26
 */
@Component
@Slf4j
public class WarehouseAddressRuleClient {

    @Autowired
    private InventoryBaseClient inventoryBaseClient;

    /**
     * 根据层级地址, 返回满足条件的仓库, 最精确的地址优先
     *
     * @param shopId
     * @param addressIds
     * @return
     */
    public Response<List<Warehouses4Address>> findByReceiverAddressIds (Long shopId, List<Long> addressIds) {
        if (null == shopId || ObjectUtils.isEmpty(addressIds)) {
            return Response.fail("warehouse.rule.address.find.fail.parameter");
        }
        try {
            return Response.ok((List<Warehouses4Address>) inventoryBaseClient.get(
                    "api/inventory/warehouse/rule/address/query/findByReceiverAddressIds",
                    null, null,
                    ImmutableMap.of("shopId", shopId, "addressIds", JSON.toJSONString(addressIds)),
                    Warehouses4Address.class,
                    true));

        } catch (Exception e) {
            log.error("find warehouse list by receiver address fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }

    }

}
