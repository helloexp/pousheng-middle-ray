package com.pousheng.middle.open.qimen;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.hash.Hashing;

import java.util.Map;

/**
 * Created by cp on 8/31/17.
 */
public abstract class WmsSignUtils {

    public static String generateSign(String secret, Map<String, Object> params) {
        String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
        return Hashing.md5().newHasher()
                .putString(toVerify, Charsets.UTF_8)
                .putString(secret, Charsets.UTF_8)
                .hash()
                .toString();
    }

}
