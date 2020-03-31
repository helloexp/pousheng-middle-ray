package com.pousheng.middle.group.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.group.impl.dao.ItemRuleShopDao;
import com.pousheng.middle.group.model.ItemRuleShop;
import com.pousheng.middle.group.service.ItemRuleShopReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/5/8
 */

@Slf4j
@Service
@RpcProvider
public class ItemRuleShopReadServiceImpl implements ItemRuleShopReadService {

    @Autowired
    private ItemRuleShopDao itemRuleShopDao;

    @Override
    public Response<ItemRuleShop> findById(Long Id) {
        try {
            return Response.ok(itemRuleShopDao.findById(Id));
        } catch (Exception e) {
            log.error("find itemRuleShop by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.shop.find.fail");
        }
    }

    @Override
    public Response<List<ItemRuleShop>> findByRuleId(Long ruleId) {
        try {
            return Response.ok(itemRuleShopDao.findByRuleId(ruleId));
        } catch (Exception e) {
            log.error("find itemRuleShop by rule id :{} failed,  cause:{}", ruleId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.shop.find.fail");
        }
    }

    @Override
    public Response<List<Long>> findShopIds() {
        try {
            return Response.ok(itemRuleShopDao.findShopIds());
        } catch (Exception e) {
            log.error("find shop id failed,  cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.shop.find.fail");
        }
    }

    @Override
    public Response<Boolean> checkShopIds(Long ruleId, List<Long> shopIds) {
        try {
            return Response.ok(itemRuleShopDao.checkShopIds(ruleId,shopIds));
        } catch (Exception e) {
            log.error("find shop id failed,  cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.shop.find.fail");
        }
    }

    @Override
    public Response<Long> findRuleIdByShopId(Long shopId) {
        try {
            return Response.ok(itemRuleShopDao.findRuleIdByShopId(shopId));
        } catch (Exception e) {
            log.error("find shop id failed,  cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.shop.find.fail");
        }
    }
}
