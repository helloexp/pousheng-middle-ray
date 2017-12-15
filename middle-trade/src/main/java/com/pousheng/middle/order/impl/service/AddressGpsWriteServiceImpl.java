package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.AddressGpsDao;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.order.service.AddressGpsWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: songrenfei
 * Desc: 地址定位信息表写服务实现类
 * Date: 2017-12-15
 */
@Slf4j
@Service
public class AddressGpsWriteServiceImpl implements AddressGpsWriteService {

    private final AddressGpsDao addressGpsDao;

    @Autowired
    public AddressGpsWriteServiceImpl(AddressGpsDao addressGpsDao) {
        this.addressGpsDao = addressGpsDao;
    }

    @Override
    public Response<Long> create(AddressGps addressGps) {
        try {
            addressGpsDao.create(addressGps);
            return Response.ok(addressGps.getId());
        } catch (Exception e) {
            log.error("create addressGps failed, addressGps:{}, cause:{}", addressGps, Throwables.getStackTraceAsString(e));
            return Response.fail("address.gps.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(AddressGps addressGps) {
        try {
            return Response.ok(addressGpsDao.update(addressGps));
        } catch (Exception e) {
            log.error("update addressGps failed, addressGps:{}, cause:{}", addressGps, Throwables.getStackTraceAsString(e));
            return Response.fail("address.gps.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long addressGpsId) {
        try {
            return Response.ok(addressGpsDao.delete(addressGpsId));
        } catch (Exception e) {
            log.error("delete addressGps failed, addressGpsId:{}, cause:{}", addressGpsId, Throwables.getStackTraceAsString(e));
            return Response.fail("address.gps.delete.fail");
        }
    }
}
