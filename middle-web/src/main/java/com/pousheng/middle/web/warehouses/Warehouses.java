package com.pousheng.middle.web.warehouses;

import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.warehouse.service.WarehouseWriteService;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-08
 */
@RestController
@RequestMapping("/api/warehouse")
@Slf4j
@OperationLogModule(OperationLogModule.Module.WAREHOUSE)
public class Warehouses {

    @RpcConsumer
    private WarehouseWriteService warehouseWriteService;

    @RpcConsumer
    private WarehouseReadService warehouseReadService;

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("新建")
    public Long create(@RequestBody Warehouse warehouse) {
        Response<Long> r = warehouseWriteService.create(warehouse);
        if (!r.isSuccess()) {
            log.error("failed to batchCreate {}, error code:{}", warehouse, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    @RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("更新")
    public Boolean update(@RequestBody Warehouse warehouse) {
        Response<Boolean> r = warehouseWriteService.update(warehouse);
        if (!r.isSuccess()) {
            log.error("failed to update {}, error code:{}", warehouse, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean delete(@PathVariable("id") Long id) {
        Response<Boolean> r = warehouseWriteService.deleteById(id);
        if (!r.isSuccess()) {
            log.error("failed to delete warehouse(id={}), error code:{} ",
                    id, r.getError());
            throw new JsonResponseException(r.getError());
        }

        return true;
    }

    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<Warehouse> pagination(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                        @RequestParam(required = false, value = "pageSize") Integer pageSize,
                                        @RequestParam(required = false, value = "code") String code,
                                        @RequestParam(required = false, value="codePrefix") String codePrefix,
                                        @RequestParam(required = false, value = "name") String namePrefix) {
        Map<String, Object> params = Maps.newHashMap();
        if (StringUtils.hasText(code)) {
            params.put("code", code);
        }
        if (StringUtils.hasText(codePrefix)) {
            params.put("codePrefix", codePrefix);
        }
        if (StringUtils.hasText(namePrefix)) {
            params.put("name", namePrefix);
        }
        Response<Paging<Warehouse>> r = warehouseReadService.pagination(pageNo, pageSize, params);
        if(!r.isSuccess()){
            log.error("failed to pagination warehouse with params:{}, error code:{}", params, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();

    }


}
