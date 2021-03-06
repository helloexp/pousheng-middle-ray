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

    //private String id;

    private String name;

    //private String pcode; // 省id 由于当一个地址查出多个坐标时高德返回的不是字符串，所以这里就不取了

    //private String citycode; // 区号!!!不是市id

    //private String adcode; // 区id

    private String location;

    private String pname;

    private String cityname;

    //private String adname;

}
