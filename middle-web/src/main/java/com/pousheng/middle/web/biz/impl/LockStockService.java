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

/**
 * @Description: 对于占库失败的进行补偿
 * @author: yjc
 * @date: 2018/9/27下午12:35
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.LOCK_STOCK_EVENT)
@Service
@Slf4j
public class LockStockService implements CompensateBizService {


        @Autowired
        private InventoryClient inventoryClient;

        @Override
        public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {

            if (null == poushengCompensateBiz) {
                log.warn("LockStockService.doProcess params is null");
                return;
            }

            String context = poushengCompensateBiz.getContext();
            if (StringUtil.isBlank(context)) {
                log.warn("LockStockService.doProcess context is null");
                throw new BizException("LockStockService.doProcess context is null");
            }

            try {
                List<InventoryTradeDTO> tradeList = JsonMapper.nonEmptyMapper().fromJson(context, JsonMapper.nonEmptyMapper().createCollectionType(List.class,InventoryTradeDTO.class));

                log.info("start to un lock stock info {}", JsonMapper.nonEmptyMapper().toJson(tradeList));
                if (tradeList == null) {
                    log.warn("LockStockService.doProcess unLockStock param is null");
                    throw new BizException("LockStockService.doProcess unLockStock param is null");
                }

                if (!ObjectUtils.isEmpty(tradeList)) {
                    Response<Boolean> tradeRet = inventoryClient.unLockForCompensate(tradeList);

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
