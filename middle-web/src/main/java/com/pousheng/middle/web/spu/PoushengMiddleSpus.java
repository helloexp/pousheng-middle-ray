package com.pousheng.middle.web.spu;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.spu.service.PoushengMiddleSpuService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.spu.model.Spu;
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

    @RequestMapping(value = "/api/pousheng-spus/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<Spu> findBy(@RequestParam(required = false) String name,
                              @RequestParam(required = false) Long brandId,
                              @RequestParam(required = false) Long id,
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

        if(Arguments.notNull(id)){
            return pagingById(id);
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

}
