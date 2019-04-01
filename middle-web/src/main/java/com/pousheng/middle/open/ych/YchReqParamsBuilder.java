package com.pousheng.middle.open.ych;

import com.google.common.collect.Maps;

import java.util.TreeMap;

/**
 * Created by cp on 9/16/17.
 */
public class YchReqParamsBuilder {

    private TreeMap<String, String> params;

    private YchReqParamsBuilder(TreeMap<String, String> params) {
        this.params = params;
    }

    public static YchReqParamsBuilder newBuilder() {
        return new YchReqParamsBuilder(Maps.newTreeMap());
    }

    public YchReqParamsBuilder put(String userId, String userIp, String ati) {
        params.put("userId", userId);
        params.put("userIp", userIp);
        params.put("ati", ati);
        return this;
    }

    public YchReqParamsBuilder put(String key, String value) {
        params.put(key, value);
        return this;
    }

    public TreeMap<String, String> build() {
        return params;
    }

}
