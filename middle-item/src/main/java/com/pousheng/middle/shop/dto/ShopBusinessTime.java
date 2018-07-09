package com.pousheng.middle.shop.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;

/**
 * Description: 门店营业信息DTO
 * User: liangyj
 * Date: 2018/5/8
 */
@Data
@ApiModel("门店营业时间")
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, includeFieldNames = true)
public class ShopBusinessTime implements Serializable {

    private static final long serialVersionUID = -1889156869071213266L;

    @ApiModelProperty(value = "营业状态（总的状态） 1：营业；2：歇业")
    Integer openingStatus;

    @ApiModelProperty(value = "营业状态（周一）1：营业；2：歇业")
    Integer openingStatusMon;
    @ApiModelProperty(value = "开始营业时间（周一）")
    String openingStartTimeMon;
    @ApiModelProperty(value = "结束营业时间（周一）")
    String openingEndTimeMon;

    @ApiModelProperty(value = "营业状态（周二）1：营业；2：歇业")
    Integer openingStatusTue;
    @ApiModelProperty(value = "开始营业时间（周二）")
    String openingStartTimeTue;
    @ApiModelProperty(value = "结束营业时间（周二）")
    String openingEndTimeTue;

    @ApiModelProperty(value = "营业状态（周三）1：营业；2：歇业")
    Integer openingStatusWed;
    @ApiModelProperty(value = "开始营业时间（周三）")
    String openingStartTimeWed;
    @ApiModelProperty(value = "结束营业时间（周三）")
    String openingEndTimeWed;

    @ApiModelProperty(value = "营业状态（周四）1：营业；2：歇业")
    Integer openingStatusThu;
    @ApiModelProperty(value = "开始营业时间（周四）")
    String openingStartTimeThu;
    @ApiModelProperty(value = "结束营业时间（周四）")
    String openingEndTimeThu;

    @ApiModelProperty(value = "营业状态（周五）1：营业；2：歇业")
    Integer openingStatusFri;
    @ApiModelProperty(value = "开始营业时间（周五）")
    String openingStartTimeFri;
    @ApiModelProperty(value = "结束营业时间（周六）")
    String openingEndTimeFri;

    @ApiModelProperty(value = "营业状态（周六）1：营业；2：歇业")
    Integer openingStatusSat;
    @ApiModelProperty(value = "开始营业时间（周六）")
    String openingStartTimeSat;
    @ApiModelProperty(value = "结束营业时间（周六）")
    String openingEndTimeSat;

    @ApiModelProperty(value = "营业状态（周日）1：营业；2：歇业")
    Integer openingStatusSun;
    @ApiModelProperty(value = "开始营业时间（周日）")
    String openingStartTimeSun;
    @ApiModelProperty(value = "结束营业时间（周六）")
    String openingEndTimeSun;

    @ApiModelProperty(value = "最大接单量")
    Integer orderAcceptQtyMax;

    @ApiModelProperty(value = "订单超时时间")
    Integer orderTimeout;
    @ApiModelProperty(value = "订单邮件发送超时时间")
    Integer orderEmailTimeout;

}
