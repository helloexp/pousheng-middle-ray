package com.pousheng.middle.web.express.esp;

import com.alibaba.fastjson.JSONObject;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.pousheng.middle.order.model.PoushengConfig;
import com.pousheng.middle.order.service.PoushengConfigService;
import com.pousheng.middle.web.express.ExpressConfigType;
import com.pousheng.middle.web.express.esp.bean.ExpressCodeMapping;
import io.terminus.common.model.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/9/11
 */
@Service
public class ConfigCacheService {

    @Autowired
    private PoushengConfigService poushengConfigService;

    //门店列表缓存
    public static final Set<String> storeList = Sets.newHashSet();
    //快递映射缓存
    public static final Map<String, ExpressCodeMapping> expressCodeMap = Maps.newHashMap();

    private LoadingCache<String, String> configCache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build(CacheLoader.from(this::loadCache));

    /**
     * 加载缓存
     *
     * @param key
     * @return
     */
    private String loadCache(String key) {
        Response<PoushengConfig> poushengConfigByTypeResponse = poushengConfigService.findPoushengConfigByType(key);
        if (poushengConfigByTypeResponse.isSuccess()) {
            PoushengConfig config = poushengConfigByTypeResponse.getResult();
            String content = config.getContent();

            //初始化门店列表缓存
            if (ExpressConfigType.store_list_ESP.equals(key)) {
                List<String> storeList = JSONObject.parseArray(content, String.class);
                storeList.clear();
                storeList.addAll(storeList);
            }

            //初始化快递映射缓存
            if (ExpressConfigType.express_mapping.equals(key)) {
                List<ExpressCodeMapping> mappings = JSONObject.parseArray(content, ExpressCodeMapping.class);
                if (mappings.size() > 0) {
                    expressCodeMap.clear();
                    for (ExpressCodeMapping esm : mappings) {
                        expressCodeMap.put(esm.getMiddleExpressCode(), esm);
                    }
                }
            }

            return content;
        }
        return null;
    }

    /**
     * 清理缓存
     *
     * @return
     */
    public boolean cleanCache() {
        configCache.cleanUp();
        storeList.clear();
        expressCodeMap.clear();
        return true;
    }

    public String getUnchecked(String key) {
        return configCache.getUnchecked(key);
    }


}
