package com.pousheng.middle.item.impl.service;

import com.google.common.base.Throwables;
import com.pousheng.middle.item.SearchSkuTemplateProperties;
import com.pousheng.middle.item.dto.IndexedSkuTemplate;
import com.pousheng.middle.item.impl.dao.SkuTemplateExtDao;
import com.pousheng.middle.item.service.IndexedSkuTemplateFactory;
import com.pousheng.middle.item.service.IndexedSkuTemplateGuarder;
import com.pousheng.middle.item.service.SkuTemplateSearchWriteService;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.spu.impl.dao.SkuTemplateDao;
import io.terminus.parana.spu.impl.dao.SpuAttributeDao;
import io.terminus.parana.spu.impl.dao.SpuDao;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.model.SpuAttribute;
import io.terminus.search.api.IndexExecutor;
import io.terminus.search.api.model.IndexTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Created by songrenfei on 2017/12/7
 */
@Service
@Slf4j
@RpcProvider
public class SkuTemplateSearchWriteServiceImpl implements SkuTemplateSearchWriteService {

    @Autowired
    private SpuDao spuDao;
    @Autowired
    private SkuTemplateDao skuTemplateDao;
    @Autowired
    private SkuTemplateExtDao skuTemplateExtDao;
    @Autowired
    private SpuAttributeDao spuAttributeDao;
    @Autowired
    private IndexExecutor indexExecutor;
    @Autowired
    private IndexedSkuTemplateFactory indexedItemFactory;
    @Autowired
    private SkuTemplateIndexAction indexedItemIndexAction;
    @Autowired
    private SearchSkuTemplateProperties searchSkuTemplateProperties;
    @Autowired
    private IndexedSkuTemplateGuarder indexedSkuTemplateGuarder;

    @Override
    public Response<Boolean> index(Long skuTemplateId) {

        try {
            SkuTemplate exist = skuTemplateDao.findById(skuTemplateId);
            if (Arguments.isNull(exist)) {
                log.error("not find sku template by id:{}", skuTemplateId);
                return Response.fail("sku.template.not.exist");
            }
            SpuAttribute spuAttribute = spuAttributeDao.findBySpuId(exist.getSpuId());
            Spu spu = spuDao.findById(exist.getSpuId());
            //更新
            if (indexedSkuTemplateGuarder.indexable(exist)) {

                Map<String, String> extra = exist.getExtra();
                //当没找到sku对应的货号时跳过
                if (Arguments.isNull(extra) || !extra.containsKey("materialId")) {
                    log.warn("sku template(id:{}) not find material id so skip create search index", exist.getId());
                    return Response.ok();
                }
                IndexedSkuTemplate indexedItem = indexedItemFactory.create(exist, spu, spuAttribute);
                IndexTask indexTask = indexedItemIndexAction.indexTask(indexedItem);
                indexExecutor.submit(indexTask);
            }

        } catch (Exception e) {
            log.error("create sku template(id:{}) search index fail,cause:{}", skuTemplateId, Throwables.getStackTraceAsString(e));
            return Response.fail("create.sku.template.search.index.fail");
        }

        return Response.ok(Boolean.TRUE);
    }

    @Override
    public Response<Boolean> delete(Long skuTemplateId) {
        try {
            SkuTemplate exist = skuTemplateDao.findById(skuTemplateId);
            if (Arguments.isNull(exist)) {
                log.error("not find sku template by id:{}", skuTemplateId);
                return Response.fail("sku.template.not.exist");
            }
            IndexTask indexTask = indexedItemIndexAction.deleteTask(exist.getId());
            indexExecutor.submit(indexTask);
        } catch (Exception e) {
            log.error("delete sku template(id:{}) search index fail,cause:{}", skuTemplateId, Throwables.getStackTraceAsString(e));
            return Response.fail("delete.sku.template.search.index.fail");
        }

        return Response.ok(Boolean.TRUE);
    }

    @Override
    public Response<Boolean> update(Long skuTemplateId) {

        try {
            SkuTemplate exist = skuTemplateDao.findById(skuTemplateId);
            if (Arguments.isNull(exist)) {
                log.error("not find sku template by id:{}", skuTemplateId);
                return Response.fail("sku.template.not.exist");
            }
            SpuAttribute spuAttribute = spuAttributeDao.findBySpuId(exist.getSpuId());
            Spu spu = spuDao.findById(exist.getSpuId());
            //更新
            if (indexedSkuTemplateGuarder.indexable(exist)) {
                IndexedSkuTemplate indexedItem = indexedItemFactory.create(exist, spu, spuAttribute);
                IndexTask indexTask = indexedItemIndexAction.indexTask(indexedItem);
                indexExecutor.submit(indexTask);
            } else { //删除
                IndexTask indexTask = indexedItemIndexAction.deleteTask(exist.getId());
                indexExecutor.submit(indexTask);
            }

        } catch (Exception e) {
            log.error("update sku template(id:{}) search index fail,cause:{}", skuTemplateId, Throwables.getStackTraceAsString(e));
            return Response.fail("update.sku.template.search.index.fail");
        }

        return Response.ok(Boolean.TRUE);

    }

}
