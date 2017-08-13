package com.pousheng.middle.open;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.EcpOrderStatus;
import com.pousheng.middle.spu.service.PoushengMiddleSpuService;
import com.taobao.api.domain.Trade;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.job.order.component.DefaultOrderReceiver;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.order.dto.OpenClientFullOrder;
import io.terminus.open.client.order.dto.OpenClientOrderInvoice;
import io.terminus.open.client.order.enums.OpenClientOrderStatus;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.order.dto.RichOrder;
import io.terminus.parana.order.dto.RichSku;
import io.terminus.parana.order.dto.RichSkusByShop;
import io.terminus.parana.order.model.Invoice;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.InvoiceWriteService;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SpuReadService;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    @RpcConsumer
    private InvoiceWriteService invoiceWriteService;

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
        Response<Optional<SkuTemplate>> findR = middleSpuService.findBySkuCode(skuCode);
        if (!findR.isSuccess()) {
            log.error("fail to find sku template by code={},cause:{}",
                    skuCode, findR.getError());
            return null;
        }
        Optional<SkuTemplate> skuTemplateOptional = findR.getResult();
        if (!skuTemplateOptional.isPresent()) {
            return null;
        }
        SkuTemplate skuTemplate = skuTemplateOptional.get();

        Sku sku = new Sku();
        sku.setId(skuTemplate.getId());
        sku.setName(skuTemplate.getName());
        sku.setPrice(skuTemplate.getPrice());
        sku.setSkuCode(skuTemplate.getSkuCode());
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


    protected RichOrder makeParanaOrder(OpenClientShop openClientShop,
                                        OpenClientFullOrder openClientFullOrder) {
        RichOrder richOrder = super.makeParanaOrder(openClientShop, openClientFullOrder);
        //初始化店铺订单的extra
        RichSkusByShop richSkusByShop = richOrder.getRichSkusByShops().get(0);
        Map<String, String> shopOrderExtra = richSkusByShop.getExtra();
        shopOrderExtra.put(TradeConstants.ECP_ORDER_STATUS, String.valueOf(EcpOrderStatus.WAIT_SHIP.getValue()));
        richSkusByShop.setExtra(shopOrderExtra);

        //初始化店铺子单extra
        List<RichSku> richSkus = richSkusByShop.getRichSkus();
        richSkus.forEach(richSku -> {
            Map<String, String> skuExtra = richSku.getExtra();
            skuExtra.put(TradeConstants.WAIT_HANDLE_NUMBER, String.valueOf(richSku.getQuantity()));
            richSku.setExtra(skuExtra);
        });
        //生成发票信息
        Long invoiceId = this.addInvoice(openClientFullOrder.getInvoice());
        richSkusByShop.setInvoiceId(invoiceId);
        return richOrder;
    }

    private Long addInvoice(OpenClientOrderInvoice openClientOrderInvoice) {
        try {
            //获取发票类型
            Integer invoiceType = Integer.valueOf(openClientOrderInvoice.getType());
            //获取抬头
            String title = openClientOrderInvoice.getTitle();
            //获取detail
            Map<String, String> detail = openClientOrderInvoice.getDetail();
            if (detail != null) {
                if (Objects.equals(invoiceType, 2)) {
                    //公司
                    detail.put("titleType", "2");
                }
            } else {
                detail = Maps.newHashMap();
                detail.put("type", String.valueOf(invoiceType));
            }

            Invoice newInvoice = new Invoice();
            newInvoice.setTitle(title);
            newInvoice.setStatus(1);
            newInvoice.setIsDefault(false);
            newInvoice.setDetail(detail);
            Response<Long> response = invoiceWriteService.createInvoice(newInvoice);
            if (!response.isSuccess()) {
                log.error("create invoice failed,caused by {}", response.getError());
                throw new ServiceException("create.invoice.failed");
            }
            return response.getResult();
        } catch (Exception e) {
            log.error("create invoice failed,caused by {}", e.getMessage());
        }
        return null;
    }
}
