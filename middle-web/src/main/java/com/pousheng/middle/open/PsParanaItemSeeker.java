package com.pousheng.middle.open;

import com.google.common.base.Optional;
import com.pousheng.erp.service.PoushengMiddleSpuService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.item.api.ParanaItemSeeker;
import io.terminus.open.client.center.item.dto.ParanaSku;
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
    public Response<Optional<ParanaSku>> findSkuBySkuCode(String skuCode) {
        Response<Optional<SkuTemplate>> findR = poushengMiddleSpuService.findBySkuCode(skuCode);
        if (!findR.isSuccess()) {
            log.error("fail to find sku template by skuCode={},cause:{}",
                    skuCode, findR.getError());
            return Response.fail(findR.getError());
        }
        Optional<SkuTemplate> skuTemplateOptional = findR.getResult();

        if (skuTemplateOptional.isPresent()) {
            SkuTemplate skuTemplate = skuTemplateOptional.get();
            ParanaSku paranaSku = new ParanaSku();
            paranaSku.setItemId(skuTemplate.getSpuId());
            //TODO 设置其他信息
            return Response.ok(Optional.of(paranaSku));
        }
        return Response.ok(Optional.absent());
    }
}
