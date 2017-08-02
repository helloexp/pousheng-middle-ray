package com.pousheng.middle.web.utils.permission;

import ch.qos.logback.core.joran.conditional.ElseAction;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;

/**
 * Created by sunbo@terminus.io on 2017/7/28.
 */
@Slf4j
@Aspect
@Component
public class ProxyPermissionCheck {


    @Autowired
    private PermissionUtil permissionUtil;


    @Around("execution(* com.pousheng.middle.web.order.*.*(..))")
    public Object check(ProceedingJoinPoint pjp) throws Throwable {

        MethodSignature signature = (MethodSignature) pjp.getSignature();

        int permissionCheckParamPos = 0;
        boolean hasPermissionCheckParamMarked = false;
        String fieldName = null;
        for (Parameter parameter : signature.getMethod().getParameters()) {
            if (parameter.isAnnotationPresent(PermissionCheckParam.class)) {

                if (parameter.getType().getName().equals(Long.class.getName()) ||
                        parameter.getType().getName().equals(long.class.getName()))
                    hasPermissionCheckParamMarked = true;
                else {
                    PermissionCheckParam checkParam = parameter.getAnnotation(PermissionCheckParam.class);
                    if (StringUtils.isNotBlank(checkParam.value())) {
                        Field field = parameter.getType().getField(checkParam.value());
                        if (null != field) {
                            fieldName = fieldName;
                            hasPermissionCheckParamMarked = true;
                        }
                    }
                }
                break;
            }
            permissionCheckParamPos++;
        }


        if (hasPermissionCheckParamMarked) {

            log.debug("this method [{}] has marked permission check", signature.getMethod().getName());

            if (pjp.getArgs()[0] == null) {
                log.info("permission check for method [{}] is aboard,parameter is null", signature.getMethod().getName());
            } else {

                PermissionCheck permissionCheckAnno = signature.getMethod().getAnnotation(PermissionCheck.class);
                if (null == permissionCheckAnno)
                    permissionCheckAnno = pjp.getTarget().getClass().getAnnotation(PermissionCheck.class);

                Long id;
                if (null != fieldName) {
                    try {
                        Field field = pjp.getArgs()[0].getClass().getField(fieldName);
                        field.setAccessible(true);
                        id = (Long) field.get(pjp.getArgs()[0]);
                    } catch (Exception e) {
                        return permissionDeny("permission.check.access.field.fail", signature.getReturnType().getName(), permissionCheckAnno.throwExceptionWhenPermissionDeny());
                    }
                } else
                    id = (Long) pjp.getArgs()[permissionCheckParamPos];


                Response<Boolean> permissionCheckResponse;
                if (permissionCheckAnno.value() == PermissionCheck.PermissionCheckType.SHOP_ORDER) {
                    permissionCheckResponse = permissionUtil.checkByShopOrderID(id);
                } else if (permissionCheckAnno.value() == PermissionCheck.PermissionCheckType.SHOP)
                    permissionCheckResponse = permissionUtil.checkByShopID(id);
                else if (permissionCheckAnno.value() == PermissionCheck.PermissionCheckType.REFUND)
                    permissionCheckResponse = permissionUtil.checkByRefundID(id);
                else
                    permissionCheckResponse = permissionUtil.checkByShipmentID(id);

                if (!permissionCheckResponse.isSuccess()) {

                    return permissionDeny(permissionCheckResponse.getError(), signature.getReturnType().getName(), permissionCheckAnno.throwExceptionWhenPermissionDeny());
                }
            }
        }

        return pjp.proceed();
    }


    private Response permissionDeny(String error, String returnTypeName, boolean throwExceptionWhenPermissionDeny) {
        if (throwExceptionWhenPermissionDeny)
            throw new JsonResponseException(error);


//        String returnTypeName = signature.getReturnType().getName();
        if (returnTypeName.equals(Response.class.getName())) {
            return Response.fail(error);
        } else throw new JsonResponseException(error);
    }

}
