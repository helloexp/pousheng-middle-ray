package com.pousheng.middle.open.ych.logger.events;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.open.ych.logger.LogSender;
import com.pousheng.middle.open.ych.token.TaobaoToken;
import com.pousheng.middle.open.ych.token.YchToken;
import io.terminus.session.util.WebUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.TreeMap;

import static com.pousheng.middle.open.ych.utils.IpUtils.isPrivateIPAddress;

/**
 * Created by cp on 9/13/17.
 */
@Slf4j
public class LogListener {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private LogSender logSender;

    @Autowired
    private TaobaoToken token;

    @Autowired
    private YchToken ychToken;

    @PostConstruct
    private void init() {
        eventBus.register(this);
    }

    @Subscribe
    public void onCallTOP(TOPCallEvent topCallEvent) {
        TreeMap<String, String> params = Maps.newTreeMap();
        params.put("userId", "系统");
        params.put("userIp", ychToken.getClientIp());
        params.put("ati", "0000000000");
        params.put("topAppKey", token.getAppKey());
        params.put("appName", "端点中台");
        params.put("url", topCallEvent.getUrl());
        logSender.send(topCallEvent.getRequestPath(), params);
    }

    @Subscribe
    public void onOpOrder(OrderOpEvent orderOpEvent) {
        log.info("start to push log for order operation...");

        HttpServletRequest request = orderOpEvent.getRequest();
        String ip = WebUtil.getClientIpAddr(request);
        if (!StringUtils.hasText(ip)) {
            log.warn("can not get client ip");
            return;
        }

        if (isPrivateIPAddress(ip)) {
            log.warn("catch private ip,skip to send order operation log");
            return;
        }

        String ati = WebUtil.findCookieValue(request, "_ati");
        if (!StringUtils.hasText(ati)) {
            log.warn("can not get ych ati");
            return;
        }
        log.debug("ati is :{}", ati);

        String url = request.getRequestURI();
        log.debug("url is :{}", url);

        TreeMap<String, String> params = Maps.newTreeMap();
        params.put("userId", "宝胜:" + MoreObjects.firstNonNull(orderOpEvent.getUser().getName(), "admin"));
        params.put("userIp", ip);
        params.put("ati", ati);
        params.put("topAppKey", token.getAppKey());
        params.put("appName", "端点中台");
        params.put("url", url);
        params.put("tradeIds", orderOpEvent.getShopOrder().getOutId());
        params.put("operation", orderOpEvent.getOperation());
        logSender.send(orderOpEvent.getRequestPath(), params);
    }
}
