package com.pousheng.middle.open.hk;

import com.google.common.collect.Lists;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.vip.osp.sdk.exception.OspException;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.open.client.vip.extra.service.VipInvoiceServerice;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMethod;
import vipapis.marketplace.invoice.ConfirmInvoiceRequest;
import vipapis.marketplace.invoice.ConfirmInvoiceResponse;

import java.util.List;

/**
 * Description: TODO
 * User:        liangyj
 * Date:        2018/11/20
 */
@OpenBean
@Slf4j
public class VipOxoInvoiceApi {

    @RpcConsumer
    private VipInvoiceServerice vipInvoiceServerice;
    @RpcConsumer
    private MiddleOrderReadService middleOrderReadService;

    @OpenMethod(key = "vip.oxo.invoice.api", paramNames = {"confirmInvoiceRequest"}, httpMethods = RequestMethod.POST)
    public ConfirmInvoiceResponse confirmInvoice(ConfirmInvoiceRequest confirmInvoiceRequest) throws OspException {
        log.info("VIP-OXO-INVOICE-CONFIRM-HK,param:{}",confirmInvoiceRequest.toString());
        List<String> outIds = Lists.newArrayList(confirmInvoiceRequest.getOrder_id());
        Response<List<ShopOrder>> response = middleOrderReadService.findByOutIdsAndOutFrom(outIds, MiddleChannel.VIPOXO.getValue());
        if(!response.isSuccess() || response.getResult().size()<=0){
            throw new OspException("failed to find order(outId:{})",confirmInvoiceRequest.getOrder_id());
        }
        Long shopId = response.getResult().get(0).getShopId();

        confirmInvoiceRequest.getOrder_id();
        return vipInvoiceServerice.confirmInvoice(shopId, confirmInvoiceRequest);
    }
}
