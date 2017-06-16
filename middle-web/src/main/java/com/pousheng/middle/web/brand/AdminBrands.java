/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.brand;

import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
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
@Slf4j
@RestController
@RequestMapping("/api/brands")
public class AdminBrands {

    @RpcConsumer
    private BrandWriteService brandWriteService;

    @RpcConsumer
    private BrandReadService brandReadService;


    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Brand> findByNamePrefix(@RequestParam(value = "name", required = false) String namePrefix,
                                        @RequestParam(value = "count", defaultValue = "5") Integer count) {
        Response<List<Brand>> r = brandReadService.findByNamePrefix(namePrefix,count);
        if (!r.isSuccess()) {
            log.warn("failed to find brands by prefix({}), error code:{}", namePrefix, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long create(@RequestBody Brand brand) {
        Response<Long> r = brandWriteService.create(brand);
        if (!r.isSuccess()) {
            log.warn("failed to create {}, error code:{}", brand, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }


    @RequestMapping(value = "/{id}/logo", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean updateLogo(@PathVariable Long id, @RequestParam String url) {
        Brand update = new Brand();
        update.setId(id);
        update.setLogo(url);

        Response<Boolean> tryUpdate = brandWriteService.update(update);
        if (!tryUpdate.isSuccess()) {
            log.error("failed to update {}, error code:{}", update, tryUpdate.getResult());
            throw new JsonResponseException(tryUpdate.getError());
        }
        return Boolean.TRUE;
    }


    /**
     * 更新品牌
     * @return 是否成功
     */
    @RequestMapping(value = "/update", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean updateLogo(@RequestBody Brand brand) {
        Response<Boolean> tryUpdate = brandWriteService.update(brand);
        if (!tryUpdate.isSuccess()) {
            log.error("failed to update {}, error code:{}", brand, tryUpdate.getResult());
            throw new JsonResponseException(tryUpdate.getError());
        }
        return Boolean.TRUE;
    }

    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Paging<Brand>> pagination(@RequestParam(required = false) String namePrefix,
                                              @RequestParam(required = false) Integer pageNo,
                                              @RequestParam(required = false) Integer pageSize) {
        return brandReadService.pagination(pageNo, pageSize, namePrefix);
    }
}
