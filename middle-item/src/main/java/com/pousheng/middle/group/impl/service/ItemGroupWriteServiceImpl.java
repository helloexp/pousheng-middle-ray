package com.pousheng.middle.group.impl.service;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.group.impl.dao.ItemGroupDao;
import com.pousheng.middle.group.model.ItemGroup;
import com.pousheng.middle.group.service.ItemGroupWriteService;
import com.pousheng.middle.item.dto.ItemGroupAutoRule;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zhaoxw
 * @date 2018/4/26
 */

@Service
@Slf4j
@RpcProvider
public class ItemGroupWriteServiceImpl implements ItemGroupWriteService {

    @Autowired
    private ItemGroupDao itemGroupDao;

    @Override
    public Response<Long> create(ItemGroup itemGroup) {
        try {
            Boolean resp = itemGroupDao.create(itemGroup);
            if (resp) {
                return Response.ok(itemGroup.getId());
            } else {
                return Response.fail("item.group.create.fail");
            }
        } catch (Exception e) {
            log.error("failed to create item group data:{} cause:{}", itemGroup, Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(ItemGroup itemGroup) {
        try {
            Boolean resp = itemGroupDao.update(itemGroup);
            if (resp) {
                return Response.ok(true);
            } else {
                return Response.fail("item.group.update.fail");
            }
        } catch (Exception e) {
            log.error("failed to update item group data:{} cause:{}", itemGroup, Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.update.fail");
        }
    }


    @Override
    public Response<Boolean> updateAutoRule(Long groupId, ItemGroupAutoRule rule) {
        try {
            ItemGroup index = itemGroupDao.findById(groupId);
            if(index==null){
                return Response.fail("item.group.not.exist");
            }
            if(index.getGroupRule()==null){
                index.setGroupRule(Lists.newArrayList());
            }
            Map<String, ItemGroupAutoRule> map = index.getGroupRule().stream()
                    .collect(Collectors.toMap(ItemGroupAutoRule::getName, e -> e));
            if(StringUtils.isEmpty(rule.getValue())){
                map.remove(rule.getName());
            }else{
                map.put(rule.getName(),rule);
            }
            index.setGroupRule(new ArrayList<>(map.values()));
            index.setGroupRule(index.getGroupRule());
            return Response.ok(itemGroupDao.update(index));
        } catch (Exception e) {
            log.error("failed to update item group id:{} , data:{}, cause:{}",groupId, rule, Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.update.fail");
        }
    }

    @Override
    public Response<Boolean> delete(Long id) {
        try {
            return Response.ok(itemGroupDao.delete(id));
        } catch (Exception e) {
            log.error("failed to delete item group id:{} cause:{}", id, Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.delete.fail");
        }
    }
}
