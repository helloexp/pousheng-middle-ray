package com.pousheng.middle.group.impl.manager;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.group.impl.dao.ItemGroupDao;
import com.pousheng.middle.group.impl.dao.ItemGroupSkuDao;
import com.pousheng.middle.group.model.ItemGroup;
import com.pousheng.middle.group.model.ItemGroupSku;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/5/9
 */
@Component
@Slf4j
public class ItemGroupSkuManager {

    @Autowired
    ItemGroupSkuDao itemGroupSkuDao;

    @Autowired
    ItemGroupDao itemGroupDao;

    @Transactional
    public Response<Long> create(ItemGroupSku itemGroupSku) {
        try {
            ItemGroupSku info = itemGroupSkuDao.findByGroupIdAndSkuId(itemGroupSku.getGroupId(), itemGroupSku.getSkuId());
            if (info != null) {
                return Response.fail("item.group.sku.is.exist");
            }
            Boolean resp = itemGroupSkuDao.create(itemGroupSku);
            if (resp) {
                updateGroupInfo(itemGroupSku.getGroupId());
                return Response.ok(itemGroupSku.getId());
            } else {
                return Response.fail("item.group.sku.create.fail");
            }
        } catch (Exception e) {
            log.error("failed to create item group sku data:{} cause:{}", itemGroupSku, Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.sku.create.fail");
        }
    }

    public Response<Integer> batchCreate(List<Long> skuIds, Long groupId, Integer type) {
        try {
            List<ItemGroupSku> list = Lists.newArrayListWithCapacity(skuIds.size());
            for (Long skuId : skuIds) {
                list.add(new ItemGroupSku().groupId(groupId).skuId(skuId).type(type));
            }
            Integer resp = itemGroupSkuDao.creates(list);
            updateGroupInfo(groupId);
            return Response.ok(resp);
        } catch (Exception e) {
            log.error("create itemGroupSku failed, skuIds:{}, groupId:{} type:{} cause:{}", skuIds, groupId, type, Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.sku.create.fail");
        }
    }


    public Response<Integer> batchDelete(List<Long> skuIds, Long groupId, Integer type) {
        try {
            Integer resp = itemGroupSkuDao.batchDelete(skuIds, groupId, type);
            updateGroupInfo(groupId);
            return Response.ok(resp);
        } catch (Exception e) {
            log.error("delete itemGroupSku failed, skuIds:{} groupId:{} type:{}, cause:{}", skuIds, groupId, type, Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.sku.delete.fail");
        }
    }

    @Transactional
    public Response<Boolean> deleteByGroupIdAndSkuId(Long groupId, Long skuId) {
        try {
            Boolean resp = itemGroupSkuDao.deleteByGroupIdAndSkuId(groupId, skuId);
            if (resp) {
                updateGroupInfo(groupId);
                return Response.ok(true);
            } else {
                return Response.fail("item.group.sku.delete.fail");
            }
        } catch (Exception e) {
            log.error("failed to delete item group sku groupId:{}  id:{} cause:{}", groupId, skuId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.sku.delete.fail");
        }
    }

    private void updateGroupInfo(Long groupId) {
        Long count = itemGroupSkuDao.countGroupSku(groupId);
        ItemGroup group = new ItemGroup().id(groupId).relatedNum(count);
        itemGroupDao.update(group);
    }
}
