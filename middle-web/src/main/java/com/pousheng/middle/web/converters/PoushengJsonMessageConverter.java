/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.converters;

import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-07-06
 */
public class PoushengJsonMessageConverter extends MappingJackson2HttpMessageConverter {
    @Override
    protected void writeInternal(Object object, Type type, HttpOutputMessage outputMessage) throws IOException, HttpMessageNotWritableException {
        if(object instanceof Response){
            Response<?> r = (Response<?>)object;
            if(r.isSuccess()){
                super.writeInternal(r.getResult(), r.getResult().getClass(),outputMessage);
            }else{
                throw new JsonResponseException(500, r.getError());
            }
        }else{
            super.writeInternal(object, type, outputMessage);
        }
    }
}
