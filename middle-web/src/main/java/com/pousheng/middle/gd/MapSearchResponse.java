/*
 * Copyright (c) 2017. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.gd;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author : panxin
 */
@Data
public class MapSearchResponse implements Serializable {

    private static final long serialVersionUID = 1416872202841454971L;

    // 0: 请求失败; 1: 请求成功
    private Integer status;

    private Integer count;

    private String info;

    private String infocode;

    private List<MapPoi> pois;

    public enum Status {

        SUCCEED(1),
        FAILED(0);

        private final int value;

        Status(int value) {
            this.value = value;
        }

        public int value() {
            return this.value;
        }

    }

}
