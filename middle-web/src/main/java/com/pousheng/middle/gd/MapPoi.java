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
public class MapPoi implements Serializable {

    private static final long serialVersionUID = 3787049418461442075L;

    private String id;

    private String name;

    private String pcode; // 省id

    private String citycode; // 区号!!!不是市id

    private String adcode; // 区id

    private String location;

}
