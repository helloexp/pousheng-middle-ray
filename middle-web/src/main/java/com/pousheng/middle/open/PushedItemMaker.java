package com.pousheng.middle.open;

import io.terminus.open.client.center.item.api.ParanaFullItemMaker;
import io.terminus.open.client.center.item.dto.ParanaFullItem;
import org.springframework.stereotype.Component;

/**
 * Created by cp on 6/19/17.
 */
@Component
public class PushedItemMaker implements ParanaFullItemMaker {

    @Override
    public ParanaFullItem make(Long spuId) {
        return null;
    }
}
