package com.pousheng.middle.order.impl.service;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockitoPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created with IntelliJ IDEA
 * User: xiehong
 * Date: 2018/5/29
 * Time: 上午11:30
 */
@SuppressWarnings("all")
@Slf4j
public abstract class AbstractServiceTest {

    protected AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

    List<Class<?>> beanList = Lists.newArrayList(
            PropertyPlaceholderAutoConfiguration.class);


    protected void addBean(Class<?> clazz) {
        ctx.register(clazz);
    }

    protected void init() throws InvocationTargetException, IllegalAccessException {
        for (Method method : this.getClass().getMethods()) {
            try {
                if (method.getName().startsWith("set") && method.getParameterTypes().length == 1) {
                    if (ctx.getBean(method.getParameterTypes()[0]) != null) {
                        method.invoke(this, ctx.getBean(method.getParameterTypes()[0]));
                        log.info("Inject {} with {}", method.getName(), ctx.getBean(method.getParameterTypes()[0]));
                    }
                }
            } catch (BeansException e) {
                // ignore
            }
        }
    }

    protected abstract Class<?> mockitoBeans();


    @Before
    public void setUp() throws Exception {
        injectYmlProperties(ctx, "application-test.yml");
        beanList.forEach(ctx::register);
        MockitoPostProcessor.register(ctx);
        ctx.register(mockitoBeans());
        ctx.refresh();

        init();
    }

    public static void injectYmlProperties(AnnotationConfigApplicationContext context, String yml) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource(yml));
        context.getEnvironment().getPropertySources()
                .addLast(new PropertiesPropertySource("ymlProperties", factory.getObject()));
    }


    protected <T> T get(Class<T> t) {
        return ctx.getBean(t);
    }
}
