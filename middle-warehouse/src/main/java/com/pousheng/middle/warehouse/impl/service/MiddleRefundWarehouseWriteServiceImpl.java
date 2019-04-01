package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.service.MiddleRefundWarehouseWriteService;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.token.impl.dao.OpenShopDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/10/24
 * pousheng-middle
 */
@Slf4j
@Service
public class MiddleRefundWarehouseWriteServiceImpl implements MiddleRefundWarehouseWriteService {
    @Autowired
    private OpenShopDao openShopDao;

    @Override
    public Response<Boolean> update(OpenShop openShop) {
        try {
            return Response.ok(openShopDao.update(openShop));
        } catch (Exception e) {
            log.error("update openShop failed, openShop:{}, cause:{}", openShop, Throwables.getStackTraceAsString(e));
            return Response.fail("openShop.update.fail");
        }
    }
}
