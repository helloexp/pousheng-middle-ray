/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.spu;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
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
        Response<Spu> rSpu = spuReadService.findById(id);
        if(!rSpu.isSuccess()){
            log.error("failed to find spu(id={}),error code:{}", id, rSpu.getError());
            throw new JsonResponseException(rSpu.getError());
        }
        return rSpu.getResult();
    }


    @RequestMapping(value="/bycat",method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<Spu> findByCategoryId(@RequestParam(name = "categoryId",required = false) Long categoryId,
                                        @RequestParam(name="id",required = false)  Long spuId,
                                        @RequestParam(name="keyword",required = false)String keyword,
                                        @RequestParam(name="pageNo",defaultValue = "1")Integer pageNo,
                                        @RequestParam(name = "pageSize", defaultValue = "10")Integer pageSize){

        if(Arguments.notNull(spuId)){
            return pagingById(spuId);
        }
        Response<Paging<Spu>> rSpu = spuReadService.findByCategoryId(categoryId,keyword,pageNo, pageSize);
        if(!rSpu.isSuccess()){
            log.error("failed to find spu by category(id={}) spu(id:{}) keyword:{},error code:{}", categoryId, spuId,keyword,rSpu.getError());
            throw new JsonResponseException(rSpu.getError());
        }
        return rSpu.getResult();
    }

    private Paging<Spu> pagingById(Long spuId){
        Response<Spu> spuResp = spuReadService.findById(spuId);
        if(!spuResp.isSuccess()){
            log.error("find spu by id:{} fail,error:{}",spuId,spuResp.getError());
            throw new JsonResponseException(spuResp.getError());
        }
        Paging<Spu> paging = Paging.empty();
        List<Spu> spus = Lists.newArrayList(spuResp.getResult());
        paging.setTotal(1L);
        paging.setData(spus);
        return paging;
    }


    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long create(@RequestBody FullSpu fullSpu) {
        
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

    @RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean update(@RequestBody FullSpu fullSpu) {

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

        Response<Boolean> rUpdate = spuWriter.update(fullSpu);
        if (!rUpdate.isSuccess()) {
            log.error("failed to update {}, error code:{}", fullSpu, rUpdate.getError());
            throw new JsonResponseException(rUpdate.getError());
        }

        return rUpdate.getResult();
    }




    @RequestMapping(value ="/extras", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean updateExtras(@RequestParam(name="id")Long id, @RequestParam("extras")String extras){
        Map<String,String> realTags = Splitter.on(',').withKeyValueSeparator(':').split(extras);
        Response<Boolean>  r = spuWriteService.extras(id,realTags);
        if(!r.isSuccess()){
            log.error("failed to update extras to {} for spu(id={}), error code:{} ",
                    extras, id, r.getError());
            throw new JsonResponseException(r.getError());
        }

        return true;
    }


    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean delete(@PathVariable("id")Long id){
        Response<Boolean>  r = spuWriteService.delete(id);
        if(!r.isSuccess()){
            log.error("failed to delete spu(id={}), error code:{} ",
                    id, r.getError());
            throw new JsonResponseException(r.getError());
        }

        return true;
    }

    @RequestMapping(value="/find-for-edit",method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public EditSpu findForEdit(@RequestParam(name = "spuId", required = false) Long id) {
        if (id == null) {
            return new EditSpu();
        }
        val resp = spuReader.findForEdit(id);
        if (!resp.isSuccess()) {
            throw new JsonResponseException(resp.getError());
        }
        return resp.getResult();
    }



}
