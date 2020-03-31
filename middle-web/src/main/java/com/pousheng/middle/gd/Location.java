/*
 * Copyright (c) 2017. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.gd;

import lombok.Data;

import java.io.Serializable;

/**
 * @author : panxin
 */
@Data
public class Location implements Serializable {

    private static final long serialVersionUID = -8847643852808827143L;

    private String lon;

    private String lat;

    private String provinceId;

    private String regionId; // 区id

    private String pname;

    private String cityname;

    private String adname;

}
