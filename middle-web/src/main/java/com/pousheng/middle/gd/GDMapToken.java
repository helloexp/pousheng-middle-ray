/*
 * Copyright (c) 2017. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.gd;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

/**
 * @author : panxin
 */
@Data
@ConfigurationProperties("gd_map")
public class GDMapToken implements Serializable {

    private static final long serialVersionUID = 3300359163082750988L;

    private String searchApi;

    private String districtApi;

    private String webKey;

}
