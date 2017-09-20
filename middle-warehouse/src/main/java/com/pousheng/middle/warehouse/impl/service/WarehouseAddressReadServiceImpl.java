package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseAddressDao;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import com.pousheng.middle.warehouse.service.WarehouseAddressReadService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by tony on 2017/7/25.
 * pousheng-middle
 */
@Slf4j
@Service
public class WarehouseAddressReadServiceImpl implements WarehouseAddressReadService {
    @Autowired
    private WarehouseAddressDao warehouseAddressDao;

    @Override
    public Response<WarehouseAddress> findByNameAndLevel(String addressName, Integer level) {

        try {
            WarehouseAddress warehouseAddress = warehouseAddressDao.findByNameAndLevel(addressName, level);
            if (warehouseAddress == null) {
                log.error("no WarehouseAddress found by name({}),level({})", addressName, level);
                return Response.fail("warehouse.address.not.exist");
            }
            return Response.ok(warehouseAddress);
        } catch (Exception e) {
            log.error("failed to find address information by name:{}  and level:{} , cause:{}", addressName,level,Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.address.find.fail");
        }
    }

    @Override
    public Response<Optional<WarehouseAddress>> findByName(String addressName) {

        try {
            WarehouseAddress warehouseAddress = warehouseAddressDao.findByName(addressName);

            return Response.ok(Optional.fromNullable(warehouseAddress));
        } catch (Exception e) {
            log.error("failed to find warehouse addresses information by name({}), cause:{}",addressName, Throwables.getStackTraceAsString(e));
            return Response.fail("warehouse.address.find.fail");
        }
    }
}
