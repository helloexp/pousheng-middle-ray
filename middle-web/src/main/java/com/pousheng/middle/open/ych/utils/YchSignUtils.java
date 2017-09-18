package com.pousheng.middle.open.ych.utils;

import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.util.Map;

/**
 * Created by cp on 9/18/17.
 */
@Slf4j
public abstract class YchSignUtils {

    public static String getSignature(String appSecret, Map<String, String> params) {
        try {
            StringBuilder combineString = new StringBuilder();
            combineString.append(appSecret);
            for (Map.Entry<String, String> entry : params.entrySet()) {
                combineString.append(entry.getKey() + entry.getValue());
            }
            combineString.append(appSecret);

            byte[] bytesOfMessage = combineString.toString().getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            return bytesToHexString(md.digest(bytesOfMessage));
        } catch (Exception e) {
            log.error("fail to generate signature for params:{},cause:{}",
                    params, Throwables.getStackTraceAsString(e));
            throw new RuntimeException(e);
        }
    }

    private static String bytesToHexString(byte[] src) {
        try {
            if (src == null || src.length <= 0) {
                return null;
            }

            StringBuilder stringBuilder = new StringBuilder("");
            for (int i = 0; i < src.length; i++) {
                int v = src[i] & 0xFF;
                String hv = Integer.toHexString(v);
                if (hv.length() < 2) {
                    stringBuilder.append(0);
                }
                stringBuilder.append(hv);
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            log.error("fail to transfer bytes:{} to hex string,cause:{}",
                    src, Throwables.getStackTraceAsString(e));
            throw new RuntimeException(e);
        }
    }

}
