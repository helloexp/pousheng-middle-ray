package com.pousheng.middle.hksyc.pos.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 恒康生成发货pos单请求主体
 * Created by songrenfei on 2018/1/16
 */
@Data
public class HkShipmentPosRequestData implements Serializable{

    private static final long serialVersionUID = -5369282654178540346L;

    //请求服务ID 仓发固定为：PS_ERP_POS_netsalshop 店发固定为：PS_ERP_POS_netsalstock
    private String sid;

    //请求时间
    private Date tranReqDate = new Date();

    private HkShipmentPosContent bizContent;
}
