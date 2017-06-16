package com.pousheng.middle.web.spu;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.middle.spu.service.PoushengMiddleSpuService;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.spu.model.Spu;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @RequestMapping(value = "/api/pousheng-spus/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<Spu> findBy(@RequestParam(required = false) String name,
                              @RequestParam(required = false) Long brandId,
                              @RequestParam(required = false) Integer type,
                              @RequestParam(required = false) Integer pageNo,
                              @RequestParam(required = false) Integer pageSize){
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

        try {
            Response<Paging<Spu>> r = poushengMiddleSpuService.findBy(pageNo, pageSize, params);
            if(!r.isSuccess()){
                log.error("failed to find spus by {}, error code:{}", params, r.getError());
                throw new JsonResponseException(r.getError());
            }
            return r.getResult();
        } catch (Exception e) {
            log.error("failed to find spus by {}, cause:{}", params, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException(500, "spu.find.fail");
        }
    }
}
