/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.advices;

import com.google.common.base.Throwables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * Just catch all npe, log it , for devops
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-02-21
 */
@ControllerAdvice
@Slf4j
public class NPEResolver {

    private final MessageSource messageSource;

    @Autowired
    public NPEResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(value = {NullPointerException.class})
    public void OPErrorHandler(NullPointerException e, HttpServletResponse response) throws Exception {
        Locale locale = LocaleContextHolder.getLocale();
        log.error("NPE happened, locale={}, cause={}", locale, Throwables.getStackTraceAsString(e));
        String message = messageSource.getMessage("npe.error", null, "npe.error", locale);
        response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }
}
