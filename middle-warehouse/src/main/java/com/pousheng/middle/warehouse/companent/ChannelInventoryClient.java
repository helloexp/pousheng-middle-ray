package com.pousheng.middle.warehouse.companent;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.dto.AvailableInventoryDTO;
import com.pousheng.middle.warehouse.dto.AvailableInventoryRequest;
import com.pousheng.middle.warehouse.dto.InventoryDTO;
import com.pousheng.middle.warehouse.dto.InventoryTradeDTO;
import com.pousheng.middle.warehouse.model.PoushengChannelDTO;
import com.pousheng.middle.warehouse.model.SkuInventory;
import com.pousheng.middle.warehouse.model.WarehouseShopStockRule;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Map;

/**
 * 渠道库存操作相关接口
 *
 * @auther feisheng.ch
 * @time 2018/5/25
 */
@Component
@Slf4j
public class ChannelInventoryClient {

    @Autowired
    private InventoryBaseClient inventoryBaseClient;

    /**
     * 批量插入渠道库存
     * @param channelDTOS
     * @return
     */
    public Response<String> batchCreate(List<PoushengChannelDTO> channelDTOS) {
        if (ObjectUtils.isEmpty(channelDTOS)) {
            return Response.fail("channel.batch.create.fail.parameter");
        }

        try {

            return Response.ok((String)inventoryBaseClient.postJson("api/inventory/channel",
                    JSON.toJSONString(channelDTOS), String.class));

        } catch (Exception e) {
            log.error("batch create channel inventory fail, cause:{}", Throwables.getStackTraceAsString(e));

            return Response.fail(e.getMessage());
        }

    }

}
