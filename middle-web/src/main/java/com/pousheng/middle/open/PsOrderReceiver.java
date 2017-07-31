package com.pousheng.middle.open;

import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.spu.service.PoushengMiddleSpuService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.job.order.component.DefaultOrderReceiver;
import io.terminus.open.client.order.dto.OpenClientFullOrder;
import io.terminus.open.client.order.enums.OpenClientOrderStatus;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SpuReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Created by cp on 7/25/17.
 */
@Component
@Slf4j
public class PsOrderReceiver extends DefaultOrderReceiver {

    @RpcConsumer
    private SpuReadService spuReadService;

    @RpcConsumer
    private PoushengMiddleSpuService middleSpuService;

    @RpcConsumer
    private OrderWriteService orderWriteService;

    @Override
    protected Item findItemById(Long paranaItemId) {
        //TODO use cache

        Response<Spu> findR = spuReadService.findById(paranaItemId);
        if (!findR.isSuccess()) {
            log.error("fail to find spu by id={},cause:{}", paranaItemId, findR.getError());
            return null;
        }
        Spu spu = findR.getResult();

        Item item = new Item();
        item.setId(spu.getId());
        item.setName(spu.getName());
        item.setMainImage(spu.getMainImage_());
        return item;
    }

    @Override
    protected Sku findSkuByCode(String skuCode) {
        //TODO use cache
        Response<SkuTemplate> findR = middleSpuService.findBySkuCode(skuCode);
        if (!findR.isSuccess()) {
            log.error("fail to find sku template by code={},cause:{}",
                    skuCode, findR.getError());
            return null;
        }
        SkuTemplate skuTemplate = findR.getResult();

        Sku sku = new Sku();
        sku.setId(skuTemplate.getId());
        sku.setName(skuTemplate.getName());
        sku.setPrice(skuTemplate.getPrice());
        try {
            sku.setExtraPrice(skuTemplate.getExtraPrice());
        } catch (Exception e) {
            //ignore
        }
        sku.setImage(skuTemplate.getImage_());
        sku.setAttrs(skuTemplate.getAttrs());
        return sku;
    }

    protected void updateParanaOrder(ShopOrder shopOrder, OpenClientFullOrder openClientFullOrder) {
        if (openClientFullOrder.getStatus() == OpenClientOrderStatus.CONFIRMED) {
            Response<Boolean> updateR = orderWriteService.shopOrderStatusChanged(shopOrder.getId(),
                    shopOrder.getStatus(), MiddleOrderStatus.CONFIRMED.getValue());
            if (!updateR.isSuccess()) {
                log.error("failed to change shopOrder(id={})'s status from {} to {} when sync order, cause:{}",
                        shopOrder.getId(), shopOrder.getStatus(), MiddleOrderStatus.CONFIRMED.getValue(), updateR.getError());
            }
        }
    }
}
