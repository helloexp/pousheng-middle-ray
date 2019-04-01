package com.pousheng.middle.group.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.group.impl.dao.ItemRuleGroupDao;
import com.pousheng.middle.group.model.ItemRuleGroup;
import com.pousheng.middle.group.service.ItemRuleGroupReadService;
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
public class ItemRuleGroupReadServiceImpl implements ItemRuleGroupReadService {

    @Autowired
    private ItemRuleGroupDao itemRuleGroupDao;

    @Override
    public Response<ItemRuleGroup> findById(Long Id) {
        try {
            return Response.ok(itemRuleGroupDao.findById(Id));
        } catch (Exception e) {
            log.error("find itemRuleGroup by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.group.find.fail");
        }
    }

    @Override
    public Response<List<ItemRuleGroup>> findByRuleId(Long ruleId) {
        try {
            return Response.ok(itemRuleGroupDao.findByRuleId(ruleId));
        } catch (Exception e) {
            log.error("find itemRuleGroup by ruleId :{} failed,  cause:{}", ruleId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.group.find.fail");
        }
    }

    @Override
    public Response<List<ItemRuleGroup>> findByGroupId(Long groupId) {
        try {
            return Response.ok(itemRuleGroupDao.findByGroupId(groupId));
        } catch (Exception e) {
            log.error("find itemRuleGroup by ruleId :{} failed,  cause:{}", groupId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.group.find.fail");
        }
    }
}
