package com.pousheng.middle.group.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.group.dto.ItemGroupSkuCriteria;
import com.pousheng.middle.group.impl.dao.ItemGroupSkuDao;
import com.pousheng.middle.group.model.ItemGroupSku;
import com.pousheng.middle.group.service.ItemGroupSkuReadService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RpcProvider
public class ItemGroupSkuReadServiceImpl implements ItemGroupSkuReadService {

    @Autowired
    private ItemGroupSkuDao itemGroupSkuDao;

    @Override
    public Response<List<ItemGroupSku>> findByGroupId(Long groupId) {
        try {
            return Response.ok(itemGroupSkuDao.findByGroupId(groupId));
        }catch (Exception e){
            log.error("find ItemGroupSku fail ,cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("itemGroupSku.find.fail");
        }
    }

    @Override
    public Response<List<ItemGroupSku>> findByGroupIdAndType(Long groupId, Integer type) {
        try {
            return Response.ok(itemGroupSkuDao.findByGroupIdAndType(groupId,type));
        }catch (Exception e){
            log.error("find ItemGroupSku fail ,cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("itemGroupSku.find.fail");
        }
    }

    @Override
    public Response<Paging<ItemGroupSku>> findByCriteria(ItemGroupSkuCriteria criteria) {
        try {
            Paging<ItemGroupSku> paging = itemGroupSkuDao.paging(criteria.getOffset(),criteria.getLimit(),criteria.toMap());
            return Response.ok(paging);
        }catch (Exception e){
            log.error("failed to paging item group sku, criteria={}, cause:{}", criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("item.group.sku.find.fail");
        }
    }

    @Override
    public Response<Long> count(Long groupId, Integer type) {
        try {
            return Response.ok(itemGroupSkuDao.countGroupSkuAndType(groupId,type));
        }catch (Exception e){
            log.error("ItemGroupSku count fail ,cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("itemGroupSku.count.fail");
        }
    }

    @Override
    public Response<List<Long>> findGroupIdsBySkuCodeAndType(List<String> skuCodes, Integer type) {
        try {
            return Response.ok(itemGroupSkuDao.findGroupIdsBySkuCodeAndType(skuCodes,type));
        } catch (Exception e){
            log.error("find ItemGroupSku fail ,cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("itemGroupSku.find.fail"); 
        }
    }
}
