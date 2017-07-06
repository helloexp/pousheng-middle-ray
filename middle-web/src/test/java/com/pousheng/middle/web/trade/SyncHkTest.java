package com.pousheng.middle.web.trade;

import com.github.kevinsawicki.http.HttpRequest;
import org.junit.Test;

/**
 * Created by songrenfei on 2017/7/5
 */
public class SyncHkTest {


    @Test
    public void testEsb(){
        String url ="https://esbt.pousheng.com/common/terminus/base/gethelloworld?name=1923311113";
        String result = HttpRequest.get(url).trustAllHosts().trustAllCerts().header("verifycode","e153ca58197e4931977f6a17a27f0beb").connectTimeout(1000000).readTimeout(1000000).body();
        System.out.println(result);

    }
}
