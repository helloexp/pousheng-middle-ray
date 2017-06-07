package com.pousheng.middle.web.spu;

import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.parana.item.SyncParanaSpuService;
import io.terminus.parana.spu.dto.FullSpu;
import io.terminus.parana.spu.service.SpuReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by songrenfei on 2017/6/7
 */
@RestController
@Slf4j
@RequestMapping("/api/spu")
public class SyncParanaSpus {

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();


    @RpcConsumer
    private SpuReadService spuReadService;
    @Autowired
    private SyncParanaSpuService syncParanaSpuService;

    @RequestMapping(value = "/{id}/sync", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> syncSpu(@PathVariable(name = "id") Long spuId){

        Response<FullSpu> fullSpuRes = spuReadService.findFullInfoBySpuId(spuId);
        if(!fullSpuRes.isSuccess()){
            log.error("find full spu by spu id:{} fail,error:{}",spuId,fullSpuRes.getError());
            throw new JsonResponseException(fullSpuRes.getError());
        }
        return syncParanaSpuService.syncSpus(mapper.toJson(fullSpuRes.getResult()));
    }
}
