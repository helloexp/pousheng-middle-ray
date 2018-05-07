package com.pousheng.middle.order.impl.service;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.pousheng.middle.order.enums.AddressBusinessType;
import com.pousheng.middle.order.impl.dao.AddressGpsDao;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.order.service.AddressGpsReadService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author: songrenfei
 * Desc: 地址定位信息表读服务实现类
 * Date: 2017-12-15
 */
@Slf4j
@Service
public class AddressGpsReadServiceImpl implements AddressGpsReadService {

    private final AddressGpsDao addressGpsDao;

    @Autowired
    public AddressGpsReadServiceImpl(AddressGpsDao addressGpsDao) {
        this.addressGpsDao = addressGpsDao;
    }

    @Override
    public Response<AddressGps> findById(Long Id) {
        try {
            return Response.ok(addressGpsDao.findById(Id));
        } catch (Exception e) {
            log.error("find addressGps by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("address.gps.find.fail");
        }
    }

    @Override
    public Response<Optional<AddressGps>> findByBusinessIdAndType(Long businessId, AddressBusinessType type) {
        try {
            AddressGps addressGps = addressGpsDao.findByBusinessIdAndType(businessId, type);
            if (addressGps == null) {
                log.error("not find address gps by businessId:{} and business type:{}", businessId,type);
            }
            return Response.ok(Optional.fromNullable(addressGps));
        }catch (Exception e){
            log.error("find address gps by businessId:{} and business type:{} fail,cause:{}", businessId,type);
            return Response.fail("find.address.gps.fail");
        }
    }

    @Override
    public Response<List<AddressGps>> findByProvinceIdAndBusinessType(Long provinceId, AddressBusinessType businessType) {
        try {
            return Response.ok(addressGpsDao.findByProvinceIdAndBusinessType(provinceId,businessType));
        } catch (Exception e) {
            log.error("find addressGps by province id :{} and business type:{} failed,  cause:{}", provinceId,businessType.getValue(), Throwables.getStackTraceAsString(e));
            return Response.fail("address.gps.find.fail");
        }
    }

    @Override
    public Response<List<AddressGps>> findByRegionIdAndBusinessType(Long regionId, AddressBusinessType businessType) {
        try {
            return Response.ok(addressGpsDao.findByRegionIdAndBusinessType(regionId,businessType));
        } catch (Exception e) {
            log.error("find addressGps by region id :{} and business type:{} failed,  cause:{}", regionId,businessType, Throwables.getStackTraceAsString(e));
            return Response.fail("address.gps.find.fail");
        }
    }
}
