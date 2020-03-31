package com.pousheng.middle.web.yintai.mq;

import com.alibaba.fastjson.JSON;
import com.pousheng.middle.mq.producer.RocketMqProducerService;
import com.pousheng.middle.web.yintai.YintaiConstant;
import com.pousheng.middle.web.yintai.dto.MessageDTO;
import io.terminus.open.client.common.mappings.model.BrandMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * AUTHOR: zhangbin
 * ON: 2019/7/3
 */
@Slf4j
@Component
public class YintaiMessageProducer {

    @Autowired
    private RocketMqProducerService mqProducerService;

    public void sendItemPush(List<BrandMapping> brands, Date startTime) {
        brands.forEach(brandMapping -> {
            MessageDTO messageDTO = new MessageDTO();
            messageDTO.setBrandMapping(brandMapping);
            messageDTO.setStart(startTime);
            mqProducerService.sendMessage(YintaiConstant.YINTAI_ITEM_PUSH_TOPIC, JSON.toJSONString(messageDTO));
        });
    }

    public void sendItemPush(String skuCode) {
        MessageDTO messageDTO = new MessageDTO();
        messageDTO.setSkuCode(skuCode);
        mqProducerService.sendMessage(YintaiConstant.YINTAI_ITEM_PUSH_TOPIC, JSON.toJSONString(messageDTO));
    }
}
