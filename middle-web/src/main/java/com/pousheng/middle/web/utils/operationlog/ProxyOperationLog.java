package com.pousheng.middle.web.utils.operationlog;

import com.pousheng.middle.order.model.OperationLog;
import com.pousheng.middle.order.service.OperationLogWriteService;
import com.pousheng.middle.web.utils.operationlog.OperationLogKey;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.common.utils.UserUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.map.HashedMap;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Created by sunbo@terminus.io on 2017/7/31.
 */
@Slf4j
@Component
@Aspect
public class ProxyOperationLog {


    @Autowired
    private HttpServletRequest request;

    @Autowired
    private OperationLogWriteService operationLogWriteService;


    @Pointcut("execution(* com.pousheng.middle.web.order.*.*(..))")
    public void orderPointcut() {
    }

    @Pointcut("execution(* com.pousheng.middle.web.warehouses.*.*(..))")
    public void warehousePointcut() {
    }


    @AfterReturning("orderPointcut() || warehousePointcut()")
    public void record(JoinPoint pjp) {


        MethodSignature signature = (MethodSignature) pjp.getSignature();

        if (signature.getMethod().isAnnotationPresent(OperationLogIgnore.class)) {
            log.debug("method [{}] annotation OperationLogIgnore,ignore record operation log", signature.getMethod().getName());
            return;
        }

        if (!signature.getMethod().isAnnotationPresent(OperationLogType.class)
                && request.getMethod().equalsIgnoreCase("GET")) {
            log.debug("method [{}] not annotation OperationLogType and request by http GET,ignore record operation log", signature.getMethod().getName());
            return;
        }

        if (!signature.getMethod().isAnnotationPresent(RequestMapping.class)
                && !signature.getMethod().isAnnotationPresent(PostMapping.class)
                && !signature.getMethod().isAnnotationPresent(PutMapping.class)
                && !signature.getMethod().isAnnotationPresent(DeleteMapping.class)
                && !signature.getMethod().isAnnotationPresent(GetMapping.class)) {
            log.debug("method [{}] not annotation RequestMapping or PostMapping or PutMapping or DeleteMapping or GetMapping,ignore record operation log");
            return;
        }

        if (null == UserUtil.getCurrentUser()) {
            log.info("no user login,record operation log abort");
            return;
        }

        String name = UserUtil.getCurrentUser().getName();

        OperationLogModule moduleAnno = signature.getMethod().getDeclaredAnnotation(OperationLogModule.class);
        if (null == moduleAnno)
            moduleAnno = pjp.getTarget().getClass().getDeclaredAnnotation(OperationLogModule.class);

        OperationLog log = new OperationLog();
        log.setOperatorName(name);
        log.setContent(getContent(signature.getMethod().getDeclaredAnnotation(OperationLogType.class), signature.getParameterNames(), pjp.getArgs()));
        log.setType(getType(moduleAnno, request.getRequestURI()));
        log.setOperateId(getKey(signature, pjp.getArgs()).orElse(""));

        operationLogWriteService.create(log);
    }


    private Integer getType(OperationLogModule operationLogModule, String url) {

        if (null != operationLogModule)
            return operationLogModule.value().getValue();

        if (url.length() <= 5)
            return OperationLogModule.Module.UNKNOWN.getValue();

        //remove '/api/'
        String key = url.substring(5);
        key = key.substring(0, key.indexOf('/'));
        return OperationLogModule.Module.fromKey(key).getValue();
    }

    private String getContent(OperationLogType operationLogTypeAnno, String[] parameterNames, Object[] args) {

        String operationType = null;
        if (null != operationLogTypeAnno) {
            operationType = operationLogTypeAnno.value();
        } else {
            switch (request.getMethod().toUpperCase()) {
                case "PUT":
                    operationType = "新增";
                    break;
                case "POST":
                    operationType = "修改";
                    break;
                case "DELETE":
                    operationType = "删除";
                    break;
                default:
                    operationType = "未知";
            }
        }

        Map<Object, Object> content = new HashedMap();
        content.put("type", operationType);
        for (int i = 0; i < parameterNames.length; i++) {
            content.put(parameterNames[i], args[i]);
        }

        return JsonMapper.nonDefaultMapper().toJson(content);
    }


    private Optional<String> getKey(MethodSignature signature, Object[] args) {

        int pos = 0;
        for (Parameter parameter : signature.getMethod().getParameters()) {
            if (parameter.isAnnotationPresent(OperationLogKey.class)) {
                if (null == args[pos])
                    return Optional.ofNullable(null);
                else return Optional.of(args[pos].toString());
            }
            pos++;
        }

        log.info("can not find key parameter with OperationLogKey annotation,start automatic match");
        if (args.length == 1 && signature.getParameterNames()[0].toUpperCase().contains("ID")) {
            return Optional.ofNullable(null == args[0] ? null : args[0].toString());
        }

        pos = 0;
        for (String name : signature.getParameterNames()) {
            if (name.equalsIgnoreCase("id")) {
                return Optional.ofNullable(null == args[pos] ? null : args[pos].toString());
            }
            pos++;
        }
        return Optional.ofNullable(null);
    }


}
