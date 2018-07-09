package com.pousheng.middle.group.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.group.dto.ItemGroupCriteria;
import com.pousheng.middle.group.impl.dao.ItemGroupDao;
import com.pousheng.middle.group.model.ItemGroup;
import com.pousheng.middle.group.service.ItemGroupReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/4/26
 */

@Service
@Slf4j
@RpcProvider
public class ItemGroupReadServiceImpl implements ItemGroupReadService {

    @Autowired
    private ItemGroupDao itemGroupDao;

    @Override
    public Response<Paging<ItemGroup>> findByCriteria(ItemGroupCriteria criteria) {
        try {
            Paging<ItemGroup> paging = itemGroupDao.paging(criteria.getOffset(), criteria.getLimit(), criteria.toMap());
            return Response.ok(paging);
        } catch (Exception e) {
            log.error("failed to paging item group, criteria={}, cause:{}", criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.find.fail");
        }
    }

    @Override
    public Response<ItemGroup> findById(Long id) {
        try {
            ItemGroup itemGroup = itemGroupDao.findById(id);
            if (itemGroup == null) {
                log.error("item group id={} not found", id);
                return Response.fail("item.group.find.fail");
            } else {
                return Response.ok(itemGroup);
            }
        } catch (Exception e) {
            log.error("find item group by id {} fail,cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.find.fail");
        }
    }


    @Override
    public Response<List<ItemGroup>> findAutoGroups() {
        try {
            return Response.ok(itemGroupDao.findAutoGroups());
        } catch (Exception e) {
            log.error("find item group fail ,cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.find.fail");
        }
    }


    @Override
    public Response<List<ItemGroup>> findByIds(List<Long> ids) {
        try {
            return Response.ok(itemGroupDao.findByIds(ids));
        } catch (Exception e) {
            log.error("find item group fail {} ,cause:{}",ids,Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.find.fail");
        }
    }


    @Override
    public Response<Boolean> checkName(String name) {
        try {
            if (itemGroupDao.findByName(name) != null) {
                return Response.ok(false);
            }
            return Response.ok(true);
        } catch (Exception e) {
            log.error("failed to check name data:{} cause:{}",name , Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.check.fail");
        }
    }
}

