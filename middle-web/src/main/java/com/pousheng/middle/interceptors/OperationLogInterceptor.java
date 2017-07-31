package com.pousheng.middle.interceptors;

import com.pousheng.middle.order.model.OperationLog;
import com.pousheng.middle.order.service.OperationLogWriteService;
import io.terminus.parana.common.utils.UserUtil;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by sunbo@terminus.io on 2017/7/31.
 */
@Component
public class OperationLogInterceptor extends HandlerInterceptorAdapter {


    @Getter
    @Setter
    private boolean ignoreGetHttpRequest = true;

    @Autowired
    private OperationLogWriteService operationLogWriteService;

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        //
        if (ignoreGetHttpRequest && request.getMethod().equalsIgnoreCase("GET"))
            return;

        String name = UserUtil.getCurrentUser().getName();
        OperationLog log = new OperationLog();
        log.setOperatorName(name);
        log.setContent(getContentFormRequestParameter());
        log.setType(getType());



        operationLogWriteService.create(log);
    }


    private Integer getType() {
        return 0;
    }

    private String getContentFormRequestParameter() {
        return null;
    }
}
