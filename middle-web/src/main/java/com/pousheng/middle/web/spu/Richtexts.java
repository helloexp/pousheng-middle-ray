/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.spu;

import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.parana.spu.service.SpuReadService;
import io.terminus.parana.spu.service.SpuWriteService;
import lombok.extern.slf4j.Slf4j;
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


}
