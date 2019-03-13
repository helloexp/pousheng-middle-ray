package com.pousheng.middle.group.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.group.impl.dao.ItemGroupSkuDao;
import com.pousheng.middle.group.impl.manager.ItemGroupSkuManager;
import com.pousheng.middle.group.model.ItemGroupSku;
import com.pousheng.middle.group.service.ItemGroupSkuWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author zhaoxw
 * @date 2018/4/27
 */

@Service
@Slf4j
@RpcProvider
public class ItemGroupSkuWriteServiceImpl implements ItemGroupSkuWriteService {

    @Autowired
    private ItemGroupSkuDao itemGroupSkuDao;

    @Autowired
    private ItemGroupSkuManager itemGroupSkuManager;

    @Override
    public Response<Long> create(ItemGroupSku itemGroupSku) {
        try {
            itemGroupSkuDao.create(itemGroupSku);
            return Response.ok(itemGroupSku.getId());
        } catch (Exception e) {
            log.error("create itemGroupSku failed, itemGroupSku:{}, cause:{}", itemGroupSku, Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.sku.create.fail");
        }
    }

    @Override
    public Response<Boolean> update(ItemGroupSku itemGroupSku) {
        try {
            return Response.ok(itemGroupSkuDao.update(itemGroupSku));
        } catch (Exception e) {
            log.error("update itemGroupSku failed, itemGroupSku:{}, cause:{}", itemGroupSku, Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.sku.update.fail");
        }
    }

    @Override
    public Response<Boolean> deleteById(Long itemGroupSkuId) {
        try {
            return Response.ok(itemGroupSkuDao.delete(itemGroupSkuId));
        } catch (Exception e) {
            log.error("delete itemGroupSku failed, itemGroupSkuId:{}, cause:{}", itemGroupSkuId, Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.sku.delete.fail");
        }
    }

    @Override
    public Response<Long> createItemGroupSku(ItemGroupSku itemGroupSku) {
        return itemGroupSkuManager.create(itemGroupSku);
    }

    @Override
    public Response<Integer> batchCreate(List<String> skuCodes, Long groupId, Integer type, Integer mark) {
        return itemGroupSkuManager.batchCreate(skuCodes, groupId, type, mark);
    }

    @Override
    public Response<Integer> batchDelete(List<String> skuCodes, Long groupId, Integer type, Integer mark) {
        return itemGroupSkuManager.batchDelete(skuCodes, groupId, type, mark);
    }

    @Override
    public Response<Boolean> deleteByGroupIdAndSkuCode(Long groupId, String skuCode) {
        return itemGroupSkuManager.deleteByGroupIdAndSkuId(groupId, skuCode);
    }

    @Override
    public Response<Integer> batchDeleteByids(List<Long> ids) {
        return itemGroupSkuManager.batchDeleteByIds(ids);
    }


}
