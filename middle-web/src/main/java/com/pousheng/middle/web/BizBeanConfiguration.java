package com.pousheng.middle.web;

import com.pousheng.middle.web.biz.CompensateBizRegistryCenter;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/6/12
 * pousheng-middle
 */
@Configuration
public class BizBeanConfiguration {
    @Autowired
    private ApplicationContext applicationContext;

    @Bean(name = "pousheng-compensate-biz-registry-center-bean-processor")
    public BeanPostProcessor beanPostProcessorForPoushengCompensateBiz() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                if (!(bean instanceof CompensateBizRegistryCenter)) {
                    return bean;
                }
                CompensateBizRegistryCenter registryCenter = (CompensateBizRegistryCenter) bean;
                Map<String, Object> beanMap = applicationContext.getBeansWithAnnotation(CompensateAnnotation.class);
                for (Object service : beanMap.values()) {
                    if (service instanceof CompensateBizService) {
                        CompensateAnnotation annotation = service.getClass().getAnnotation(CompensateAnnotation.class);
                        registryCenter.register(annotation.bizType().name(), (CompensateBizService) service);
                    }
                }
                return registryCenter;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                return bean;
            }
        };
    }
}
