package com.pousheng.middle.web.item.dto;

import io.terminus.open.client.common.mappings.model.ItemMapping;
import lombok.Data;

/**
 * Created by songrenfei on 2019/4/18
 */
@Data
public class ItemMappingInfo extends ItemMapping{

    private static final long serialVersionUID = -6234802603776794441L;

    /**
     * 是否有平均分配的标记
     */
    private Boolean isAverageRatio;

    private String spuCode;
}
