package com.pousheng.middle.group.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.group.dto.ItemRuleCriteria;
import com.pousheng.middle.group.impl.dao.ItemRuleDao;
import com.pousheng.middle.group.model.ItemRule;
import com.pousheng.middle.group.service.ItemRuleReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Paging;
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
public class ItemRuleReadServiceImpl implements ItemRuleReadService {

    @Autowired
    private ItemRuleDao itemRuleDao;

    @Override
    public Response<ItemRule> findById(Long Id) {
        try {
            return Response.ok(itemRuleDao.findById(Id));
        } catch (Exception e) {
            log.error("find itemRule by id :{} failed,  cause:{}", Id, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.find.fail");
        }
    }

    @Override
    public Response<Paging<ItemRule>> paging(ItemRuleCriteria criteria) {
        try {
            return Response.ok(itemRuleDao.paging(criteria.getOffset(),criteria.getLimit(),criteria.toMap()));
        } catch (Exception e) {
            log.error("find itemRule by criteria :{} failed,  cause:{}", criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.find.fail");
        }
    }

    @Override
    public Response<Paging<ItemRule>> pagingIds(ItemRuleCriteria criteria) {
        try {
            return Response.ok(itemRuleDao.pagingIds(criteria.getOffset(),criteria.getLimit(),criteria.toMap()));
        } catch (Exception e) {
            log.error("find itemRule by criteria :{} failed,  cause:{}", criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("item.rule.find.fail");
        }
    }
}
