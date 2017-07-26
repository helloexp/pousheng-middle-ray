package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.WarehouseAddressDao;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import com.pousheng.middle.warehouse.service.WarehouseAddressReadService;
import io.terminus.common.exception.ServiceException;
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
                return Response.fail("rule.address.find.fail");
            }
            return Response.ok(warehouseAddress);
        } catch (ServiceException e1) {
            log.error("failed to find address information, error code:{}", e1.getMessage());
            return Response.fail(e1.getMessage());
        } catch (Exception e) {
            log.error("failed to find rule addresses information for ruleId({}), cause:{}"
                    , Throwables.getStackTraceAsString(e));
            return Response.fail("rule.address.find.fail");
        }
    }
}
