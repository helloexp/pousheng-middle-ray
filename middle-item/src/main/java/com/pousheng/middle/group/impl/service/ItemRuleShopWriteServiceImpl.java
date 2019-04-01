package com.pousheng.middle.group.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.group.impl.dao.ItemRuleShopDao;
import com.pousheng.middle.group.model.ItemRuleShop;
import com.pousheng.middle.group.service.ItemRuleShopWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author zhaoxw
 * @date 2018/5/8
 */
@Slf4j
@Service
@RpcProvider
public class ItemRuleShopWriteServiceImpl implements ItemRuleShopWriteService {

    @Autowired
    private ItemRuleShopDao itemRuleShopDao;

    @Override
    public Response<Long> create(ItemRuleShop itemRuleShop) {
        try {
            itemRuleShopDao.create(itemRuleShop);
            return Response.ok(itemRuleShop.getId());
        } catch (Exception e) {
            log.error("create itemRuleShop failed, itemRuleShop:{}, cause:{}", itemRuleShop, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.shop.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(ItemRuleShop itemRuleShop) {
        try {
            return Response.ok(itemRuleShopDao.update(itemRuleShop));
        } catch (Exception e) {
            log.error("update itemRuleShop failed, itemRuleShop:{}, cause:{}", itemRuleShop, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.shop.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long itemRuleShopId) {
        try {
            return Response.ok(itemRuleShopDao.delete(itemRuleShopId));
        } catch (Exception e) {
            log.error("delete itemRuleShop failed, itemRuleShopId:{}, cause:{}", itemRuleShopId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.shop.delete.fail");
        }
    }
}
