package com.pousheng.middle.web.shop.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by songrenfei on 2018/4/1
 */
@Slf4j
@Component
public class MiddleOpenShopCacher {

    private LoadingCache<String, Map<String, OpenShop>> openShopCacher;

    @Autowired
    private OpenShopReadService openShopReadService;

    @Value("${cache.duration.in.minutes: 30}")
    private Integer duration;

    private static final String KEY = "all-open-shop";

    private static String MPOS = "mpos-";

    private static String YJ = "yj-";


    @PostConstruct
    public void init() {
        this.openShopCacher = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES)
                .build(new CacheLoader<String, Map<String, OpenShop>>() {
                    @Override
                    public Map<String, OpenShop> load(String key) throws Exception {
                        Map<String, OpenShop> map = new HashMap<>();
                        Response<List<OpenShop>> response = openShopReadService.findAll();
                        if (!response.isSuccess()) {
                            throw new JsonResponseException(response.getError());
                        }
                        List<OpenShop> list = response.getResult();
                        for (OpenShop shop : list) {
                            if (shop.getShopName().startsWith(MPOS) || shop.getShopName().startsWith(YJ)) {
                                map.put(shop.getAppKey(), shop);
                            } else {
                                if(StringUtils.isEmpty(shop.getExtra().get("companyCode"))||StringUtils.isEmpty( shop.getExtra().get("hkPerformanceShopOutCode"))){
                                    log.warn("open shop id {} info is not complete ",shop.getId());
                                    continue;
                                }
                                String appKey = shop.getExtra().get("companyCode") + "-" + shop.getExtra().get("hkPerformanceShopOutCode");
                                map.put(appKey, shop);
                            }
                        }
                        return map;
                    }
                });
    }

    public OpenShop findByAppKey(String appKey) {
        return openShopCacher.getUnchecked(KEY).get(appKey);

    }

}
