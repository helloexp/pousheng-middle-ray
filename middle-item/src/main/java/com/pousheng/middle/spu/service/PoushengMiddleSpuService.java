package com.pousheng.middle.spu.service;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
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
    private SpuDao spuDao;

    @Autowired
    private SkuTemplateDao skuTemplateDao;

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
            Paging<Spu> r = spuDao.paging(pi.getOffset(), pi.getLimit(), params);
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
}
