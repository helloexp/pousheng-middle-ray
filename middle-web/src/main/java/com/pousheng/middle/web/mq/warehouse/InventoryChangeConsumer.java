package com.pousheng.middle.web.mq.warehouse;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.open.StockPusher;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.dto.InventoryDTO;
import com.pousheng.middle.web.mq.warehouse.model.InventoryChangeDTO;
import io.terminus.common.model.Response;
import io.terminus.common.rocketmq.annotation.ConsumeMode;
import io.terminus.common.rocketmq.annotation.MQConsumer;
import io.terminus.common.rocketmq.annotation.MQSubscribe;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;

/**
 * 库存发生变动，接收mq消息
 *
 * @auther feisheng.ch
 * @time 2018/5/24
 */

@ConditionalOnProperty(name = "inventory.push.topic.consume.enable", havingValue = "true", matchIfMissing = false)
@Component
@Slf4j
@MQConsumer
public class InventoryChangeConsumer {

    @Autowired
    private StockPusher stockPusher;
    @Autowired
    private InventoryClient inventoryClient;

    /**
     * 有库存发生变动的消息过来，开始推送库存
     * @param skuCodeJson
     * @return
     */
    @MQSubscribe(topic = "poushengInventoryTopic", consumerGroup = "inventoryChangeGroup",
            consumeMode = ConsumeMode.CONCURRENTLY)
    public Response<Boolean> handleInventoryChange(String skuCodeJson) {
        if (ObjectUtils.isEmpty(skuCodeJson)) {
            log.error("fail to handle inventory change, because the skuCode is empty");
            return Response.fail("inventory.change.handle.fail");
        }

        try {
            log.info("inventory changed: start to consume mq message, msg:{}",skuCodeJson);

            List<String> skuCodes = Lists.newArrayList();
            List<InventoryChangeDTO> changeDTOS = JSON.parseArray(skuCodeJson, InventoryChangeDTO.class);
            if (!ObjectUtils.isEmpty(changeDTOS)) {
                for (InventoryChangeDTO dto : changeDTOS) {
                    try {
                        if (StringUtils.isNotBlank(dto.getSkuCode())) {
                            skuCodes.add(dto.getSkuCode());
                        } else {
                            if (null != dto.getWarehouseId()) {
                                Response<List<InventoryDTO>> inventoryList = inventoryClient.findSkuStocks(dto.getWarehouseId(), null);
                                if (inventoryList.isSuccess() && !ObjectUtils.isEmpty(inventoryList.getResult())) {
                                    skuCodes.addAll(Lists.transform(inventoryList.getResult(), input -> input.getSkuCode()));
                                }
                            }
                        }
                    }catch (Exception e){
                        log.error("fail to handle inventory change, because: {}", Throwables.getStackTraceAsString(e));
                    }
                }

                stockPusher.submit(skuCodes);
            }

        } catch (Exception e) {
            log.error("fail to handle inventory change, because: {}", Throwables.getStackTraceAsString(e));
            return Response.fail("inventory.change.handle.fail");
        }

        log.info("inventory changed: success to consume mq message");

        return Response.ok(Boolean.TRUE);
    }

}
