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

    @Transactional
    public Response<Long> create(ItemGroupSku itemGroupSku) {
        try {
            ItemGroupSku info = itemGroupSkuDao.findByGroupIdAndSkuCode(itemGroupSku.getGroupId(), itemGroupSku.getSkuCode());
            if (info != null) {
                return Response.fail("item.group.sku.is.exist");
            }
            Boolean resp = itemGroupSkuDao.create(itemGroupSku);
            if (resp) {
                return Response.ok(itemGroupSku.getId());
            } else {
                return Response.fail("item.group.sku.create.fail");
            }
        } catch (Exception e) {
            log.error("failed to create item group sku data:{} cause:{}", itemGroupSku, Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.sku.create.fail");
        }
    }

    public Response<Integer> batchCreate(List<String> skuCodes, Long groupId, Integer type) {
        try {
            List<ItemGroupSku> list = Lists.newArrayListWithCapacity(skuCodes.size());
            for (String skuCode : skuCodes) {
                list.add(new ItemGroupSku().groupId(groupId).skuCode(skuCode).type(type));
            }
            Integer resp = itemGroupSkuDao.creates(list);
            return Response.ok(resp);
        } catch (Exception e) {
            log.error("create itemGroupSku failed, skuCodes:{}, groupId:{} type:{} cause:{}", skuCodes, groupId, type, Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.sku.create.fail");
        }
    }


    public Response<Integer> batchDelete(List<String> skuCodes, Long groupId, Integer type) {
        try {
            Integer resp = itemGroupSkuDao.batchDelete(skuCodes, groupId, type);
            return Response.ok(resp);
        } catch (Exception e) {
            log.error("delete itemGroupSku failed, skuCodes:{} groupId:{} type:{}, cause:{}", skuCodes, groupId, type, Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.sku.delete.fail");
        }
    }

    @Transactional
    public Response<Boolean> deleteByGroupIdAndSkuId(Long groupId, String skuCode) {
        try {
            Boolean resp = itemGroupSkuDao.deleteByGroupIdAndSkuCode(groupId, skuCode);
            if (resp) {
                return Response.ok(true);
            } else {
                return Response.fail("item.group.sku.delete.fail");
            }
        } catch (Exception e) {
            log.error("failed to delete item group sku groupId:{}  id:{} cause:{}", groupId, skuCode, Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.sku.delete.fail");
        }
    }
}
