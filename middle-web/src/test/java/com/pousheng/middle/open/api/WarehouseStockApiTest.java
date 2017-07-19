package com.pousheng.middle.open.api;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.pousheng.middle.open.api.dto.ErpStock;
import io.terminus.common.utils.JsonMapper;
import org.assertj.core.util.Lists;
import org.junit.Test;

import java.util.Map;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-19
 */
public class WarehouseStockApiTest {

    @Test
    public void onStockChanged() throws Exception {
        Map<String, String> params = Maps.newTreeMap();
        params.put("appKey","pousheng");
        params.put("pampasCall","hk.stock.api");
        params.put("total", "1");

        ErpStock erpStock = new ErpStock();
        erpStock.setBarcode("xxxx");
        erpStock.setCompany_id("200");
        erpStock.setStock_id("200000003");
        erpStock.setQuantity(10);
        erpStock.setModify_time("2017-07-19 15:19:20");
        params.put("data", JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(Lists.newArrayList(erpStock)));

        params.put("sign", sign(params, "middle"));

        HttpRequest r = HttpRequest.post("http://localhost:8080/api/gateway", params, true);

        System.out.println(r.body());
    }

    /**
     * 对参数列表进行签名
     */
    private  String sign(Map<String, String> params, String secret) {
        try {
            String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
            System.out.println(toVerify);

            String sign = Hashing.md5().newHasher()
                    .putString(toVerify, Charsets.UTF_8)
                    .putString(secret, Charsets.UTF_8).hash().toString();
            System.out.println(sign);

            return sign;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}