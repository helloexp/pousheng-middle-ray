package com.pousheng.middle.hksyc.pos.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 恒康拒收单请求实体
 */
@Data
public class HkSaleRefuseRequestData implements Serializable{

    private static final long serialVersionUID = -5369282654178540346L;

    /**
     * 拒收单固定为PS_ERP_POS_netsalrefuse
     */
    private String sid;

    //请求时间
    private String tranReqDate;
    //请求实体
    private HkSaleRefuseContent bizContent;
}
