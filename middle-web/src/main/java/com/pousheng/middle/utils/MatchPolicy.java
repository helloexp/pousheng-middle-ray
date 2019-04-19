/*
 * Copyright (c) 2014 杭州端点网络科技有限公司
 */

package com.pousheng.middle.utils;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-18 10:20<br/>
 */
public enum MatchPolicy {
    MATCH_ANY("match any"),
    MATCH_NON("match non"),
    MATCH_ALL("match all");

    private final String detail;

    MatchPolicy(String detail) {
        this.detail = detail;
    }

    public String getDetail() {
        return detail;
    }
}
