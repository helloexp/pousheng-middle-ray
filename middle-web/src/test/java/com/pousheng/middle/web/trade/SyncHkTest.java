package com.pousheng.middle.web.trade;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.web.MiddleConfiguration;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

/**
 * Created by songrenfei on 2017/7/5
 */
@Slf4j
public class SyncHkTest {

    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Test
    public void testEsb(){
        String url ="https://esbt.pousheng.com/common/terminus/base/gethelloworld?name=1923311113";
        String result = HttpRequest.get(url).trustAllHosts().trustAllCerts().header("verifycode","e153ca58197e4931977f6a17a27f0beb").connectTimeout(1000000).readTimeout(1000000).body();
        System.out.println(result);

    }

    protected Map<String, Object> params = Maps.newTreeMap();

    @Test
    public void testHkSyncShipment(){

        params.put("appKey","pousheng");
        params.put("pampasCall","hk.shipments.api");
        params.put("shipmentId","7");
        params.put("hkShipmentId","7");
        params.put("shipmentCorpCode","hkshunfeng");
        params.put("shipmentSerialNo","7423333332");
        params.put("shipmentDate","20160625224210");
        String sign = sign("middle");
        System.out.println("==============sign: "+sign);
        params.put("sign",sign);

        log.info(JsonMapper.nonDefaultMapper().toJson(params));;
        post(middleUrl());

    }
    /**
     * 对参数列表进行签名
     */
    public String sign(String secret) {
        try {
            String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);

            String sign = Hashing.md5().newHasher()
                    .putString(toVerify, Charsets.UTF_8)
                    .putString(secret, Charsets.UTF_8).hash().toString();

            return sign;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String middleUrl() {
        String suffix = Joiner.on('&').withKeyValueSeparator("=").join(params);
        String url =  "http://127.0.0.1:8092/api/gateway" + "?" + suffix;
        System.out.println(url);
        return url;
    }

    public void post(String url){
        String result = HttpRequest.post(url).connectTimeout(1000000).readTimeout(1000000).body();
        System.out.println(result);
    }
}
