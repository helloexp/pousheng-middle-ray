package com.pousheng.middle.open;

import com.pousheng.middle.spu.service.PoushengMiddleSpuService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.item.api.ParanaItemSeeker;
import io.terminus.open.client.center.item.dto.ParanaItem;
import io.terminus.parana.spu.model.SkuTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Created by cp on 8/10/17.
 */
@Component
@Slf4j
public class PsParanaItemSeeker implements ParanaItemSeeker {

    @RpcConsumer
    private PoushengMiddleSpuService poushengMiddleSpuService;

    @Override
    public ParanaItem findBySkuCode(String skuCode) {
        Response<SkuTemplate> finR = poushengMiddleSpuService.findBySkuCode(skuCode);
        if (!finR.isSuccess()) {
            log.error("fail to find sku template by skuCode={},cause:{}",
                    skuCode, finR.getError());
            throw new ServiceException(finR.getError());
        }
        SkuTemplate skuTemplate = finR.getResult();

        ParanaItem paranaItem = new ParanaItem();
        paranaItem.setItemId(skuTemplate.getSpuId());
        //TODO 设置其他信息
        return paranaItem;
    }
}
