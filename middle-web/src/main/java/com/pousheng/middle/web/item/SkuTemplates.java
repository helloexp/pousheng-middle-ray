package com.pousheng.middle.web.item;

import com.google.common.collect.Lists;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by songrenfei on 2017/6/30
 */
@RestController
@Slf4j
public class SkuTemplates {

    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;

    /**
     * 获取当前sku code对应sku的spu下的全部sku模板
     * @param skuCode 商品编码
     * @return sku模板
     */
    @RequestMapping(value="/api/sku-templates-spu",method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SkuTemplate> findSkuTemplates(@RequestParam(name = "skuCode") String skuCode) {
        Response<List<SkuTemplate>> skuTemplateRes = skuTemplateReadService.findBySkuCodes(Lists.newArrayList(skuCode));
        if(!skuTemplateRes.isSuccess()){
            log.error("find sku template by sku code:{} fail,error:{}",skuCode,skuTemplateRes.getError());
            throw new JsonResponseException(skuTemplateRes.getError());
        }
        List<SkuTemplate> skuTemplates = skuTemplateRes.getResult();
        if(CollectionUtils.isEmpty(skuTemplates)){
            log.error("not find sku template by sku code:{}",skuCode);
            throw new JsonResponseException("sku.template.not.exist");
        }

        SkuTemplate skuTemplate = skuTemplates.get(0);

        Response<List<SkuTemplate>> spuSkuTemplatesRes = skuTemplateReadService.findBySpuId(skuTemplate.getSpuId());
        if(!spuSkuTemplatesRes.isSuccess()){
            log.error("find sku template by spu id:{} fail,error:{}",skuTemplate.getSpuId());
            throw new JsonResponseException(spuSkuTemplatesRes.getError());
        }
        return spuSkuTemplatesRes.getResult();
    }
}
