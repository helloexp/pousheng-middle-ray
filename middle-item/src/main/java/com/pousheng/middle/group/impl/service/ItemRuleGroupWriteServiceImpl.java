package com.pousheng.middle.group.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.group.impl.dao.ItemRuleGroupDao;
import com.pousheng.middle.group.model.ItemRuleGroup;
import com.pousheng.middle.group.service.ItemRuleGroupWriteService;
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
public class ItemRuleGroupWriteServiceImpl implements ItemRuleGroupWriteService {

    @Autowired
    private ItemRuleGroupDao itemRuleGroupDao;


    @Override
    public Response<Long> create(ItemRuleGroup itemRuleGroup) {
        try {
            itemRuleGroupDao.create(itemRuleGroup);
            return Response.ok(itemRuleGroup.getId());
        } catch (Exception e) {
            log.error("create itemRuleGroup failed, itemRuleGroup:{}, cause:{}", itemRuleGroup, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.group.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(ItemRuleGroup itemRuleGroup) {
        try {
            return Response.ok(itemRuleGroupDao.update(itemRuleGroup));
        } catch (Exception e) {
            log.error("update itemRuleGroup failed, itemRuleGroup:{}, cause:{}", itemRuleGroup, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.group.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long itemRuleGroupId) {
        try {
            return Response.ok(itemRuleGroupDao.delete(itemRuleGroupId));
        } catch (Exception e) {
            log.error("delete itemRuleGroup failed, itemRuleGroupId:{}, cause:{}", itemRuleGroupId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.group.delete.fail");
        }
    }
}
