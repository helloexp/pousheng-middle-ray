package com.pousheng.middle.hksyc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YJSyncShipmentRequest implements Serializable {


    private static final long serialVersionUID = 5355581357036506491L;
    /**
     * 订单号
     */
    private String order_sn;

    /**
     * 发货信息 json串
     */
    private List<LogisticsInfo> logistics_info;
}
