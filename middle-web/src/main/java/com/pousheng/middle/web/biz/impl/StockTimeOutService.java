package com.pousheng.middle.web.biz.impl;

import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.dto.InventoryTradeDTO;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.common.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import java.util.List;
import java.util.Objects;

/**
 * Created with IntelliJ IDEA
 * User: yujiacheng
 * Date: 2018/7/6
 * Time: 下午2:01
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.STOCK_API_TIME_OUT)
@Service
@Slf4j
public class StockTimeOutService implements CompensateBizService {

    @Autowired
    private InventoryClient inventoryClient;


    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        if (null == poushengCompensateBiz) {
            log.warn("StockTimeOutService.doProcess params is null");
            return;
        }

        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("StockTimeOutService.doProcess context is null");
            throw new BizException("StockTimeOutService.doProcess context is null");
        }

        try {
            List<InventoryTradeDTO> tradeList = JsonMapper.nonEmptyMapper().fromJson(context, JsonMapper.nonEmptyMapper().createCollectionType(List.class,InventoryTradeDTO.class));

            log.info("start to unlock stock info {}", JsonMapper.nonEmptyMapper().toJson(tradeList));
            if (tradeList == null) {
                log.warn("StockTimeOutService.doProcess unLockStock param is null");
                throw new BizException("StockTimeOutService.doProcess unLockStock param is null");
            }

            if (!ObjectUtils.isEmpty(tradeList)) {
                Response<Boolean> tradeRet = inventoryClient.unLock(tradeList);

                if (!tradeRet.isSuccess() && Objects.equals("inventory.occupy.event.not.found", tradeRet.getError())) {
                    // 库存中心没有占用的情况下
                    return;
                }

                if (!tradeRet.isSuccess() || !tradeRet.getResult()) {
                    log.error("fail to unLock inventory, trade trade dto: {}, cause:{}", JsonMapper.nonEmptyMapper().toJson(tradeList), tradeRet.getError());
                    throw new ServiceException(tradeRet.getError());
                }
            }

        } catch (Exception e){
            throw new BizException("try to unLockStock param for shipment fail,caused by {}", e);
        }
    }

}
