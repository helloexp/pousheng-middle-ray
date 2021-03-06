package com.pousheng.middle.web.spu;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.erp.dto.MiddleSkuInfo;
import com.pousheng.erp.model.SpuMaterial;
import com.pousheng.erp.service.PoushengMiddleSpuService;
import com.pousheng.erp.service.SpuMaterialReadService;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import io.terminus.parana.spu.service.SpuReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-06
 */
@RestController
@Slf4j
public class PoushengMiddleSpus {

    @Autowired
    private PoushengMiddleSpuService poushengMiddleSpuService;
    @RpcConsumer
    private SpuReadService spuReadService;
    @Autowired
    private SpuMaterialReadService spuMaterialReadService;
    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;


    @RequestMapping(value = "/api/pousheng-spus/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<Spu> findBy(@RequestParam(required = false) String name,
                              @RequestParam(required = false) Long brandId,
                              @RequestParam(required = false) Long id,
                              @RequestParam(required = false) String specification,
                              @RequestParam(required = false) String materialCode,
                              @RequestParam(required = false) Integer type,
                              @RequestParam(required = false) Integer pageNo,
                              @RequestParam(required = false) Integer pageSize){
        if(log.isDebugEnabled()){
            log.debug("API-POUSHENG-SPUS-PAGING-START param: name [{}] brandId [{}] id [{}] specification [{}] materialCode [{}] type [{}] pageNo [{}] pageSize [{}]",
                    name,brandId,id,specification,materialCode,type,pageNo,pageSize);
        }
        Map<String, Object> params = Maps.newHashMap();
        if(StringUtils.hasText(name)){
            params.put("name", name.trim());
        }
        if(brandId!=null){
            params.put("brandId", brandId);
        }
        if(type!=null){
            params.put("type", type);
        }
        //货号
        if (StringUtils.hasText(materialCode)){
            Response<Optional<SpuMaterial>> spuMaterialResponse = spuMaterialReadService.findbyMaterialCode(materialCode);
            if (!spuMaterialResponse.isSuccess()){
                log.error("failed to find spuMaterial by materialCode={}, error code:{}", materialCode, spuMaterialResponse.getError());
            }
            Optional<SpuMaterial> spuMaterialOptional = spuMaterialResponse.getResult();
            if(spuMaterialOptional.isPresent()){
                SpuMaterial spuMaterial = spuMaterialOptional.get();
                params.put("id",spuMaterial.getSpuId());
            }else{
                return Paging.empty();
            }
        }
        //型号
        if (StringUtils.hasText(specification)){
            params.put("specification",specification);
        }
        if(Arguments.notNull(id)){
            return pagingById(id);
        }
        try {
            Response<Paging<Spu>> r = poushengMiddleSpuService.findBy(pageNo, pageSize, params);
            if(!r.isSuccess()){
                log.error("failed to find spus by {}, error code:{}", params, r.getError());
                throw new JsonResponseException(r.getError());
            }
            if(log.isDebugEnabled()){
                log.debug("API-POUSHENG-SPUS-PAGING-START param: name [{}] brandId [{}] id [{}] specification [{}] materialCode [{}] type [{}] pageNo [{}] pageSize [{}] ,resp: [{}]",
                        name,brandId,id,specification,materialCode,type,pageNo,pageSize,JsonMapper.nonEmptyMapper().toJson(r.getResult()));
            }
            return r.getResult();
        } catch (Exception e) {
            log.error("failed to find spus by {}, cause:{}", params, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException(500, "spu.find.fail");
        }
    }


    private Paging<Spu> pagingById(Long spuId){
        Response<Spu> spuResp = spuReadService.findById(spuId);
        if(!spuResp.isSuccess()){
            if(Objects.equal("spu.not.found",spuResp.getError())){
                return Paging.empty();
            }
            log.error("find spu by id:{} fail,error:{}",spuId,spuResp.getError());
            throw new JsonResponseException(spuResp.getError());
        }
        Paging<Spu> paging = Paging.empty();
        List<Spu> spus = Lists.newArrayList(spuResp.getResult());
        paging.setTotal(1L);
        paging.setData(spus);
        return paging;
    }

    @ApiOperation("根据skuCode查询mpos商品信息")
    @RequestMapping(value = "/api/mpos/sku-code", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public MiddleSkuInfo findBySkuCode(@RequestParam String skuCode) {
        Response<MiddleSkuInfo> response = poushengMiddleSpuService.findBySku(skuCode);
        if (!response.isSuccess()) {
            log.error("find spu fail,skuCode:{}",skuCode);
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    @ApiOperation("根据模版查询mpos商品信息")
    @RequestMapping(value = "/api/mpos/sku-templateId", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public MiddleSkuInfo findBySkuCode(@RequestParam Long templateId) {
        Response<MiddleSkuInfo> response = poushengMiddleSpuService.findBySkuTemplatesId(templateId);
        if (!response.isSuccess()) {
            log.error("find spu fail,templateId:{}",templateId);
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    @ApiOperation("根据模版list查询mpos商品集合信息")
    @RequestMapping(value = "/api/mpos/spu-templateIdList", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SkuTemplate> findBySkuTemplateCodes(@RequestParam List<String> skuTemplateCodes) {
        Response<List<SkuTemplate>> skuTempRes = skuTemplateReadService.findBySkuCodes(skuTemplateCodes);
        if (!skuTempRes.isSuccess()) {
            log.error("find spu fail,skuTemplateCodes:{}",skuTemplateCodes.toArray());
            throw new JsonResponseException(skuTempRes.getError());
        }
        return skuTempRes.getResult();
    }

}
