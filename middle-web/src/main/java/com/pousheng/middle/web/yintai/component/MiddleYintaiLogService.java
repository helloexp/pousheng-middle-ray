package com.pousheng.middle.web.yintai.component;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.item.constant.ItemPushStatus;
import com.pousheng.middle.item.dao.ChannelItemPushDao;
import com.pousheng.middle.item.dao.ChannelItemPushLogDao;
import com.pousheng.middle.group.model.ChannelItemPush;
import com.pousheng.middle.group.model.ChannelItemPushLog;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.web.yintai.YintaiAttributeEnum;
import com.pousheng.middle.web.yintai.dto.RecordLog;
import com.pousheng.middle.web.yintai.dto.YintaiPushItemDTO;
import com.pousheng.middle.web.yintai.dto.YintaiPushItemLogDTO;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.item.dto.OpenClientSkuAttribute;
import io.terminus.parana.cache.SpuCacher;
import io.terminus.parana.spu.dto.FullSpu;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AUTHOR: zhangbin
 * ON: 2019/7/17
 */
@Slf4j
@Service
public class MiddleYintaiLogService {

    @Autowired
    private ChannelItemPushDao channelItemPushDao;

    @Autowired
    private ChannelItemPushLogDao channelItemPushLogDao;
    @Autowired
    private EventBus eventBus;
    @Autowired
    private SpuCacher spuCacher;

    @PostConstruct
    private void init() {
        eventBus.register(this);
    }

    /**
     * 商品同步日志历史明细
     * @param search
     * @return
     */
    public Paging<YintaiPushItemLogDTO> pagingLogDetail(Integer pageNo, Integer pageSize, YintaiPushItemLogDTO search) {
        Map<String, Object> criteria = Maps.newHashMap();
        if (!Strings.isNullOrEmpty(search.getChannel())) {
            criteria.put("channel", search.getChannel());
        }
        if (!Strings.isNullOrEmpty(search.getSpuCode())) {
            criteria.put("spuCode", search.getSpuCode());
        }
        if (!Strings.isNullOrEmpty(search.getSkuCode())) {
            criteria.put("skuCode", search.getSkuCode());
        }
        if (search.getBrandId() != null) {
            criteria.put("brandId", search.getBrandId());
        }

        PageInfo pageInfo = PageInfo.of(pageNo, pageSize);
        criteria.putAll(pageInfo.toMap());

        Paging<ChannelItemPushLog> logPaging = channelItemPushLogDao.paging(criteria);
        if (logPaging.isEmpty()) {
            return Paging.empty();
        }

        return new Paging<>(logPaging.getTotal(), logPaging.getData().stream().map(this::convert).collect(Collectors.toList()));
    }

    /**
     * 商品同步日志
     * @param search
     * @return
     */
    public Paging<YintaiPushItemLogDTO> paging(Integer pageNo, Integer pageSize, YintaiPushItemLogDTO search) {
        Map<String, Object> criteria = Maps.newHashMap();
        if (!Strings.isNullOrEmpty(search.getChannel())) {
            criteria.put("channel", search.getChannel());
        }
        if (!Strings.isNullOrEmpty(search.getSpuCode())) {
            criteria.put("spuCode", search.getSpuCode());
        }
        if (!Strings.isNullOrEmpty(search.getSkuCode())) {
            criteria.put("skuCode", search.getSkuCode());
        }
        if (search.getBrandId() != null) {
            criteria.put("brandId", search.getBrandId());
        }
        if (!Strings.isNullOrEmpty(search.getSyncStatus())) {
            criteria.put("status", Integer.valueOf(search.getSyncStatus()));
        }

        PageInfo pageInfo = PageInfo.of(pageNo, pageSize);
        criteria.putAll(pageInfo.toMap());

        Paging<ChannelItemPush> paging = channelItemPushDao.paging(criteria);
        if (paging.isEmpty()) {
            return Paging.empty();
        }

        return new Paging<>(paging.getTotal(), paging.getData().stream().map(this::convert).collect(Collectors.toList()));
    }

    public List<ChannelItemPush> findBy(String spuCode, String skuCode, Long brandId, Integer status) {
        Map<String, Object> criteria = Maps.newHashMap();
        criteria.put("spuCode", spuCode);
        criteria.put("skuCode", skuCode);
        criteria.put("brandId", brandId);
        criteria.put("status", status);
        return channelItemPushDao.list(criteria);
    }

    private YintaiPushItemLogDTO convert(ChannelItemPushLog channelItemPushLog) {
//        YintaiPushItemDTO pushItemDTO = new YintaiPushItemDTO();
//        String pushItemJson = channelItemPushLog.getExtra().get("pushItem");
//        if (Strings.isNullOrEmpty(pushItemJson)) {
//            pushItemDTO = JSON.parseObject(pushItemJson, YintaiPushItemDTO.class);
//        }
        YintaiPushItemLogDTO dto = new YintaiPushItemLogDTO();
//        dto.setSpuCode(channelItemPushLog.getSpuCode());
//        dto.setName(pushItemDTO.getName());
//        dto.setSkuCode(channelItemPushLog.getSkuCode());
//        dto.setColor(pushItemDTO.getAttrs().get(YintaiAttributeEnum.COLOR));
//        dto.setSize(pushItemDTO.getAttrs().get(YintaiAttributeEnum.SIZE));
//        dto.setPrice(pushItemDTO.getPrice());
//        Brand brand = brandCacher.findBrandById(channelItemPushLog.getBrandId());
//        if (brand != null) {
//            dto.setBrand(brand.getName());
//        }
//        dto.setOutBrand(pushItemDTO.getOutBrandId());
//        dto.setSyncStatus(ItemPushStatus.from(channelItemPushLog.getStatus()).getDesc());
//        dto.setSyncTime(channelItemPushLog.getCreatedAt());
        return dto;
    }

    private YintaiPushItemLogDTO convert(ChannelItemPush channelItemPush) {
        YintaiPushItemLogDTO dto = new YintaiPushItemLogDTO();
        dto.setSpuCode(channelItemPush.getSpuCode());
        dto.setName(channelItemPush.getSpuName());
        dto.setSkuCode(channelItemPush.getSkuCode());
        dto.setColor(channelItemPush.getColor());
        dto.setSize(channelItemPush.getSize());
        dto.setPrice(String.valueOf(channelItemPush.getPrice()));
        dto.setBrand(channelItemPush.getBrandName());
        dto.setOutBrand(channelItemPush.getChannelBrandId());
        dto.setSyncStatus(ItemPushStatus.from(channelItemPush.getStatus()).getDesc());
        dto.setSyncTime(channelItemPush.getUpdatedAt());
        return dto;
    }

    public void saveLog(ItemMapping mapping, ItemPushStatus status, String cause) {
        eventBus.post(new HandleLogEvent(mapping, status.getValue(), cause));
    }

    @Subscribe
    public void handleSaveLog(HandleLogEvent event) {
        ItemMapping item = event.getItem();
        FullSpu spu = spuCacher.findFullSpuById(item.getItemId());
        ChannelItemPushLog pushLog = new ChannelItemPushLog();
        pushLog.setChannel(MiddleChannel.YINTAI.getValue());
        pushLog.setSpuCode(spu.getSpu().getSpuCode());
        pushLog.setBrandId(spu.getSpu().getBrandId());
        pushLog.setSkuCode(item.getSkuCode());
        pushLog.setChannelItemId(item.getChannelItemId());
        pushLog.setChannelSkuId(item.getChannelSkuId());
        pushLog.setExtraJson(item.getSkuAttributesJson());
        pushLog.setStatus(event.getStatus());
        pushLog.setCause(event.getCause());
        channelItemPushLogDao.create(pushLog);

        ChannelItemPush origin = channelItemPushDao.findByChannelAndSpuAndSku(MiddleChannel.YINTAI.getValue(), spu.getSpu().getSpuCode(), item.getSkuCode());
        if (origin == null) {
            ChannelItemPush itemPush = new ChannelItemPush();
            itemPush.setBrandId(spu.getSpu().getBrandId());
            itemPush.setBrandName(spu.getSpu().getBrandName());
            itemPush.setSpuCode(spu.getSpu().getSpuCode());
            itemPush.setSpuName(spu.getSpu().getName());
            itemPush.setSkuId(null);
            itemPush.setSkuCode(item.getSkuCode());
            itemPush.setChannel(MiddleChannel.YINTAI.getValue());
            itemPush.setOpenShopId(item.getOpenShopId());
            itemPush.setOpenShopName(item.getOpenShopName());
            itemPush.setChannelItemId(item.getChannelItemId());
            itemPush.setChannelSkuId(item.getChannelSkuId());

            itemPush.setStatus(item.getStatus());
            for (OpenClientSkuAttribute skuAttribute : item.getSkuAttributes()) {
                if (YintaiAttributeEnum.from(skuAttribute.getAttributeKey()) == YintaiAttributeEnum.COLOR) {
                    itemPush.setColor(skuAttribute.getAttributeValue());
                } else if (YintaiAttributeEnum.from(skuAttribute.getAttributeKey()) == YintaiAttributeEnum.SIZE){
                    itemPush.setSize(skuAttribute.getAttributeValue());
                } else if ("price".equals(skuAttribute.getAttributeKey())) {
                    itemPush.setPrice(Integer.valueOf(skuAttribute.getAttributeValue()));
                } else if ("channelBrandId".equals(skuAttribute.getAttributeKey())) {
                    itemPush.setChannelBrandId(skuAttribute.getAttributeValue());
                }
            }
            channelItemPushDao.create(itemPush);
        } else {
            origin.setChannelItemId(item.getChannelItemId());
            origin.setChannelSkuId(item.getChannelSkuId());
            origin.setStatus(item.getStatus());
            channelItemPushDao.update(origin);
        }

    }

    public void saveErrorLog(RecordLog recordLog) {
        List<YintaiPushItemDTO> itemList = recordLog.getPushItems();
        for (YintaiPushItemDTO item : itemList) {
            ChannelItemPushLog pushLog = new ChannelItemPushLog();
            pushLog.setChannel(MiddleChannel.YINTAI.getValue());
            pushLog.setSpuCode(item.getSpuCode());
            pushLog.setBrandId(item.getBrandId());
            pushLog.setSkuCode(item.getSkuCode());
            pushLog.setChannelItemId(null);
            pushLog.setChannelSkuId(null);
            List<OpenClientSkuAttribute> openClientSkuAttributes = item.getAttrs().entrySet().stream().map(entry -> {
                OpenClientSkuAttribute attribute = new OpenClientSkuAttribute();
                attribute.setAttributeKey(entry.getKey().getValue());
                attribute.setAttributeValue(entry.getValue());
                return attribute;
            }).collect(Collectors.toList());
            pushLog.setExtraJson(JSON.toJSONString(openClientSkuAttributes));
            pushLog.setStatus(ItemPushStatus.FAIL.getValue());
            pushLog.setCause(recordLog.getCause());
            channelItemPushLogDao.create(pushLog);

            ChannelItemPush origin = channelItemPushDao.findByChannelAndSpuAndSku(MiddleChannel.YINTAI.getValue(), item.getSpuCode(), item.getSkuCode());
            if (origin == null) {
                ChannelItemPush itemPush = new ChannelItemPush();
                itemPush.setBrandId(item.getBrandId());
                itemPush.setBrandName(item.getBrandName());
                itemPush.setSpuCode(item.getSpuCode());
                itemPush.setSpuName(item.getName());
                itemPush.setSkuId(item.getSkuId());
                itemPush.setSkuCode(item.getSkuCode());
                itemPush.setChannel(MiddleChannel.YINTAI.getValue());
                itemPush.setOpenShopId(null);
                itemPush.setOpenShopName(null);
                itemPush.setChannelItemId(null);
                itemPush.setChannelSkuId(null);
                itemPush.setStatus(ItemPushStatus.FAIL.getValue());
                for (Map.Entry<YintaiAttributeEnum, String> entry : item.getAttrs().entrySet()) {
                    if (entry.getKey() == YintaiAttributeEnum.COLOR) {
                        itemPush.setColor(entry.getValue());
                    } else if (entry.getKey() == YintaiAttributeEnum.SIZE){
                        itemPush.setSize(entry.getValue());
                    }
                }
                itemPush.setChannelBrandId(item.getOutBrandId());
                try {
                    itemPush.setPrice(Integer.valueOf(item.getPrice()));
                } catch (NumberFormatException e) {
                    //
                }
                channelItemPushDao.create(itemPush);
            } else {
                origin.setStatus(ItemPushStatus.FAIL.getValue());
                origin.setChannelBrandId(item.getOutBrandId());
                origin.setBrandId(item.getBrandId());
                origin.setBrandName(item.getBrandName());
                origin.setSpuCode(item.getSpuCode());
                origin.setSpuName(item.getName());
                channelItemPushDao.update(origin);
            }
        }

    }

    @Data
    public class HandleLogEvent {
        private ItemMapping item;

        private Integer status;

        private String cause;

        public HandleLogEvent(ItemMapping item, Integer status, String cause) {
            this.item = item;
            this.status = status;
            this.cause = cause;
        }
    }
}
