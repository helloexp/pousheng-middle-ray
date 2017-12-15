package com.pousheng.middle.web.address;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.cache.WarehouseAddressCacher;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by songrenfei on 2017/12/15
 */
@RestController
@Slf4j
public class WarehouseAddresss {

    @Autowired
    private WarehouseAddressCacher warehouseAddressCacher;

    /**
     * 获取某一个地址的所有下级地址
     *
     * @param addressId id
     * @return 下级地址列表
     */
    @RequestMapping(value = "/api/middle/address/{id}/children", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<List<WarehouseAddress>> childAddressOf(@PathVariable("id") Long addressId) {
        try {
            log.debug("START TEST LOG find child address by parent id:{} by cache",addressId);
            List<WarehouseAddress> warehouseAddress= warehouseAddressCacher.findByPid(addressId);
            log.debug("END TEST LOG find child address by parent id:{}  by cache",addressId);
            return Response.ok(warehouseAddress);
        } catch (Exception e) {
            log.error("fail to find child address by pid {} for cache, cause:{}", addressId,
                    Throwables.getStackTraceAsString(e));
            return Response.fail("address.query.fail");
        }
    }
    /**
     * 根据pid获取下级地址信息
     * @param id
     * @return
     */
    @RequestMapping(value = "/api/warehouse/address/{id}/children",method = RequestMethod.GET,produces=MediaType.APPLICATION_JSON_VALUE)
    public Response<List<WarehouseAddress>> findWarehouseAddressByPid(@PathVariable("id")long id){
       return this.childAddressOf(id);
    }

}
