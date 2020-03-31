package com.pousheng.middle.open.hk;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.vip.osp.sdk.exception.OspException;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.vip.extra.service.VipInvoiceServerice;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMethod;
import vipapis.order.OrderInvoiceReq;

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

    @OpenMethod(key = "vip.oxo.invoice.api", paramNames = {"order_invoice"}, httpMethods = RequestMethod.POST)
    public void confirmInvoice(OrderInvoiceReq orderInvoice) throws OspException {
        log.info("VIP-OXO-INVOICE-CONFIRM-HK,param:{}", JsonMapper.nonEmptyMapper().toJson(orderInvoice));
        List<String> outIds = Lists.newArrayList(orderInvoice.getOrder_id());
        Response<List<ShopOrder>> response = middleOrderReadService.findByOutIdsAndOutFrom(outIds, MiddleChannel.VIPOXO.getValue());
        if(!response.isSuccess() || response.getResult().size()<=0){
            throw new OPServerException(200,"failed to find order by outId:{"+orderInvoice.getOrder_id()+"}");
        }
        Long shopId = response.getResult().get(0).getShopId();
        try {
            vipInvoiceServerice.confirmInvoice(shopId, orderInvoice);
        }catch (OspException oe) {
            log.error("failed to confirm invoice param:{}, returnCode:{}, returnMessage:{}", orderInvoice.toString(),oe.getReturnCode(),oe.getMessage());
            throw new OPServerException(200,oe.getReturnCode()+ oe.getReturnMessage());
        }catch (Exception e) {
            log.error("failed to confirm invoice param:{}", JsonMapper.nonEmptyMapper().toJson(orderInvoice),Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, "confirm.fail");
        }
    }
}
