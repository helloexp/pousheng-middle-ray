package com.pousheng.middle.web.item;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import io.terminus.parana.spu.service.SpuReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by songrenfei on 2017/6/30
 */
@RestController
@Slf4j
public class SkuTemplates {

    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;
    @RpcConsumer
    private SpuReadService spuReadService;

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

        Response<Spu> spuRes = spuReadService.findById(skuTemplate.getSpuId());
        if(!spuRes.isSuccess()){
            log.error("find spu by id:{} fail,error:{}",skuTemplate.getSpuId(),spuRes.getError());
            throw new JsonResponseException(spuRes.getError());
        }


        Response<List<SkuTemplate>> spuSkuTemplatesRes = skuTemplateReadService.findBySpuId(skuTemplate.getSpuId());
        if(!spuSkuTemplatesRes.isSuccess()){
            log.error("find sku template by spu id:{} fail,error:{}",skuTemplate.getSpuId());
            throw new JsonResponseException(spuSkuTemplatesRes.getError());
        }

        List<SkuTemplate> skuTemplatesList = spuSkuTemplatesRes.getResult();
        skuTemplatesList.forEach(skuTemplate1 -> skuTemplate1.setName(spuRes.getResult().getName()));
        return spuSkuTemplatesRes.getResult();
    }


    @RequestMapping(value="/api/sku-template/paging",method=RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<SkuTemplate> pagination(@RequestParam(value = "ids",required = false) List<Long> ids,@RequestParam(value ="skuCode", required = false) String skuCode,
                                          @RequestParam(value="name",  required = false) String name,
                                          @RequestParam(value = "spuId",required = false) Long spuId,
                                          @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                          @RequestParam(value = "pageSize", required = false) Integer pageSize){

        Map<String, Object> params = Maps.newHashMap();
        if (Objects.nonNull(ids)){
            params.put("ids",ids);
        }
        if(StringUtils.hasText(skuCode)){
            params.put("skuCode", skuCode);
        }
        if(StringUtils.hasText(name)){
            params.put("name", name);
        }
        if (spuId!=null){
            params.put("spuId",spuId);
        }
        params.put("statuses",Lists.newArrayList(1,-3));
        Response<Paging<SkuTemplate>> r = skuTemplateReadService.findBy(pageNo, pageSize, params);
        if(!r.isSuccess()){
            log.error("failed to pagination skuTemplates with params({}), error code:{}", params, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();

    }
}
