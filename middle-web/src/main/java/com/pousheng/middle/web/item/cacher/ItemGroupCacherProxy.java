package com.pousheng.middle.web.item.cacher;

import com.pousheng.middle.group.model.ItemGroup;
import com.pousheng.middle.group.service.ItemGroupReadService;
import com.pousheng.middle.group.service.ItemGroupWriteService;
import com.pousheng.middle.web.mq.group.CacherName;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.rocketmq.core.TerminusMQProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/**
 * @author zhaoxw
 * @date 2018/5/14
 */
@Slf4j
@Component
@CacheConfig(cacheNames = "ItemGroup")
public class ItemGroupCacherProxy {

    @RpcConsumer
    private ItemGroupReadService itemGroupReadService;

    @RpcConsumer
    private ItemGroupWriteService itemGroupWriteService;

    @Autowired
    private TerminusMQProducer producer;

    @Value("${terminus.rocketmq.cacherClearTopic}")
    private String cacherClearTopic;

    /**
     * 获取分组信息
     */
    @Cacheable(key = "'group#findById:'.concat(#id.hashCode())")
    public ItemGroup findById(Long id) {
        Response<ItemGroup> resp = itemGroupReadService.findById(id);
        if (!resp.isSuccess()) {
            log.error("failed to find rule (shopId={}, error:{}",
                    id, resp.getError());
            throw new ServiceException("item.group.find.fail");
        }
        return resp.getResult();
    }


    @CacheEvict(key = "'group#findById:'.concat(#group.getId().hashCode())")
    public Response<Boolean> update(ItemGroup group) {
        producer.send(cacherClearTopic, CacherName.ITEM_GROUP.value());
        return itemGroupWriteService.update(group);
    }
}
