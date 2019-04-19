package com.pousheng.middle.open.stock;

import com.google.common.base.Stopwatch;
import com.pousheng.middle.web.mq.warehouse.model.InventoryChangeDTO;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 库存推送 包含SkuCode-WarehouseId-ShopId
 *
 * @author tanlongjun
 */
@Component
@Slf4j
public class InventoryPusherClient {
    @Autowired
    private ShopInventoryPusher shopInventoryPusher;

    @Autowired
    private YjInventoryPusher yjInventoryPusher;

    private static final JsonMapper MAPPER = JsonMapper.JSON_NON_EMPTY_MAPPER;

    /**
     * 库存推送
     * 针对仓库级别的skuode和店铺级别的skuCode
     *
     * @param changeDTOList
     */
    public void submit(List<InventoryChangeDTO> changeDTOList) {
        Stopwatch stopwatch = Stopwatch.createStarted();
        log.info("INVENTORY-PUSHER-CLIENT-SUBMIT-START param:{}", MAPPER.toJson(changeDTOList));
        if (CollectionUtils.isEmpty(changeDTOList)) {
            log.warn("inventory change list is empty.skip to push.");
            return;
        }
        shopInventoryPusher.push(changeDTOList);

        yjInventoryPusher.push(changeDTOList);
        stopwatch.stop();
        log.info("INVENTORY-PUSHER-CLIENT-SUBMIT-END,end time:{}ms",
            stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }
}
