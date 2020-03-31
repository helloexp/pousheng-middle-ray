package com.pousheng.middle.group.model;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 渠道商品推送日志
 * AUTHOR: zhangbin
 * ON: 2019/7/17
 */
@Data
public class ChannelItemPushLog {

    private Long id;

    private String channel;

    private String spuCode;

    private String skuCode;

    private Long brandId;

    private String channelItemId;

    private String channelSkuId;

    private String extraJson;
    /**
     * @see com.pousheng.middle.item.constant.ItemPushStatus
     */
    private Integer status;

    private String cause;

    private Date createdAt;

    private Date updatedAt;

    private Map<String, String> extra;

    public void setExtraJson(String extraJson) {
        this.extraJson = extraJson;
        if (Strings.isNullOrEmpty(extraJson)) {
            this.extra = Collections.emptyMap();
        } else {
            this.extra = Maps.newHashMap();
            List<Map> maps = JSON.parseArray(extraJson, Map.class);
            for (Map map : maps) {
                extra.putAll(map);
            }
        }
    }

    public void setExtra(Map<String, String> extra) {
        this.extra = extra;
        if (extra == null || extra.isEmpty()) {
            this.extraJson = null;
        } else {
            try {
                this.extraJson = JSON.toJSONString(extra);
            } catch (Exception e) {
                //ignore this fuck exception
            }
        }
    }
}
