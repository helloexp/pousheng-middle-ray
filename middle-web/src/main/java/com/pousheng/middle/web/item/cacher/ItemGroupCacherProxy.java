package com.pousheng.middle.web.item.cacher;

import com.pousheng.middle.group.model.ItemGroup;
import com.pousheng.middle.group.service.ItemGroupReadService;
import com.pousheng.middle.group.service.ItemGroupWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
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
        return itemGroupWriteService.update(group);
    }
}
