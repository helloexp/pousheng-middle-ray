package com.pousheng.middle.open;

import io.terminus.pampas.openplatform.core.OPMessageSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Author:cp
 * Created on 10/10/16.
 */
@Component
public class OPMessageSources implements OPMessageSource {

    @Autowired
    private MessageSource messageSource;

    public String get(String code, Object... args) {
        if (messageSource == null) {
            return code;
        }
        return messageSource.getMessage(code, args, code, Locale.SIMPLIFIED_CHINESE);
    }
}