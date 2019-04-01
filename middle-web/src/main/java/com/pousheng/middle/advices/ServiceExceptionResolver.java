package com.pousheng.middle.advices;

import com.google.common.base.Throwables;
import io.terminus.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;

/**
 * Author:cp
 * Created on 06/01/2017.
 */
@Slf4j
@ControllerAdvice
public class ServiceExceptionResolver {

    private final MessageSource messageSource;

    @Autowired
    public ServiceExceptionResolver(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(value = {ServiceException.class})
    public void OPErrorHandler(ServiceException e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        Locale locale = LocaleContextHolder.getLocale();
        log.error("ServiceException happened,locale={}, cause={}", locale, Throwables.getStackTraceAsString(e));
        String message = messageSource.getMessage(e.getMessage(), null, e.getMessage(), locale);
        response.sendError(HttpStatus.INTERNAL_SERVER_ERROR.value(), message);
    }
}
