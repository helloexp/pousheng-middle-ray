package com.pousheng.middle.web.shop.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Description: 门店订单到期时间
 * User: liangyj
 * Date: 2018/5/11
 */
@Data
@ApiModel("门店订单到期时间")
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true, includeFieldNames = true)
public class OrderExpireInfo implements Serializable {
    private static final long serialVersionUID = -1889152769071213232L;

    @ApiModelProperty(value = "门店ID")
    private Long id;
    @ApiModelProperty(value = "商店外码ID")
    private String outId;
    @ApiModelProperty(value = "行业ID")
    private Long businessId;
    @ApiModelProperty(value = "订单超期时间")
    private LocalDateTime orderExpireTime;
    @ApiModelProperty(value = "订单超期时间发送邮件时间")
    private LocalDateTime emailExpireTime;


}
