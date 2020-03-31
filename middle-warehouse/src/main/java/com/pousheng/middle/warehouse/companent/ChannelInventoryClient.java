package com.pousheng.middle.warehouse.companent;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Throwables;
import com.pousheng.middle.warehouse.model.PoushengChannelDTO;
import io.terminus.applog.annotation.LogMe;
import io.terminus.applog.annotation.LogMeContext;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.List;

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
    @LogMe(description = "批量创建指定库存（渠道库存）", ignore = true)
    public Response<String> batchCreate(@LogMeContext List<PoushengChannelDTO> channelDTOS) {
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

    /**
     * 更新仓库标签
     * @param json
     * @return
     */
    public Response<String> updateWarehouseTags(String json) {
        if (StringUtils.isEmpty(json)) {
            return Response.fail("标签数据不存在");
        }
        try {
            Object result = inventoryBaseClient.postJson("api/inventory/warehouse/tags", json, String.class);
            if (result == null) {
                return Response.fail("更新仓库标签出错");
            }
            return Response.ok((String)result);
        } catch (Exception e) {
            String errorMsg = "更新仓库标签出错:" + e.getMessage();
            log.error(errorMsg);
            e.printStackTrace();
            return Response.fail(errorMsg);
        }
    }

}
