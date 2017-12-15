/*
 * Copyright (c) 2017. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.gd;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * @author : panxin
 */
@Data
public class GDSearchRequestParams implements Serializable {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    private static final long serialVersionUID = -8205717656143164968L;

    private String key;

    private String keywords;

    private String types;

    private String city;

    private Integer offset;

    private Integer page;

    private String extensions;

    public Map toMap() {
        return MAPPER.convertValue(this, Map.class);
    }

    public enum Extensions {

        ALL("all"),
        BASE("base");

        private final String key;

        Extensions(String key) {
            this.key = key;
        }

        public String key() {
            return this.key;
        }

        @Override
        public String toString() {
            return this.key;
        }
    }

}
