package com.pousheng.erp.service;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.pousheng.erp.dao.mysql.ErpSpuDao;
import com.pousheng.erp.dto.MiddleSkuInfo;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.spu.impl.dao.SkuTemplateDao;
import io.terminus.parana.spu.impl.dao.SpuDao;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by songrenfei on 2017/6/7
 */
@Slf4j
@Service
public class PoushengMiddleSpuService {

    @Autowired
    private ErpSpuDao erpSpuDao;

    @Autowired
    private SkuTemplateDao skuTemplateDao;

    @Autowired
    private SpuDao spuDao;

    /**
     * 分页查询spu列表
     *
     * @param pageNo   起始页码
     * @param pageSize 每页返回条数
     * @param params   参数
     * @return 分页结果
     */
    public Response<Paging<Spu>> findBy(Integer pageNo, Integer pageSize, Map<String, Object> params) {
        try {
            PageInfo pi = new PageInfo(pageNo, pageSize);
            Paging<Spu> r = erpSpuDao.erPpaging(pi.getOffset(), pi.getLimit(), params);
            return Response.ok(r);
        } catch (Exception e) {
            log.error("failed to find spus by {}, cause:{}", params, Throwables.getStackTraceAsString(e));
            return Response.fail("spu.find.fail");
        }
    }

    public Response<Optional<SkuTemplate>> findBySkuCode(String skuCode) {
        try {
            List<SkuTemplate> skuTemplates = skuTemplateDao.findBySkuCode(skuCode);
            if (CollectionUtils.isEmpty(skuTemplates)) {
                log.warn("sku template not found where skuCode={}", skuCode);
                return Response.ok(Optional.absent());
            }
            return Response.ok(Optional.of(skuTemplates.get(0)));
        } catch (Exception e) {
            log.error("fail to find sku template by skuCode={},cause:{}",
                    skuCode, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.template.find.fail");
        }
    }

    /**
     *
     * @param skuCode sku码
     * @return dto对象
     */
    public Response<MiddleSkuInfo> findBySku(String skuCode) {
        try {
            MiddleSkuInfo middleSkuInfo = new MiddleSkuInfo();
            List<SkuTemplate> skuTemplates = skuTemplateDao.findBySkuCode(skuCode);
            for (SkuTemplate skuTemplate : skuTemplates) {
                if (skuTemplate.getStatus() == 1) {
                    middleSkuInfo.setSkuTemplate(skuTemplate);
                    break;
                }
            }
            if (middleSkuInfo.getSkuTemplate() == null) {
                log.error("fail to find sku info by skuCode={},cause:{}", skuCode);
                return Response.fail("sku.template.find.fail");
            }
            Spu spu = spuDao.findById(middleSkuInfo.getSkuTemplate().getSpuId());
            middleSkuInfo.setSpu(spu);
            return Response.ok(middleSkuInfo);
        } catch (Exception e) {
            log.error("fail to find sku info by skuCode={},cause:{}",
                    skuCode, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.template.find.fail");
        }
    }

    /**
     *
     * @param templatesId templatesId主键
     * @return dto对象
     */
    public Response<MiddleSkuInfo> findBySkuTemplatesId(Long templatesId) {
        try {
            MiddleSkuInfo middleSkuInfo = new MiddleSkuInfo();
            SkuTemplate skuTemplate = skuTemplateDao.findById(templatesId);
            if (skuTemplate == null) {
                log.error("fail to find sku info by templatesId={},cause:{}", templatesId);
                return Response.fail("sku.template.find.fail");
            }
            Spu spu = spuDao.findById(skuTemplate.getSpuId());
            middleSkuInfo.setSkuTemplate(skuTemplate);
            middleSkuInfo.setSpu(spu);
            return Response.ok(middleSkuInfo);
        } catch (Exception e) {
            log.error("fail to find sku info by templatesId={},cause:{}",
                    templatesId, Throwables.getStackTraceAsString(e));
            return Response.fail("sku.template.find.fail");
        }
    }
}
