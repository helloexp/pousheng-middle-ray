package com.pousheng.middle.open;

import com.google.common.base.Optional;
import com.pousheng.middle.order.enums.MiddleChannel;
import io.terminus.common.model.Response;
import io.terminus.open.client.taobao.component.DefaultTaobaoOrderSeeker;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 查询订单在中台是否已经存在，已经存在则抛出错误
 * @author <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * com.pousheng.middle.open
 * 2018/8/29 15:47
 * pousheng-middle
 */
@Component
@Slf4j
public class PsTaobaoOrderSeek extends DefaultTaobaoOrderSeeker {

    @Autowired
    private ShopOrderReadService shopOrderReadService;
    @Override
    public Response<Boolean> findByOuterOrderId(String outerOrderId) {
        if (log.isDebugEnabled()){
            log.debug("find taobao order for single order api,outerId {}",outerOrderId);
        }
        Response<Optional<ShopOrder>> response = shopOrderReadService.findByOutIdAndOutFrom(outerOrderId, MiddleChannel.TAOBAO.getValue());
        if (!response.isSuccess()){
            return Response.ok(Boolean.TRUE);
        }
        Optional<ShopOrder> shopOrderOptional = response.getResult();
        if (!shopOrderOptional.isPresent()){
            return Response.ok(Boolean.TRUE);
        }
        return Response.fail("taobao.order.exist");
    }
}
