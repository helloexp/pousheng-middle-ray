package com.pousheng.middle.open.mpos.dto;

import com.google.common.collect.Maps;
import lombok.Data;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by penghui on 2018/1/10
 */
@Data
public class MposShipmentExtra {

    private final static DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyyMMddHHmmss");

    /**
     *
     */
    private Long id;


    /**
     * 下单店铺ID
     */
    private Long fromShopId;

    /**
     * 下单店铺名称
     */
    private String fromShopName;

    /**
     * 接单店铺ID
     */
    private Long dispatchShopId;

    /**
     * 接单店铺名称
     */
    private String dispatchShopName;

    /**
     * 发货单id
     */
    private Long shipmentId;

    /**
     * 外部发货单ID
     */
    private Long outerShipmentId;

    /**
     * 店铺订单ID
     */
    private String shopOrderId;

    /**
     * 子订单IDS
     */
    private String skuOrderIds;

    /**
     * 子订单IDS不存数据库
     */
    private List<Long> skuOrderIdList;

    /**
     * 快递单号
     */
    private String shipmentSerialNo;

    /**
     * 快递代码
     */
    private String shipmentCorpCode;

    /**
     * 接单人姓名或编号
     */
    private String acceptName;

    /**
     * 拒绝原因
     */
    private String reason;

    /**
     * 状态 0:待接单，1：已接单， 2：呼叫快递  3:已发货， -1：已拒绝
     */
    private Integer status;

    /**
     * 发货时间
     */
    private Date shipmentDate;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 修改时间
     */
    private Date updatedAt;


    public Map<String,String> transToMap(){
        Map<String,String> map = Maps.newHashMap();
        map.put("mposReceiveStaff",acceptName);
        map.put("shipmentSerialNo",shipmentSerialNo);
        map.put("shipmentCorpCode",shipmentCorpCode);
        if(Objects.nonNull(shipmentDate))
            map.put("shipmentDate",DFT.print(shipmentDate.getTime()));
        map.put("mposRejectReason",reason);
        return map;
    }
}
