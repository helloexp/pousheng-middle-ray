package com.pousheng.middle.warehouses;

import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseAddressRuleWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;

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

    /**
     * 返回rule id
     * @param addresses 对应的地址数组
     * @return  rule id
     */
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long create(@RequestBody WarehouseAddress[] addresses){


        return null;
    }


    @Data
    public static class WarehouseAddress implements Serializable{

        private static final long serialVersionUID = -8928998224540423389L;

        private String addressName;

        private Integer addressId;

        private Integer addressType;
    }

}
