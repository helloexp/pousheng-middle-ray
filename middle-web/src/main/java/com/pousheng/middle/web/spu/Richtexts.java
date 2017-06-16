/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.spu;

import com.pousheng.middle.web.utils.RichTextCleaner;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SpuReadService;
import io.terminus.parana.spu.service.SpuWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * Spu 富文本详情接口
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-01-31
 */
@RestController
@Slf4j
@RequestMapping("/api/spu/{id}/detail")
public class Richtexts {

    @RpcConsumer
    private SpuReadService spuReadService;

    @RpcConsumer
    private SpuWriteService spuWriteService;

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean editRichText(@PathVariable("id") Long spuId, @RequestParam("detail") String richText) {

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


        return true;
    }

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String findRichTextById(@PathVariable("id") Long spuId) {
        Response<String> r = spuReadService.findRichTextById(spuId);
        if (!r.isSuccess()) {
            log.error("failed to find rich text detail for spu(id={}), error code:{}",
                    spuId, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }
}
