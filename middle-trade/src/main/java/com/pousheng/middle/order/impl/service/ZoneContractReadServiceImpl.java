package com.pousheng.middle.order.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.ZoneContractDao;
import com.pousheng.middle.order.model.ZoneContract;
import com.pousheng.middle.order.service.ZoneContractReadService;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.pousheng.middle.order.constant.TradeConstants.STATUS_ENABLE;

/**
 * Author: songrenfei
 * Desc: 区部联系人表读服务实现类
 * Date: 2018-04-04
 */
@Slf4j
@Service
public class ZoneContractReadServiceImpl implements ZoneContractReadService {

    private final ZoneContractDao zoneContractDao;

    @Autowired
    public ZoneContractReadServiceImpl(ZoneContractDao zoneContractDao) {
        this.zoneContractDao = zoneContractDao;
    }

    @Override
    public Response<ZoneContract> findById(Long Id) {
        try {
            return Response.ok(zoneContractDao.findById(Id));
        } catch (Exception e) {
            log.error("find zoneContract by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("zoneName.contract.find.fail");
        }
    }

    @Override
    public Response<Paging<ZoneContract>> pagination(String zoneName, Integer pageNo, Integer pageSize) {


        try {

            PageInfo pageInfo = new PageInfo(pageNo, pageSize);
            ZoneContract criteria = new ZoneContract();
            criteria.setStatus(STATUS_ENABLE);
            criteria.setZoneName(zoneName);

            return Response.ok(zoneContractDao.paging(pageInfo.getOffset(), pageInfo.getLimit(), criteria));

        } catch (Exception e) {


            log.error("paging zonecontract exception,zoneName={},pageNo={},pageSize={},cause={}", zoneName, pageNo, pageSize, Throwables.getStackTraceAsString(e));
            return Response.fail("zoneName.contract.find.fail");

        }
    }

    @Override
    public Response<List<ZoneContract>> findByZoneId(String zoneId) {


        try {

            return Response.ok(zoneContractDao.findByZoneId(zoneId));

        } catch (Exception e) {

            log.error(" zonecontacts findByZoneId error,cause={}", Throwables.getStackTraceAsString(e));
            return Response.fail("zoneName.contract.find.fail");

        }
    }
}
