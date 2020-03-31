package com.pousheng.middle;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockitoPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created with IntelliJ IDEA
 * User: yujiacheng
 * Date: 2018/5/12
 * Time: 下午1:38
 */
@Slf4j
@SuppressWarnings("all")
public abstract class AbstractRestApiTest {


    protected AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
    List<Class<?>> beanList = Lists.newArrayList(PropertyPlaceholderAutoConfiguration.class);
    @Rule
    public ExpectedException thrown = ExpectedException.none();


    protected void addBean(Class<?> clazz) {
        ctx.register(clazz);
    }

    protected abstract Class<?> mockitoBeans();

    @Before
    public void setUp() throws Exception {
        injectYmlProperties(ctx);
        beanList.forEach(ctx::register);
        MockitoPostProcessor.register(ctx);
        ctx.register(mockitoBeans());
        ctx.refresh();
        init();
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

    private static void injectYmlProperties(AnnotationConfigApplicationContext context) {
        YamlPropertiesFactoryBean factory = new YamlPropertiesFactoryBean();
        factory.setResources(new ClassPathResource("application-webtest.yml"));
        context.getEnvironment().getPropertySources()
                .addLast(new PropertiesPropertySource("ymlProperties", factory.getObject()));
    }

    protected <T> T get(Class<T> t) {
        return ctx.getBean(t);
    }

    protected String read(String resourceName) {
        try {
            Stream<String> stream = Files.lines(Paths.get(ClassLoader.getSystemResource(resourceName).toURI()));
            return stream.map(Object::toString).collect(Collectors.joining());

        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}

