package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.ZoneContractDao;
import com.pousheng.middle.order.model.ZoneContract;
import com.pousheng.middle.order.service.ZoneContractWriteService;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author: songrenfei
 * Desc: 区部联系人表写服务实现类
 * Date: 2018-04-04
 */
@Slf4j
@Service
public class ZoneContractWriteServiceImpl implements ZoneContractWriteService {

    private final ZoneContractDao zoneContractDao;

    @Autowired
    public ZoneContractWriteServiceImpl(ZoneContractDao zoneContractDao) {
        this.zoneContractDao = zoneContractDao;
    }

    @Override
    public Response<Long> create(ZoneContract zoneContract) {
        try {
            zoneContractDao.create(zoneContract);
            return Response.ok(zoneContract.getId());
        } catch (Exception e) {
            log.error("create zoneContract failed, zoneContract:{}, cause:{}", zoneContract, Throwables.getStackTraceAsString(e));
            return Response.fail("zoneName.contract.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(ZoneContract zoneContract) {
        try {
            return Response.ok(zoneContractDao.update(zoneContract));
        } catch (Exception e) {
            log.error("update zoneContract failed, zoneContract:{}, cause:{}", zoneContract, Throwables.getStackTraceAsString(e));
            return Response.fail("zoneName.contract.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long zoneContractId) {
        try {
            return Response.ok(zoneContractDao.delete(zoneContractId));
        } catch (Exception e) {
            log.error("delete zoneContract failed, zoneContractId:{}, cause:{}", zoneContractId, Throwables.getStackTraceAsString(e));
            return Response.fail("zoneName.contract.delete.fail");
        }
    }
}
