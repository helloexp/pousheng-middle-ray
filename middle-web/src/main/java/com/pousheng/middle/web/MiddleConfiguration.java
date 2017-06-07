package com.pousheng.middle.web;

import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.pousheng.erp.ErpConfiguration;
import com.pousheng.erp.component.SpuInfoFetcher;
import com.pousheng.erp.model.PoushengMaterial;
import com.pousheng.middle.PoushengMiddleItemConfiguration;
import com.pousheng.middle.web.converters.PoushengJsonMessageConverter;
import io.terminus.open.client.MappingConfiguration;
import io.terminus.open.client.parana.ParanaAutoConfiguration;
import io.terminus.parana.ItemApiConfiguration;
import io.terminus.parana.ItemAutoConfig;
import io.terminus.parana.TradeApiConfig;
import io.terminus.parana.TradeAutoConfig;
import io.terminus.parana.order.api.DeliveryFeeCharger;
import io.terminus.parana.order.dto.RichSkusByShop;
import io.terminus.parana.order.model.ReceiverInfo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Description: add something here
 * User: xiao
 * Date: 28/04/2017
 */
@Configuration
@Import({PoushengMiddleItemConfiguration.class,
        ErpConfiguration.class,
        ItemApiConfiguration.class,
        TradeApiConfig.class,
        TradeAutoConfig.class,
        MappingConfiguration.class,
        ParanaAutoConfiguration.class})
@EnableScheduling
public class MiddleConfiguration extends WebMvcConfigurerAdapter {

    @Bean
    public SpuInfoFetcher<PoushengMaterial> spuInfoFetcher(){
        return new SpuInfoFetcher<PoushengMaterial>() {
            public List<PoushengMaterial> fetch(int pageNo, int pageSize, Date start, Date end) {
                //todo: implement me
                return null;
            }
        };
    }

    /**
     * 中台不需要计算运费
     * @return deliveryFeeCharger
     */
    @Bean
    public DeliveryFeeCharger deliveryFeeCharger(){
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

}
