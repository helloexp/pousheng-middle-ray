package com.pousheng.middle.web.biz.impl;

import com.pousheng.middle.order.dispatch.component.MposSkuStockLogic;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.common.StringUtil;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.service.ShipmentReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Created with IntelliJ IDEA
 * User: yujiacheng
 * Date: 2018/7/6
 * Time: 下午2:01
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.STOCK_API_DECREASE_STOCK)
@Service
@Slf4j
public class DecreaseStockService implements CompensateBizService {

    @Autowired
    private ShipmentReadService shipmentReadServices;
    @Autowired
    private MposSkuStockLogic mposSkuStockLogic;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        if (null == poushengCompensateBiz) {
            log.warn("DecreaseStockService.doProcess params is null");
            return;
        }

        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("DecreaseStockService.doProcess context is null");
            throw new BizException("DecreaseStockService.doProcess context is null");
        }
        Long shipmentId = JsonMapper.nonEmptyMapper().fromJson(context, Long.class);

        if (shipmentId == null) {
            log.warn("DecreaseStockService.doProcess unLockStock param shipment is null");
            throw new BizException("DecreaseStockService.doProcess unLockStock param shipment is null");
        }

        try {
            // 获取对应的发货单然后去扣减库存
            Response<Shipment> shipmentResponse =  shipmentReadServices.findById(shipmentId);
            if (!shipmentResponse.isSuccess()) {
                log.error("find shipment by id {} fail", shipmentId);
            }

            Shipment shipment = shipmentResponse.getResult();

            Response<Boolean> result = mposSkuStockLogic.decreaseStock(shipment);
            if (!result.isSuccess() && Objects.equals("inventory.occupy.event.not.found", result.getError())) {
                // 超时异常
                return;
            }

            // 其他类型的错误需要继续轮询，抛出异常给上层捕获
            if (!result.isSuccess()) {
                log.error("biz id {} will be continue, context is {}, error code {}",
                        poushengCompensateBiz.getId(), shipmentId, result.getError());
                throw new ServiceException(result.getError());
            }

        } catch (Exception e){
            throw new BizException("try to decreaseStock param for shipment fail,caused by {}", e);
        }
    }
}
