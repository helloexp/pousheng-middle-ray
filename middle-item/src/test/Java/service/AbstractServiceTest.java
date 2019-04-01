package service;

import org.junit.Before;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.test.mock.mockito.MockitoPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.ClassPathResource;

/**
 * Description: 抽象服务测试
 * User: xiao
 * Date: 29/09/2017
 */
@SuppressWarnings("all")
public abstract class AbstractServiceTest {

    protected AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

    protected void addBean(Class<?> clazz) {
        ctx.register(clazz);
    }

    protected void init() {
    }

    protected abstract Class<?> mockitoBeans();


    @Before
    public void setUp() throws Exception {
        injectYmlProperties(ctx, "application-test.yml");
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
