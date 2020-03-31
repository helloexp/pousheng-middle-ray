package com.pousheng.middle.item.dao;

import com.google.common.collect.Maps;
import com.pousheng.middle.group.model.ChannelItemPush;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * AUTHOR: zhangbin
 * ON: 2019/7/17
 */
@Repository
public class ChannelItemPushDao extends MyBatisDao<ChannelItemPush> {

    public ChannelItemPush findByChannelAndSpuAndSku(String channel, String spuCode, String skuCode) {
        Map<String, String> criteria = Maps.newHashMap();
        criteria.put("channel", channel);
        criteria.put("spuCode", spuCode);
        criteria.put("skuCode", skuCode);
        return sqlSession.selectOne(sqlId("findByChannelAndSpuAndSku"), criteria);
    }
}
