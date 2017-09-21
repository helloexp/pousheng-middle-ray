package com.pousheng.middle.open.ych.events;

import com.google.common.base.MoreObjects;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.open.ych.YchApi;
import com.pousheng.middle.open.ych.YchReqParamsBuilder;
import com.pousheng.middle.web.events.user.LoginEvent;
import io.terminus.common.model.Response;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.session.util.WebUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.TreeMap;

/**
 * Created by cp on 9/16/17.
 */
@Slf4j
public class YchLoginListener {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private YchApi ychApi;

    @PostConstruct
    private void register() {
        eventBus.register(this);
    }

    @Subscribe
    public void onLogin(LoginEvent loginEvent) {
        String ip = WebUtil.getClientIpAddr(loginEvent.getRequest());
        if (!StringUtils.hasText(ip)) {
            log.warn("can not get client ip");
            return;
        }
        log.info("login ip is :{}", ip);
        if ("127.0.0.1".equals(ip)) {
            log.warn("catch localhost ip");
            return;
        }

        String ati = WebUtil.findCookieValue(loginEvent.getRequest(), "_ati");
        if (!StringUtils.hasText(ati)) {
            log.warn("can not get ych ati");
            return;
        }

        final ParanaUser user = loginEvent.getUser();
        String userId = "宝胜:" + MoreObjects.firstNonNull(user.getName(), "admin");
        sendLoginLog(userId, ip, ati);
        computeRisk(userId, ip, ati);
    }

    private void sendLoginLog(String userId, String userIp, String ati) {
        TreeMap<String, String> params = YchReqParamsBuilder.newBuilder()
                .put(userId, userIp, ati)
                .put("tid", "pony官方旗舰店")
                .put("loginResult", "success")
                .put("loginMessage", "登录成功")
                .build();
        Response<Boolean> response = ychApi.sendLoginLog(params);
        if (!response.isSuccess()) {
            log.error("fail to send login log to ych with params:{},cause:{}",
                    params, response.getError());
        }
    }

    private void computeRisk(String userId, String userIp, String ati) {
        TreeMap<String, String> params = YchReqParamsBuilder.newBuilder()
                .put(userId, userIp, ati)
                .build();
        Response<Double> response = ychApi.computeRisk(params);
        if (!response.isSuccess()) {
            log.error("fail to compute risk with params:{},cause:{}",
                    params, response.getError());
            return;
        }

        log.info("risk is {} where userId={},userIp={},ati={}",
                response.getResult(), userId, userId, ati);
    }

}
