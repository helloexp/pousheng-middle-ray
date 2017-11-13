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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-19
 */
public class WarehouseStockApiTest {

    @Test
    public void onStockChanged() throws Exception {
        for (int k = 0;k < 100000;k++){
            Map<String, String> params = Maps.newTreeMap();
            params.put("appKey","pousheng");
            params.put("pampasCall","hk.stock.api");
            params.put("total", "1");
            List<ErpStock> stocks = new ArrayList<>();
            for (int i = 0;i <1;i++){
                ErpStock erpStock = new ErpStock();
                erpStock.setBarcode("48602NB0225F");
                erpStock.setCompany_id("301");
                erpStock.setStock_id("301011047");
                erpStock.setQuantity(1000+i);
                erpStock.setModify_time("2017-07-19 15:19:20");
                stocks.add(erpStock);
            }
            params.put("data", JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(stocks));

            params.put("sign", sign(params, "6a0e@93204aefe45d47f6e488"));

            HttpRequest r = HttpRequest.post("http://middle-api-prepub.pousheng.com/api/gateway", params, true);
            //HttpRequest r = HttpRequest.post("http://127.0.0.1:8095/api/gateway", params, true);
            System.out.println(r.body());
        }
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