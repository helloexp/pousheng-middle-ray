package com.pousheng.middle.open.api;

import com.google.common.collect.Maps;
import com.pousheng.middle.open.ych.YchApi;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.TreeMap;

/**
 * Created by cp on 9/15/17.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = YchConfig.class)
@ActiveProfiles("test")
public class YchApiTest {

    @Autowired
    private YchApi ychApi;

    @Test
    public void testSendLoginLog() {
        TreeMap<String, String> params = Maps.newTreeMap();
        params.put("userId", "1");
        params.put("userIp", "115.199.242.116");
        params.put("ati", "4627640147937");
        params.put("tid", "胜道运动旗舰店");
        params.put("loginResult", "success");
        params.put("loginMessage", "登录成功");
        ychApi.sendLoginLog(params);
    }

    @Test
    public void testComputeRisk() {
        TreeMap<String, String> params = Maps.newTreeMap();
        params.put("userId", "1");
        params.put("userIp", "115.199.242.116");
        params.put("ati", "4627640147937");
        ychApi.computeRisk(params);
    }

    @Test
    public void testGetVerifyUrl() {
        TreeMap<String, String> params = Maps.newTreeMap();
        params.put("userId", "1");
        params.put("userIp", "115.199.242.116");
        params.put("ati", "4627640147937");
        params.put("sessionId", String.valueOf(System.currentTimeMillis()));
        params.put("mobile", "18969973054");
        params.put("redirectURL", "http://devt.pousheng.com/api/callback/jd");
        String url = ychApi.getVerifyUrl(params);
        System.out.println("url:" + url);
    }

    @Test
    public void testVerifyPassed() {
        TreeMap<String, String> params = Maps.newTreeMap();
        params.put("token", "55277D16748855476ADA6EB75084BB68");
        Assert.assertTrue(ychApi.isVerifyPassed(params));
    }

}
