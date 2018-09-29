package com.pousheng.middle.web.item.cacher;

import com.pousheng.middle.warehouse.model.VipWarehouseMapping;
import com.pousheng.middle.warehouse.service.VipWarehouseMappingReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * @author zhaoxw
 * @date 2018/9/29
 */
@Slf4j
@Component
@CacheConfig(cacheNames = "VipWarehouse")
public class VipWarehouseMappingProxy {

    @RpcConsumer
    private VipWarehouseMappingReadService vipWarehouseMappingReadService;

    /**
     * 通过仓库id获取
     */
    @Cacheable(key = "'vip#findByWarehouseId:'.concat(#warehouseId.hashCode())")
    public String findByWarehouseId(Long warehouseId) {
        Response<VipWarehouseMapping> resp = vipWarehouseMappingReadService.findByWarehouseId(warehouseId);
        if (!resp.isSuccess()) {
            log.error("failed to find rule (shopId={}, error:{}",
                    warehouseId, resp.getError());
            throw new ServiceException("item.rule.find.fail");
        }
        if (resp.getResult() == null) {
            return null;
        }
        return resp.getResult().getVipStoreSn();
    }

    @CacheEvict(allEntries = true)
    public void refreshAll() {

        log.info("refresh cacher");

    }


}
