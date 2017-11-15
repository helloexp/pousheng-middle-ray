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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        for (int k = 0;k < 1;k++){
            Map<String, String> params = Maps.newTreeMap();
            params.put("appKey","pousheng");
            params.put("pampasCall","hk.stock.api");
            params.put("total", "1");
            String data = this.getData("/Users/tony/Desktop/stock");
            //String data = "[{\"barcode\":\"4056566556129\",\"company_id\":\"301\",\"material_id\":\"AY5504\",\"modify_time\":\"2017-11-14 19:12:17\",\"quantity\":74,\"size_id\":\"00000298\",\"stock_id\":\"301011047\"},{\"barcode\":\"4056566556167\",\"company_id\":\"301\",\"material_id\":\"AY5504\",\"modify_time\":\"2017-11-14 19:12:17\",\"quantity\":202,\"size_id\":\"00000893\",\"stock_id\":\"301011047\"},{\"barcode\":\"4056566556006\",\"company_id\":\"301\",\"material_id\":\"AY5504\",\"modify_time\":\"2017-11-14 19:12:17\",\"quantity\":153,\"size_id\":\"00000894\",\"stock_id\":\"301011047\"},{\"barcode\":\"4056567138386\",\"company_id\":\"301\",\"material_id\":\"B54293\",\"modify_time\":\"2017-11-14 19:12:17\",\"quantity\":11,\"size_id\":\"00000010\",\"stock_id\":\"301011047\"},{\"barcode\":\"4057284975278\",\"company_id\":\"301\",\"material_id\":\"B74437\",\"modify_time\":\"2017-11-14 19:12:17\",\"quantity\":37,\"size_id\":\"00000010\",\"stock_id\":\"301011047\"},{\"barcode\":\"4057291727990\",\"company_id\":\"301\",\"material_id\":\"BB3563\",\"modify_time\":\"2017-11-14 19:12:17\",\"quantity\":5,\"size_id\":\"00000016\",\"stock_id\":\"301011047\"},{\"barcode\":\"4057291727976\",\"company_id\":\"301\",\"material_id\":\"BB3563\",\"modify_time\":\"2017-11-14 19:12:17\",\"quantity\":6,\"size_id\":\"00000017\",\"stock_id\":\"301011047\"},{\"barcode\":\"4058031530825\",\"company_id\":\"301\",\"material_id\":\"BQ7782\",\"modify_time\":\"2017-11-14 19:12:17\",\"quantity\":11,\"size_id\":\"00000298\",\"stock_id\":\"301011047\"},{\"barcode\":\"4057286735009\",\"company_id\":\"301\",\"material_id\":\"BR1024\",\"modify_time\":\"2017-11-14 19:12:17\",\"quantity\":43,\"size_id\":\"00000893\",\"stock_id\":\"301011047\"},{\"barcode\":\"4058032644545\",\"company_id\":\"301\",\"material_id\":\"BR4058\",\"modify_time\":\"2017-11-14 19:12:17\",\"quantity\":185,\"size_id\":\"00000893\",\"stock_id\":\"301011047\"},{\"barcode\":\"4057283797451\",\"company_id\":\"301\",\"material_id\":\"BW0539\",\"modify_time\":\"2017-11-14 19:12:17\",\"quantity\":106,\"size_id\":\"00000017\",\"stock_id\":\"301011047\"},{\"barcode\":\"4057283797550\",\"company_id\":\"301\",\"material_id\":\"BW0539\",\"modify_time\":\"2017-11-14 19:12:17\",\"quantity\":88,\"size_id\":\"00000018\",\"stock_id\":\"301011047\"},{\"barcode\":\"4058027146603\",\"company_id\":\"301\",\"material_id\":\"CD2331\",\"modify_time\":\"2017-11-14 19:12:17\",\"quantity\":9,\"size_id\":\"00000298\",\"stock_id\":\"301011047\"},{\"barcode\":\"4058027146542\",\"company_id\":\"301\",\"material_id\":\"CD2331\",\"modify_time\":\"2017-11-14 19:12:17\",\"quantity\":20,\"size_id\":\"00000894\",\"stock_id\":\"301011047\"},{\"barcode\":\"4057289868919\",\"company_id\":\"301\",\"material_id\":\"CG5762\",\"modify_time\":\"2017-11-14 19:12:17\",\"quantity\":0,\"size_id\":\"00000008\",\"stock_id\":\"301011047\"},{\"barcode\":\"4057289864836\",\"company_id\":\"301\",\"material_id\":\"CG5762\",\"modify_time\":\"2017-11-14 19:12:17\",\"quantity\":10,\"size_id\":\"00000010\",\"stock_id\":\"301011047\"},{\"barcode\":\"675911109151\",\"company_id\":\"301\",\"material_id\":\"880555001\",\"modify_time\":\"2017-11-14 19:12:17\",\"quantity\":68,\"size_id\":\"00000017\",\"stock_id\":\"301011164\"}]";
            params.put("data", data);
            params.put("sign", sign(params, "6a0e@93204aefe45d47f6e488"));

            //HttpRequest r = HttpRequest.post("http://middle-api-prepub.pousheng.com/api/gateway", params, true);
            HttpRequest r = HttpRequest.post("http://devt-api-middle.pousheng.com/api/gateway", params, true);
            //HttpRequest r = HttpRequest.post("http://localhost:8095/api/gateway", params, true);
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

    private String getData(String dir){
        Path path = Paths.get(dir);
        try {
            List<String> inputs = Files.readAllLines(path, Charsets.UTF_8);
            return inputs.get(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}