/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.brand;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.brand.model.Brand;
import io.terminus.parana.brand.service.BrandReadService;
import io.terminus.parana.brand.service.BrandWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-01-15
 */
@Api(description = "品牌API")
@Slf4j
@RestController
@RequestMapping("/api/brands")
public class AdminBrands {

    @RpcConsumer
    private BrandWriteService brandWriteService;

    @RpcConsumer
    private BrandReadService brandReadService;


    @ApiOperation("根据品牌名称搜索品牌信息")
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Brand> findByNamePrefix(@RequestParam(value = "name", required = false) String namePrefix,
                                        @RequestParam(value = "count", defaultValue = "10") Integer count) {
        if(log.isDebugEnabled()){
            log.debug("API-BRANDS-FINDBYNAMEPREFIX-START param: namePrefix [{}] count [{}]",namePrefix,count);
        }
        Response<List<Brand>> r = brandReadService.findByNamePrefix(namePrefix,count);
        if (!r.isSuccess()) {
            log.warn("failed to find brands by prefix({}), error code:{}", namePrefix, r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-BRANDS-FINDBYNAMEPREFIX-END param: namePrefix [{}] count [{}] resp: [{}]",namePrefix,count,JsonMapper.nonEmptyMapper().toJson(r.getResult()));
        }
        return r.getResult();
    }

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long create(@RequestBody Brand brand) {
        String brandStr = JsonMapper.nonEmptyMapper().toJson(brand);
        if(log.isDebugEnabled()){
            log.debug("API-BRANDS-CREATE-START param: brand [{}] ",brandStr);
        }
        Response<Long> r = brandWriteService.create(brand);
        if (!r.isSuccess()) {
            log.warn("failed to create {}, error code:{}", brand, r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-BRANDS-CREATE-END param: brand [{}] ,resp: [{}]",brandStr,JsonMapper.nonEmptyMapper().toJson(r.getResult()));
        }
        return r.getResult();
    }


    @RequestMapping(value = "/{id}/logo", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean updateLogo(@PathVariable Long id, @RequestParam String url) {
        if(log.isDebugEnabled()){
            log.debug("API-BRANDS-ID-LOGO-START param: id [{}] url [{}]",id,url);
        }
        Brand update = new Brand();
        update.setId(id);
        update.setLogo(url);

        Response<Boolean> tryUpdate = brandWriteService.update(update);
        if (!tryUpdate.isSuccess()) {
            log.error("failed to update {}, error code:{}", update, tryUpdate.getResult());
            throw new JsonResponseException(tryUpdate.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-BRANDS-ID-LOGO-END param: id [{}] url [{}] ,resp: [{}]",id,url,true);
        }
        return Boolean.TRUE;
    }


    /**
     * 更新品牌
     * @return 是否成功
     */
    @RequestMapping(value = "/update", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean updateLogo(@RequestBody Brand brand) {
        String brandStr = JsonMapper.nonEmptyMapper().toJson(brand);
        if(log.isDebugEnabled()){
            log.debug("API-BRANDS-UPDATE-START param: brand [{}] ",brandStr);
        }
        Response<Boolean> tryUpdate = brandWriteService.update(brand);
        if (!tryUpdate.isSuccess()) {
            log.error("failed to update {}, error code:{}", brand, tryUpdate.getResult());
            throw new JsonResponseException(tryUpdate.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-BRANDS-UPDATE-END param: brand [{}] ,resp: [{}]",brandStr,true);
        }
        return Boolean.TRUE;
    }

    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Paging<Brand>> pagination(@RequestParam(required = false) String namePrefix,
                                              @RequestParam(required = false) Integer pageNo,
                                              @RequestParam(required = false) Integer pageSize) {
        if(log.isDebugEnabled()){
            log.debug("API-BRANDS-PAGING-START param: namePrefix [{}] pageNo [{}] pageSize [{}]",namePrefix,pageNo,pageSize);
        }
        Response<Paging<Brand>> resp = brandReadService.pagination(pageNo, pageSize, namePrefix);
        if(log.isDebugEnabled()){
            log.debug("API-BRANDS-PAGING-END param: namePrefix [{}] pageNo [{}] pageSize [{}] ,resp: [{}]",namePrefix,pageNo,pageSize,resp);
        }
        return resp;
    }
}
