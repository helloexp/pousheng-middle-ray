/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.spu;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.pousheng.middle.web.utils.RichTextCleaner;
import io.swagger.annotations.ApiOperation;
import io.terminus.applog.annotation.LogMe;
import io.terminus.applog.annotation.LogMeContext;
import io.terminus.applog.annotation.LogMeId;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.component.dto.spu.EditSpu;
import io.terminus.parana.component.spu.component.SpuReader;
import io.terminus.parana.component.spu.component.SpuWriter;
import io.terminus.parana.spu.dto.FullSpu;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SpuReadService;
import io.terminus.parana.spu.service.SpuWriteService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 创建和更新spu相关的接口
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-03-03
 */
@RestController
@RequestMapping("/api/spu")
@Slf4j
public class Spus {

    @RpcConsumer
    private SpuReadService spuReadService;

    @RpcConsumer
    private SpuWriteService spuWriteService;

    @Autowired
    private SpuReader spuReader;

    @Autowired
    private SpuWriter spuWriter;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Spu findById(@RequestParam(name = "id") Long id){
        if(log.isDebugEnabled()){
            log.debug("API-SPU-FINDBYID-START param: id [{}]",id);
        }
        Response<Spu> rSpu = spuReadService.findById(id);
        if(!rSpu.isSuccess()){
            log.error("failed to find spu(id={}),error code:{}", id, rSpu.getError());
            throw new JsonResponseException(rSpu.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-SPU-FINDBYID-END param: id [{}] ,resp: [{}]",id,JsonMapper.nonEmptyMapper().toJson(rSpu.getResult()));
        }
        return rSpu.getResult();
    }


    @RequestMapping(value="/bycat",method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<Spu> findByCategoryId(@RequestParam(name = "categoryId",required = false) Long categoryId,
                                        @RequestParam(name="id",required = false)  Long spuId,
                                        @RequestParam(name="keyword",required = false)String keyword,
                                        @RequestParam(name="pageNo",defaultValue = "1")Integer pageNo,
                                        @RequestParam(name = "pageSize", defaultValue = "10")Integer pageSize){
        if(log.isDebugEnabled()){
            log.debug("API-SPU-BYCAT-START param: categoryId [{}] spuId [{}] keyword [{}] pageNo [{}] pageSize [{}]",categoryId,spuId,keyword,pageNo,pageSize);
        }
        Response<Paging<Spu>> rSpu = spuReadService.findByCategoryId(categoryId,keyword,pageNo, pageSize);
        if(!rSpu.isSuccess()){
            log.error("failed to find spu by category(id={}) spu(id:{}) keyword:{},error code:{}", categoryId, spuId,keyword,rSpu.getError());
            throw new JsonResponseException(rSpu.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-SPU-BYCAT-END param: categoryId [{}] spuId [{}] keyword [{}] pageNo [{}] pageSize [{}] ,resp: [{}]",categoryId,spuId,keyword,pageNo,pageSize,JsonMapper.nonEmptyMapper().toJson(rSpu.getResult()));
        }
        return rSpu.getResult();
    }

    @ApiOperation("创建")
    @LogMe(description = "创建Spu", compareTo = "spuDao#findById")
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long create(@RequestBody @LogMeContext  FullSpu fullSpu) {
        if(log.isDebugEnabled()){
            log.debug("API-SPU-CREATE-START param: fullSpu [{}]",JsonMapper.nonEmptyMapper().toJson(fullSpu));
        }
        Spu spu = fullSpu.getSpu();

        //完善商品的默认信息
        defaultSpuInfos(spu);

        //计算商品价格区间
        try {
            extractInfoFromSkus(spu, fullSpu.getSkuTemplates());
        } catch (Exception e) {
            log.error("bad sku info", e);
            throw new JsonResponseException("illegal.sku.info");
        }

        Response<Long> rSpuId = spuWriter.create(fullSpu);
        if (!rSpuId.isSuccess()) {
            log.error("failed to create {}, error code:{}", fullSpu, rSpuId.getError());
            throw new JsonResponseException(rSpuId.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-SPU-CREATE-END param: fullSpu [{}] ,resp: [{}]",JsonMapper.nonEmptyMapper().toJson(fullSpu),JsonMapper.nonEmptyMapper().toJson(rSpuId.getResult()));
        }
        return rSpuId.getResult();
    }

    private void defaultSpuInfos(Spu spu) {
        spu.setStatus(MoreObjects.firstNonNull(spu.getStatus(), 1)); //默认正常状态
        spu.setType(MoreObjects.firstNonNull(spu.getType(), 1));   //默认为普通spu
        spu.setReduceStockType(MoreObjects.firstNonNull(spu.getReduceStockType(), 1)); //默认拍下减库存
        spu.setStockType(MoreObjects.firstNonNull(spu.getStockType(), 0));//默认不分仓存储

    }

    private void extractInfoFromSkus(Spu spu, List<SkuTemplate> skuTemplates) {

        //计算Spu 库存
        if (Objects.equal(spu.getStockType(), 0)) {
            int stockQuantity = 0;
            for (SkuTemplate skuTemplate : skuTemplates) {
                if(skuTemplate.getStockQuantity()!=null) {
                    if(skuTemplate.getStockQuantity()<0){
                        throw new IllegalArgumentException("sku.stock.negative");
                    }
                    stockQuantity += skuTemplate.getStockQuantity();
                }
            }
            spu.setStockQuantity(stockQuantity);
        }

        int highPrice = -1;
        int lowPrice = -1;

        for (SkuTemplate skuTemplate : skuTemplates) {
            if (skuTemplate.getPrice() != null) {
                if (skuTemplate.getPrice() <= 0) {
                    throw new IllegalArgumentException("sku.price.need.positive");
                }
                if (skuTemplate.getPrice() > highPrice) {
                    highPrice = skuTemplate.getPrice();
                }
                if (skuTemplate.getPrice() < lowPrice || lowPrice < 0) {
                    lowPrice = skuTemplate.getPrice();
                }
            }
        }
        if (highPrice > 0) {
            spu.setHighPrice(highPrice);
        }
        if (lowPrice > 0) {
            spu.setLowPrice(lowPrice);
        }
    }

    @ApiOperation("更新")
    @LogMe(description = "更新SPU",ignore = true)
    @RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean update(@RequestBody @LogMeContext FullSpu fullSpu) {

        if(log.isDebugEnabled()){
            log.debug("API-SPU-UPDATE-START param: fullSpu [{}]",JsonMapper.nonEmptyMapper().toJson(fullSpu));
        }
        Spu spu = fullSpu.getSpu();

        final Long spuId = spu.getId();
        Response<Spu> rSpu = spuReadService.findById(spuId);
        if(!rSpu.isSuccess()){
            log.error("failed to find spu(id={}), error code:{}", spuId, rSpu.getError());
            throw new JsonResponseException(rSpu.getError());
        }

        spu.setStockType(rSpu.getResult().getStockType());
        //避免重复计算默认值啥的
        extractInfoFromSkus(spu, fullSpu.getSkuTemplates());
        List<SkuTemplate> skuTemplates = fullSpu.getSkuTemplates().stream().filter(it->!Objects.equal(it.getSkuCode(),"0")).collect(Collectors.toList());
        fullSpu.setSkuTemplates(skuTemplates);
        Response<Boolean> rUpdate = spuWriter.update(fullSpu);
        if (!rUpdate.isSuccess()) {
            log.error("failed to update {}, error code:{}", fullSpu, rUpdate.getError());
            throw new JsonResponseException(rUpdate.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-SPU-UPDATE-START param: fullSpu [{}] ,resp: [{}]",JsonMapper.nonEmptyMapper().toJson(fullSpu),rUpdate.getResult());
        }
        return rUpdate.getResult();
    }



    @ApiOperation("附加标签")
    @LogMe(description = "更新Spu附加标签", compareTo = "spuDao#findById")
    @RequestMapping(value ="/extras", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean updateExtras(@RequestParam(name = "id") @LogMeId Long id, @RequestParam("extras") @LogMeId String extras){
        if(log.isDebugEnabled()){
            log.debug("API-SPU-EXTRAS-START param: id [{}] extras [{}]",id,extras);
        }
        Map<String,String> realTags = Splitter.on(',').withKeyValueSeparator(':').split(extras);
        Response<Boolean>  r = spuWriteService.extras(id,realTags);
        if (!r.isSuccess()) {
            log.error("failed to update extras to {} for spu(id={}), error code:{} ",
                    extras, id, r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-SPU-EXTRAS-END param: id [{}] extras [{}]",id,extras);
        }
        return true;
    }


    @ApiOperation("删除")
    @LogMe(description = "删除Spu", deleting = "spuDao#findById")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean delete(@PathVariable("id") @LogMeContext  Long id){
        if(log.isDebugEnabled()){
            log.debug("API-SPU-DELETE-START param: id [{}]",id);
        }
        Response<Boolean>  r = spuWriteService.delete(id);
        if(!r.isSuccess()){
            log.error("failed to delete spu(id={}), error code:{} ",
                    id, r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-SPU-DELETE-END param: id [{}]",id);
        }
        return true;
    }

    @RequestMapping(value="/find-for-edit",method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public EditSpu findForEdit(@RequestParam(name = "spuId", required = false) Long id) {
        if(log.isDebugEnabled()){
            log.debug("API-SPU-FINDFOREDIT-START param: id [{}]",id);
        }
        if (id == null) {
            return new EditSpu();
        }
        val resp = spuReader.findForEdit(id);
        if (!resp.isSuccess()) {
            throw new JsonResponseException(resp.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-SPU-FINDFOREDIT-END param: id [{}] ,resp: [{}]",id,JsonMapper.nonEmptyMapper().toJson(resp.getResult()));
        }
        return resp.getResult();
    }

    @ApiOperation("根据spuId编辑富文本详情")
    @LogMe(description = "根据spuId编辑富文本详情", compareTo = "spuDetailDao#findById")
    @RequestMapping(value = "/{id}/detail", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean editMRichText(@PathVariable("id") @LogMeContext Long spuId, @RequestParam("detail") @LogMeContext String richText) {

        if(log.isDebugEnabled()){
            log.debug("API-SPU-EDITRICHTEXT-START param: spuId [{}] richText [{}]",spuId,richText);
        }
        Response<Spu> rSpu = spuReadService.findById(spuId);
        if (!rSpu.isSuccess()) {
            log.error("failed to find spu(id={}), error code:{}", spuId, rSpu.getError());
            throw new JsonResponseException(rSpu.getError());
        }

        String safeRichText = RichTextCleaner.safe(richText);

        Response<Boolean> r = spuWriteService.editRichText(spuId, safeRichText);

        if (!r.isSuccess()) {
            log.error("failed to edit richtext for spu(id={}), error code:{}", spuId, r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-SPU-EDITRICHTEXT-END param: spuId [{}] richText [{}]",spuId,richText);
        }
        return true;
    }

    @ApiOperation("根据spuId查询富文本详情")
    @RequestMapping(value ="/{id}/detail" ,method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String findRichTextById(@PathVariable("id") Long spuId) {
        if(log.isDebugEnabled()){
            log.debug("API-SPU-FINDRICHTEXTBYID-START param: spuId [{}]",spuId);
        }
        Response<String> r = spuReadService.findRichTextById(spuId);
        if (!r.isSuccess()) {
            log.error("failed to find rich text detail for spu(id={}), error code:{}",
                    spuId, r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-SPU-FINDRICHTEXTBYID-END param: spuId [{}] ,resp: [{}]",spuId,r.getResult());
        }
        return r.getResult();
    }

}
