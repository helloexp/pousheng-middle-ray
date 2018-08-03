package com.pousheng.middle.web.mq.group;

import com.pousheng.middle.web.item.cacher.ItemGroupCacher;
import com.pousheng.middle.web.item.cacher.ShopGroupRuleCacher;
import com.pousheng.middle.web.item.cacher.WarehouseGroupRuleCacher;
import io.terminus.common.rocketmq.annotation.ConsumeMode;
import io.terminus.common.rocketmq.annotation.MQConsumer;
import io.terminus.common.rocketmq.annotation.MQSubscribe;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author zhaoxw
 * @date 2018/8/1
 */

@Component
@Slf4j
@MQConsumer
public class CacherChangeConsumer {

    @Autowired
    private ItemGroupCacher itemGroupCacher;

    @Autowired
    private ShopGroupRuleCacher  shopGroupRuleCacher;

    @Autowired
    private WarehouseGroupRuleCacher warehouseGroupRuleCacher;

    @MQSubscribe(topic = "cacherClearTopic", consumerGroup = "cacherGroup",
            consumeMode = ConsumeMode.CONCURRENTLY)
    public void clear(Integer carcherId) {
        CacherName cacher = CacherName.from(carcherId);
        switch (cacher) {
            case GROUP_RULE:
                log.info("start to clear item group rule cacher");
                shopGroupRuleCacher.refreshAll();
                warehouseGroupRuleCacher.refreshAll();
                break;
            case ITEM_GROUP:
                log.info("start to clear item group cacher");
                itemGroupCacher.refreshAll();
                break;
            default:
                log.warn("illegal cacher id");
                break;
        }

    }
}
