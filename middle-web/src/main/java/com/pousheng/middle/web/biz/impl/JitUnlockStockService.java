package com.pousheng.middle.web.biz.impl;

import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.dto.InventoryTradeDTO;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.common.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * JIT 释放库存补偿任务
 *
 * @author tanlongjun
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.JIT_UNLOCK_STOCK_API)
@Service
@Slf4j
public class JitUnlockStockService implements CompensateBizService {

    @Autowired
    private InventoryClient inventoryClient;


    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        if (null == poushengCompensateBiz) {
            log.warn("JITUnlockStockService.doProcess params is null");
            return;
        }

        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("JITUnlockStockService.doProcess context is null");
            throw new BizException("JITUnlockStockService.doProcess context is null");
        }
        List<InventoryTradeDTO> request= JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(context,
            JsonMapper.nonEmptyMapper().createCollectionType(List.class,InventoryTradeDTO.class));
        if(request==null){
            log.error("unlock stock param:{}",context);
            throw new BizException("could not unlock stock.");
        }
        Response<Boolean> response=inventoryClient.unLock(request);
        if(!response.isSuccess()){
            log.error("failed to lock inventory.warehouse id:{},sku info:{}");
            throw new BizException("failed to unlock inventory{}");
        }

    }


}
