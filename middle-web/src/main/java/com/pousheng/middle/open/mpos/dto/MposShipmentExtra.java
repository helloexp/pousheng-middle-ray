package com.pousheng.middle.open.mpos.dto;

import com.google.common.collect.Maps;
import lombok.Data;

import java.util.Map;

/**
 * Created by penghui on 2018/1/10
 */
@Data
public class MposShipmentExtra {

    private Long shipmentId;

    private String status;

    private String mposReceiveStaff;

    private String shipmentSerialNo;

    private String shipmentCorpCode;

    private String shipmentDate;

    private String mposRejectReason;

    public Map<String,String> toExtraMap(){
        Map<String,String> map = Maps.newHashMap();
        map.put("mposReceiveStaff",mposReceiveStaff);
        map.put("shipmentSerialNo",shipmentSerialNo);
        map.put("shipmentCorpCode",shipmentCorpCode);
        map.put("shipmentDate",shipmentDate);
        map.put("mposRejectReason",mposRejectReason);
        return map;
    }
}
