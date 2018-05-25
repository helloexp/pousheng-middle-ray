package com.pousheng.middle.web;

import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.pousheng.auth.AuthConfiguration;
import com.pousheng.erp.ErpConfiguration;
import com.pousheng.middle.PoushengMiddleItemConfiguration;
import com.pousheng.middle.gd.GDMapToken;
import com.pousheng.middle.interceptors.LoginInterceptor;
import com.pousheng.middle.open.PsPersistedOrderMaker;
import com.pousheng.middle.open.erp.ErpOpenApiToken;
import com.pousheng.middle.order.dispatch.component.DispatchOrderChain;
import com.pousheng.middle.order.dispatch.link.*;
import com.pousheng.middle.schedule.jobs.BatchConfig;
import com.pousheng.middle.schedule.jobs.BatchJobProperties;
import com.pousheng.middle.schedule.jobs.TaskConfig;
import com.pousheng.middle.web.converters.PoushengJsonMessageConverter;
import com.pousheng.middle.web.item.PoushengPipelineConfigurer;
import io.terminus.open.client.center.OpenClientCenterAutoConfig;
import io.terminus.open.client.parana.ParanaAutoConfiguration;
import io.terminus.parana.ItemApiConfiguration;
import io.terminus.parana.TradeApiConfig;
import io.terminus.parana.TradeAutoConfig;
import io.terminus.parana.auth.AuthApiConfiguration;
import io.terminus.parana.cache.BackCategoryCacher;
import io.terminus.parana.cache.CategoryAttributeCacher;
import io.terminus.parana.cache.SpuCacher;
import io.terminus.parana.component.attribute.CategoryAttributeNoCacher;
import io.terminus.parana.order.api.AbstractPersistedOrderMaker;
import io.terminus.parana.order.api.DeliveryFeeCharger;
import io.terminus.parana.order.dto.RichSkusByShop;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.rule.RuleExecutorRegistry;
import io.terminus.parana.user.ext.DefaultUserTypeBean;
import io.terminus.parana.user.ext.UserTypeBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.MultipartAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.DateFormatter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;

import javax.servlet.MultipartConfigElement;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Description: add something here
 * MiddleUser: xiao
 * Date: 28/04/2017
 */
@Configuration
@Import({PoushengMiddleItemConfiguration.class,
        ErpConfiguration.class,
        ItemApiConfiguration.class,
        TradeApiConfig.class,
        TradeAutoConfig.class,
        ParanaAutoConfiguration.class,
        AuthConfiguration.class,
        AuthApiConfiguration.class,
        OpenClientCenterAutoConfig.class,
        BatchConfig.class,
        TaskConfig.class,
        MultipartAutoConfiguration.class,
        BizBeanConfiguration.class})

@ComponentScan(
        {"com.pousheng.middle.order",
                "com.pousheng.middle.warehouse",
                "com.pousheng.middle.open",
                "com.pousheng.middle.advices",
                "com.pousheng.middle.auth",
                "com.pousheng.middle.erpsyc",
                "com.pousheng.middle.hksyc",
                "com.pousheng.middle.yyedisyc",
                "com.pousheng.middle.interceptors",
                "com.pousheng.middle.web",
                "com.pousheng.middle.gd"})
@EnableScheduling
@EnableAutoConfiguration
@EnableConfigurationProperties({
        ErpOpenApiToken.class, GDMapToken.class, BatchJobProperties.class
})
@Slf4j
public class MiddleConfiguration extends WebMvcConfigurerAdapter {


    @Autowired
    private LoginInterceptor loginInterceptor;


    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor);
    }


    /**
     * 中台不需要计算运费
     *
     * @return deliveryFeeCharger
     */
    @Bean
    public DeliveryFeeCharger deliveryFeeCharger() {
        return new DeliveryFeeCharger() {
            @Override
            public Integer charge(Long aLong, Integer integer, Integer integer1) {
                return 0;
            }

            @Override
            public Map<Long, Integer> charge(List<RichSkusByShop> list, ReceiverInfo receiverInfo) {
                return Collections.emptyMap();
            }
        };
    }

    /**
     * 文件上传配置
     * @return
     */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        //文件最大
        factory.setMaxFileSize("20480KB"); //KB,MB
        /// 设置总上传数据总大小
        factory.setMaxRequestSize("204800KB");
        return factory.createMultipartConfig();
    }

    @Bean
    public PoushengPipelineConfigurer pipelineConfigurer(BackCategoryCacher backCategoryCacher,
                                                         SpuCacher spuCacher,
                                                         CategoryAttributeCacher categoryAttributeCacher,
                                                         CategoryAttributeNoCacher categoryAttributeNoCacher,
                                                         RuleExecutorRegistry ruleExecutorRegistry) {
        PoushengPipelineConfigurer poushengPipelineConfigurer = new PoushengPipelineConfigurer(backCategoryCacher,
                categoryAttributeNoCacher);
        poushengPipelineConfigurer.configureRuleExecutors(ruleExecutorRegistry);
        return poushengPipelineConfigurer;
    }


    /*@Bean
    public DispatchOrderChain dispatchOrderChain(AppointShopDispatchLink appointShopDispatchLink, AllShopDispatchlink allShopDispatchlink,
                                                 AllWarehouseDispatchLink allWarehouseDispatchLink, OnlineSaleWarehouseDispatchLink onlineSaleWarehouseDispatchLink,
                                                 ProvinceInnerShopDispatchlink provinceInnerShopDispatchlink,ProvinceInnerWarehouseDispatchLink provinceInnerWarehouseDispatchLink,
                                                 ShopOrWarehouseDispatchlink shopOrWarehouseDispatchlink){
        DispatchOrderChain dispatchOrderChain = new DispatchOrderChain();
        List<DispatchOrderLink> dispatchOrderLinks = Lists.newArrayList();
        dispatchOrderLinks.add(appointWarehouseDispatchLink);
        dispatchOrderLinks.add(appointShopDispatchLink);
        dispatchOrderLinks.add(onlineSaleWarehouseDispatchLink);
        dispatchOrderLinks.add(provinceInnerWarehouseDispatchLink);
        dispatchOrderLinks.add(allWarehouseDispatchLink);
        dispatchOrderLinks.add(provinceInnerShopDispatchlink);
        dispatchOrderLinks.add(allShopDispatchlink);
        dispatchOrderLinks.add(allShopDispatchlink);
        dispatchOrderLinks.add(shopOrWarehouseDispatchlink);
        dispatchOrderChain.setDispatchOrderLinks(dispatchOrderLinks);
        return dispatchOrderChain;
    }*/


    @Bean
    public DispatchOrderChain dispatchOrderChain(AppointWarehouseDispatchLink appointWarehouseDispatchLink,AppointShopDispatchLink appointShopDispatchLink, ShopWarehouseDispatchLink shopWarehouseDispatchLink,
                                                 TotalWarehouseDispatchLink totalWarehouseDispatchLink,ShopOrWarehouseDispatchlink shopOrWarehouseDispatchlink){
        DispatchOrderChain dispatchOrderChain = new DispatchOrderChain();
        List<DispatchOrderLink> dispatchOrderLinks = Lists.newArrayList();
        dispatchOrderLinks.add(appointWarehouseDispatchLink);
        dispatchOrderLinks.add(appointShopDispatchLink);
        dispatchOrderLinks.add(totalWarehouseDispatchLink);
        dispatchOrderLinks.add(shopWarehouseDispatchLink);
        dispatchOrderLinks.add(shopOrWarehouseDispatchlink);
        dispatchOrderChain.setDispatchOrderLinks(dispatchOrderLinks);
        return dispatchOrderChain;
    }


    @Bean
    public EventBus eventBus() {
        return new AsyncEventBus(
                new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors()*5, Runtime.getRuntime().availableProcessors() * 8, 5, TimeUnit.MINUTES,
                        new ArrayBlockingQueue<>(100000), (new ThreadFactoryBuilder()).setNameFormat("event-bus-%d").build(),
                        new RejectedExecutionHandler() {
                            @Override
                            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                                log.error("event {} is rejected", r);
                            }
                        }));
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatter(new DateFormatter("yyyy-MM-dd HH:mm:ss"));
        registry.addFormatter(new DateFormatter("yyyy-MM-dd"));
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        Iterables.removeIf(converters, new Predicate<HttpMessageConverter<?>>() {
            @Override
            public boolean apply(HttpMessageConverter<?> input) {
                if (input instanceof AbstractJackson2HttpMessageConverter) {
                    return true;
                }
                if (input instanceof StringHttpMessageConverter) {
                    return true;
                }
                return false;
            }
        });

        //force utf-8 encode
        StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter(Charsets.UTF_8);
        stringHttpMessageConverter.setSupportedMediaTypes(Lists.newArrayList(MediaType.TEXT_PLAIN, MediaType.ALL));
        converters.add(1, stringHttpMessageConverter);

        final PoushengJsonMessageConverter paranaJsonMessageConverter = new PoushengJsonMessageConverter();
        //paranaJsonMessageConverter.setObjectMapper(JsonMapper.nonEmptyMapper().getMapper());
        converters.add(paranaJsonMessageConverter);
    }

    @Bean
    public LocaleResolver localeResolver() {
        SessionLocaleResolver localeResolver = new SessionLocaleResolver();
        localeResolver.setDefaultLocale(Locale.CHINA);
        return localeResolver;
    }

    @Bean
    public AbstractPersistedOrderMaker orderMaker(){
        return new PsPersistedOrderMaker(OrderLevel.SHOP,OrderLevel.SHOP);
    }
    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("middle_messages", "messages");
        messageSource.setDefaultEncoding("UTF-8");
        messageSource.setFallbackToSystemLocale(true);
        messageSource.setCacheSeconds(-1);
        return messageSource;
    }

    @Bean
    public UserTypeBean userTypeBean() {
        return new DefaultUserTypeBean();
    }



}
