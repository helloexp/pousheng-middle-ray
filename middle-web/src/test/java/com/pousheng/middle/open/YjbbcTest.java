package com.pousheng.middle.open;

import com.alibaba.fastjson.JSONObject;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;

import java.util.Map;

/**
 * @author zhurg
 * @date 2019/7/2 - 下午1:59
 */
public class YjbbcTest {

    public static void main(String[] args) {
        Map<String, Object> params = Maps.newTreeMap();
        params.put("appKey", "pousheng");
        JSONObject jsonObject = JSONObject.parseObject("{\"refund\":{\"outOrderId\":\"311070413+100\",\"outAfterSaleOrderId\":\"311070413+100\",\"buyerName\":\"didi\",\"fee\":59700,\"type\":2,\"buyerMobile\":\"156767677777\",\"buyerNote\":\"不好\",\"sellerNote\":\"统一\",\"status\":1,\"expressCode\":\"\",\"applyAt\":\"20190420 19:06:03\",\"returnStockid\":\"\",\"channel\":\"yunjubbc\"},\"items\":[{\"afterSaleId\":\"311070413+100\",\"skuAfterSaleId\":\"311070413+100\",\"skuCode\":\"4057283468351\",\"itemName\":\"BW0620 颜色: 黑色 尺码: 9\",\"quantity\":3,\"fee\":59700}]}");
        params.put("data", jsonObject.toJSONString());
        params.put("pampasCall", "out.order.refund.api");
        String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
        String sign = Hashing.md5().newHasher().putString(toVerify, Charsets.UTF_8).putString("6a0e@93204aefe45d47f6e488", Charsets.UTF_8).hash().toString();
        System.out.println(sign);
        params.put("sign",sign);
        String url = "http://localhost:8092/api/gateway";
        String response = HttpRequest.post(url)
                .contentType("application/x-www-form-urlencoded")
                .form(params)
                .connectTimeout(10000).readTimeout(10000).body();

        System.out.println(response);
    }
}
