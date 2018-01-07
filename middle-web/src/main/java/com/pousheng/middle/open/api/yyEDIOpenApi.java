package com.pousheng.middle.open.api;

import com.google.common.base.Throwables;
import com.pousheng.middle.open.api.dto.YyEdiShipInfo;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.List;

/**
 * 订单派发中心回调中台接口
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/2
 * pousheng-middle
 */
@OpenBean
@Slf4j
public class yyEDIOpenApi {
    /**
     * yyEDI回传发货信息
     * @param shipInfo
     */
    @OpenMethod(key = "yyEDI.shipments.api", paramNames = {"shipInfo"}, httpMethods = RequestMethod.POST)
    public void receiveYYEDIShipmentResult(List<YyEdiShipInfo> shipInfo){
       try{

       }catch (JsonResponseException | ServiceException e) {
        log.error("hk shipment handle result to pousheng fail,error:{}", e.getMessage());
        throw new OPServerException(200,e.getMessage());
       }catch (Exception e){
        log.error("hk shipment handle result failed，caused by {}", Throwables.getStackTraceAsString(e));
        throw new OPServerException(200,"sync.fail");
       }
    }

    @OpenMethod(key = "hk.refund.confirm.received.api", paramNames = {"refundOrderId", "hkRefundOrderId", "itemInfo",
            "receivedDate","itemCode","quantity"
    }, httpMethods = RequestMethod.POST)
    public void syncHkRefundStatus(Long refundOrderId,
                                   @NotEmpty(message = "hk.refund.order.id.is.null") String hkRefundOrderId,
                                   @NotEmpty(message = "item.info.empty") String itemInfo,
                                   @NotEmpty(message = "received.date.empty") String receivedDate
    ) {

    }
}
