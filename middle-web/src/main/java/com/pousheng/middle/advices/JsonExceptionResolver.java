/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.advices;

import com.google.common.base.Throwables;
import com.google.common.net.MediaType;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-02-20
 */
@Slf4j
@ControllerAdvice
public class JsonExceptionResolver {

    private final MessageSource messageSource;

    @Autowired
    public JsonExceptionResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(value = {JsonResponseException.class})
    public void OPErrorHandler(JsonResponseException e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Locale locale = LocaleContextHolder.getLocale();
        log.debug("JsonResponseException happened,locale={}, cause={}",locale, Throwables.getStackTraceAsString(e));
        String message = messageSource.getMessage(e.getMessage(), null, e.getMessage(), locale);
        response.setContentType(MediaType.JSON_UTF_8.toString());
        response.setStatus(e.getStatus());
        try(PrintWriter out = response.getWriter()){
            out.print(JsonMapper.nonDefaultMapper().toJson(ErrorResponse.fail(e.getMessage(),message)));
        }
    }
}



