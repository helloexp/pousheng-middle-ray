/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.category;

import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.cache.CategoryAttributeCacher;
import io.terminus.parana.category.dto.GroupedCategoryAttribute;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.category.service.BackCategoryReadService;
import io.terminus.parana.category.service.BackCategoryWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-01-15
 */
@RestController
@Slf4j
@RequestMapping("/api/backCategories")
public class AdminBackCategories {

    private final CategoryAttributeCacher categoryAttributeCacher;

    @RpcConsumer
    private BackCategoryReadService backCategoryReadService;

    @RpcConsumer
    private BackCategoryWriteService backCategoryWriteService;

    @Autowired
    public AdminBackCategories(CategoryAttributeCacher categoryAttributeCacher) {
        this.categoryAttributeCacher = categoryAttributeCacher;
    }

    @RequestMapping(value = "/children", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<List<BackCategory>> findChildrenByPid(@RequestParam(value = "pid", defaultValue = "0") Long pid) {
        if(log.isDebugEnabled()){
            log.debug("API-BACKCATEGORIES-CHILDREN-START param: pid [{}]",pid);
        }
        Response<List<BackCategory>> r = backCategoryReadService.findChildrenByPid(pid);
        if (!r.isSuccess()) {
            log.warn("failed to find children of back category(id={}), error code:{}", pid, r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-BACKCATEGORIES-CHILDREN-END param: pid [{}] ,resp: [{}]",pid,r);
        }
        return r;
    }

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<BackCategory> create(@RequestBody BackCategory backCategory) {
        if(log.isDebugEnabled()){
            log.debug("API-BACKCATEGORIES-CREATE-START param: backCategory [{}]",backCategory);
        }
        Response<BackCategory> r = backCategoryWriteService.create(backCategory);
        if (!r.isSuccess()) {
            log.warn("failed to create {}, error code:{}", backCategory, r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-BACKCATEGORIES-CREATE-END param: backCategory [{}] ,resp: [{}]",backCategory,r);
        }
        return r;
    }

    @RequestMapping(value = "/{id}/name", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> update(@PathVariable("id") long id,
                              @RequestParam("name") String name) {
        if(log.isDebugEnabled()){
            log.debug("API-BACKCATEGORIES-ID-NAME-START param: id [{}] name [{}]",id,name);
        }
        Response<Boolean> r = backCategoryWriteService.updateName(id, name);
        if (!r.isSuccess()) {
            log.warn("failed to update back category(id={}) name to {} ,error code:{}", id, name, r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-BACKCATEGORIES-ID-NAME-END param: id [{}] name [{}] ,resp: [{}]",id,name,r);
        }
        return r;
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> disable(@PathVariable("id") Long id) {
        if(log.isDebugEnabled()){
            log.debug("API-BACKCATEGORIES-ID-START param: id [{}]",id);
        }
        Response<Boolean> r = backCategoryWriteService.disable(id);
        if (!r.isSuccess()) {
            log.warn("failed to editable back category(id={}) ,error code:{}", id, r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-BACKCATEGORIES-ID-END param: id [{}] ,resp: [{}]",id,r);
        }
        return r;
    }

    @RequestMapping(value = "/{id}/grouped-attribute", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<GroupedCategoryAttribute> findGroupedAttributeByCategoryId(@PathVariable Long id) {
        if(log.isDebugEnabled()){
            log.debug("API-BACKCATEGORIES-ID-GROUPED-ATTRIBUTE-START param: id [{}]",id);
        }
        List<GroupedCategoryAttribute> categoryAttributes = categoryAttributeCacher.findGroupedAttributeByCategoryId(id);
        if(log.isDebugEnabled()){
            log.debug("API-BACKCATEGORIES-ID-GROUPED-ATTRIBUTE-END param: id [{}] ,resp: [{}]",id,categoryAttributes);
        }
        return categoryAttributes;
    }
}
