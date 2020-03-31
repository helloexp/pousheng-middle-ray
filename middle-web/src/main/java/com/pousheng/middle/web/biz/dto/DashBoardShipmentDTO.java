package com.pousheng.middle.web.biz.dto;


import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 门店派单同步dashborad
 *
 */
@Data
public class DashBoardShipmentDTO implements Serializable {

    /**
     *   单号
     */
    private String billNo;
    /**
     *   金额
     */
    private Long amount;
    /**
     *   生成时间
     */
    private String billDate;
    /**
     *   出货店铺代码（提醒信息的店铺）
     */
    private String outShopCode;
    /**
     *   来源店铺代码
     */
    private String sourceShopCode;
    /**
     *   订单类型
     */
    private String billType;
    /**
     *   来源店铺公司代码
     */
    private String sourceShopCompanyCode;
    /**
     *   出货店铺公司代码
     */
    private String destinationCompanyCode;
    /**
     *   商品信息
     */
    private List<DashBoardShipmentItemDTO> goodsList;
}
