package com.pousheng.middle.order.dto;

import lombok.Data;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/8
 * pousheng-middle
 */
@Data
public class MiddleChangeReceiveInfo implements java.io.Serializable {
    private static final long serialVersionUID = 1800776042152914287L;
    private Long id;
    private Long userId;
    private String receiveUserName;
    private String phone;
    private String mobile;
    private String email;
    private Boolean isDefault;
    private Integer status;
    private String province;
    private Integer provinceId;
    private String city;
    private Integer cityId;
    private String region;
    private Integer regionId;
    private String street;
    private Integer streetId;
    private String detail;
    private String postcode;
}
