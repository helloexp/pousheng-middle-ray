package com.pousheng.middle.warehouse.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.impl.dao.OpenShopWarehouseExtDao;
import com.pousheng.middle.warehouse.service.MiddleRefundWarehouseReadService;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.token.impl.dao.OpenShopDao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/10/24
 * pousheng-middle
 */
@Slf4j
@Service
public class MiddleRefundWarehouseReadServiceImpl implements MiddleRefundWarehouseReadService
{
    @Autowired
    private OpenShopDao openShopDao;
    @Autowired
    private OpenShopWarehouseExtDao openShopWarehouseExtDao;

    @Override
    public Response<Paging<OpenShop>> pagination(Integer pageNo, Integer pageSize, Map<String, Object> params) {
        try{
            PageInfo pageInfo = new PageInfo(pageNo, pageSize);
            Paging<OpenShop> p = openShopDao.paging(pageInfo.getOffset(), pageInfo.getLimit(), params);
            return Response.ok(p);
        }catch (Exception e){
            log.error("failed to pagination openShop with params:{}, cause:{}",
                    params, Throwables.getStackTraceAsString(e));
            return Response.fail("openShop.find.fail");
        }
    }


    @Override
    public Response<Paging<OpenShop>> paginationExt(Integer pageNo, Integer pageSize, Map<String, Object> params) {
        try{
            PageInfo pageInfo = new PageInfo(pageNo, pageSize);
            Paging<OpenShop> p = openShopWarehouseExtDao.pagingWithConditions(pageInfo.getOffset(), pageInfo.getLimit(), params);
            return Response.ok(p);
        }catch (Exception e){
            log.error("failed to pagination openShop with params:{}, cause:{}",
                    params, Throwables.getStackTraceAsString(e));
            return Response.fail("openShop.find.fail");
        }
    }
}
