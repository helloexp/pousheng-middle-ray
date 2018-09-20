package com.pousheng.middle.web.utils;

import io.terminus.pampas.openplatform.exceptions.OPServerException;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;

public class ApiParamUtil {

    /**
     * 验证实体对象是否为空
     *
     * @param bean
     * @param attributeName 属性字段
     */
    public static void validateRequired(Object bean, String... attributeName) {
        List<String> list = Arrays.asList(attributeName);
        PropertyDescriptor origDescriptors[] = PropertyUtils.getPropertyDescriptors(bean);
        for (PropertyDescriptor origDescriptor : origDescriptors) {

            String name = origDescriptor.getName();
            if (!list.contains(name)) {
                continue;
            }
            if ("class".equals(name)) {
                continue;
            }
            if (PropertyUtils.isReadable(bean, name)) {
                Object value = null;
                try {
                    value = PropertyUtils.getSimpleProperty(bean, name);
                } catch (IllegalAccessException e) {
                    throw new OPServerException(200, name + " cannot access");
                } catch (InvocationTargetException e) {
                    throw new OPServerException(200, name + " cannot invocation");
                } catch (NoSuchMethodException e) {
                    throw new OPServerException(200, name + " not defined");
                }
                if (value == null) {
                    throw new OPServerException(200, name + " is null");
                }
                if (origDescriptor.getPropertyType().equals(String.class)) {
                    if (StringUtils.isBlank(value.toString())) {
                        throw new OPServerException(200, name + " is empty");
                    }
                }

            }
        }
    }
}
