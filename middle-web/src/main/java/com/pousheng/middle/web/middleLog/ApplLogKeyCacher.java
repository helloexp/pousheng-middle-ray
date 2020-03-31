package com.pousheng.middle.web.middleLog;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.terminus.applog.core.criteria.strategy.UniqueKeyQueryStrategy;
import io.terminus.applog.core.model.MemberAppLogKey;
import io.terminus.applog.core.service.MemberAppLogKeyReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * @author zhaoxw
 * @date 2018/8/24
 */
@Slf4j
@Component
public class ApplLogKeyCacher {

    private LoadingCache<String, MemberAppLogKey> keyCacher;

    private LoadingCache<Long, MemberAppLogKey> cacher;

    @Value("${cache.duration.in.minutes: 60}")
    private Integer duration;

    @RpcConsumer
    private MemberAppLogKeyReadService memberAppLogKeyReadService;

    private static String DESCRIPTION = "description";

    @PostConstruct
    public void init() {
        this.keyCacher = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build(new CacheLoader<String, MemberAppLogKey>() {
                    @Override
                    public MemberAppLogKey load(String description) {
                        UniqueKeyQueryStrategy uqs = UniqueKeyQueryStrategy.create();
                        uqs.addUniqueKey(DESCRIPTION);
                        Response<MemberAppLogKey> resp = memberAppLogKeyReadService.findByUniqueKey(description, uqs);
                        if (!resp.isSuccess()) {
                            log.error("failed to find log key{},cause{}", description, resp.getError());
                            return null;
                        }
                        return resp.getResult();
                    }
                });
        this.cacher = CacheBuilder.newBuilder()
                .expireAfterWrite(duration, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build(new CacheLoader<Long, MemberAppLogKey>() {
                    @Override
                    public MemberAppLogKey load(Long id) {
                        Response<MemberAppLogKey> resp = memberAppLogKeyReadService.findById(id);
                        if (!resp.isSuccess()) {
                            log.error("failed to find log id {},cause{}", id, resp.getError());
                            return null;
                        }
                        return resp.getResult();
                    }
                });
    }

    /**
     * 根据名称搜索
     *
     * @param descreption
     * @return
     */
    public MemberAppLogKey findByDescription(String descreption) {
        try {
            return keyCacher.get(descreption);
        } catch (Exception e) {
            Throwables.propagateIfPossible(e, ServiceException.class);
            log.error("failed to find key by descreption({}), cause:{}", descreption, Throwables.getStackTraceAsString(e));
            throw new ServiceException(e);
        }
    }

    public MemberAppLogKey findById(Long id) {
        try {
            return cacher.get(id);
        } catch (Exception e) {
            Throwables.propagateIfPossible(e, ServiceException.class);
            log.error("failed to find key by id({}), cause:{}", id, Throwables.getStackTraceAsString(e));
            throw new ServiceException(e);
        }
    }
}
