/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.advices;

import com.google.common.base.Throwables;
import io.terminus.parana.common.exception.InvalidException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-02-20
 */
@Slf4j
@ControllerAdvice
public class InvalidExceptionResolver {

    private final MessageSource messageSource;

    @Autowired
    public InvalidExceptionResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(value = {InvalidException.class})
    public void ruleChainErrorHandler(InvalidException e, HttpServletResponse response) throws Exception {
        log.debug("InvalidException happened, cause={}", Throwables.getStackTraceAsString(e));
        Locale locale = LocaleContextHolder.getLocale();
        String message = messageSource.getMessage(e.getError(), e.getParams(), e.getError(), locale);
        response.sendError(e.getStatus(), message);
    }
}
