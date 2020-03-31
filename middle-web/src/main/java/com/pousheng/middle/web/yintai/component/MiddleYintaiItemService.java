package com.pousheng.middle.web.yintai.component;

import com.alibaba.fastjson.JSON;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.pousheng.middle.enums.OpenShopEnum;
import com.pousheng.middle.item.constant.ItemPushStatus;
import com.pousheng.middle.item.dao.SpuExtDao;
import com.pousheng.middle.item.impl.dao.SkuTemplateExtDao;
import com.pousheng.middle.open.stock.StockPusherLogic;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.warehouse.companent.WarehouseRulesClient;
import com.pousheng.middle.warehouse.companent.WarehouseShopRuleClient;
import com.pousheng.middle.warehouse.dto.ShopStockRuleDto;
import com.pousheng.middle.web.mq.warehouse.InventoryChangeProducer;
import com.pousheng.middle.web.mq.warehouse.model.InventoryChangeDTO;
import com.pousheng.middle.web.yintai.YintaiAttributeEnum;
import com.pousheng.middle.web.yintai.dto.RecordLog;
import com.pousheng.middle.web.yintai.dto.YintaiBrand;
import com.pousheng.middle.web.yintai.dto.YintaiPushItemDTO;
import com.pousheng.middle.web.yintai.mq.YintaiMessageProducer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.ItemServiceRegistryCenter;
import io.terminus.open.client.center.MappingServiceRegistryCenter;
import io.terminus.open.client.common.mappings.model.BrandMapping;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.OpenClientBrandMappingService;
import io.terminus.open.client.common.mappings.service.OpenClientItemMappingService;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.item.dto.OpenClientItemYintai;
import io.terminus.open.client.item.dto.OpenClientSkuAttribute;
import io.terminus.open.client.item.dto.OpenClientSkuYintai;
import io.terminus.open.client.item.service.OpenClientItemService;
import io.terminus.open.client.tyintai.utils.YintaiOutIdBuilder;
import io.terminus.parana.attribute.dto.GroupedOtherAttribute;
import io.terminus.parana.brand.model.Brand;
import io.terminus.parana.cache.BackCategoryCacher;
import io.terminus.parana.cache.BrandCacher;
import io.terminus.parana.category.model.BackCategory;
import io.terminus.parana.spu.impl.dao.SpuAttributeDao;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.model.SpuAttribute;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.assertj.core.util.Strings;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * AUTHOR: zhangbin
 * ON: 2019/7/3
 */
@Slf4j
@Service
public class MiddleYintaiItemService {

    @Autowired
    private BackCategoryCacher backCategoryCacher;
    @Autowired
    private SpuExtDao spuDao;
    @Autowired
    private SkuTemplateExtDao skuTemplateDao;
    @Autowired
    private SpuAttributeDao spuAttributeDao;
    @Autowired
    private WarehouseRulesClient warehouseRulesClient;
    @Autowired
    private OpenShopReadService openShopReadService;
    @Autowired
    private StockPusherLogic stockPusherLogic;
    @Autowired
    private WarehouseShopRuleClient warehouseShopRuleClient;
    @Autowired
    private MappingServiceRegistryCenter mappingCenter;
    @Autowired
    private YintaiMessageProducer yintaiMessageProducer;
    @Autowired
    private ItemServiceRegistryCenter itemServiceRegistryCenter;
    @Autowired
    private MiddleYintaiLogService middleYintaiLogService;
    @Autowired
    private BrandCacher brandCacher;
    @Autowired
    private InventoryChangeProducer inventoryChangeProducer;
    @Autowired
    private MiddleYintaiBrandService middleYintaiBrandService;

    public ExecutorService executorService = new ThreadPoolExecutor(
            10, 10, 0, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200),
            new ThreadFactoryBuilder().setNameFormat("yintai-item-upload-%d").build(),
            (r, executor) -> log.error("yintai-item-push-task-rejected "));
    /**
     * 获取推送商品列表
     * @param brandId
     * @param startTime 开始时间范围
     * @return
     */
    public List<YintaiPushItemDTO> findPushItems(Long brandId, Date startTime) {
        List<Spu> spus = spuDao.findByBrandId(brandId);

        if (CollectionUtils.isEmpty(spus)) {
            log.info("spus is empty  brandId:({})", brandId);
            return Collections.emptyList();
        }

        OpenClientBrandMappingService brandService = mappingCenter.getBrandService(MiddleChannel.YINTAI.getValue());
        Response<Map<String, BrandMapping>> brandMappingResp = brandService.findBrandMapping(brandId);
        if (!brandMappingResp.isSuccess() || !brandMappingResp.getResult().containsKey(MiddleChannel.YINTAI.getValue())) {
            log.info("brandMapping is empty  brandId:({})", brandId);
            return Collections.emptyList();
        }
        BrandMapping brandMapping = brandMappingResp.getResult().get(MiddleChannel.YINTAI.getValue());
        if (Strings.isNullOrEmpty(brandMapping.getChannelBrandId())) {
            log.info("ChannelBrandId is null  brandId:({})", brandId);
            return Collections.emptyList();
        }

        List<YintaiPushItemDTO> result = Lists.newCopyOnWriteArrayList();

        List<List<Spu>> spuPartition = Lists.partition(spus, 200);
        spuPartition.forEach(partition-> {
            List<Long> spuIds = partition.stream().map(Spu::getId).collect(Collectors.toList());
            List<SkuTemplate> skuTemplates = skuTemplateDao.findBySpuIds(spuIds, startTime);
            if (CollectionUtils.isEmpty(skuTemplates)) {
                return;
            }
            log.info("greater than starttime size({}), brandId:({}), startTime:({})",skuTemplates.size(), brandId, startTime);
            //过滤
//            skuTemplates = getGreaterThanStartTime(skuTemplates, startTime);
            skuTemplates = getNeverPushToYintai(skuTemplates,spuIds);
            skuTemplates = getAllShopInventoryGreaterThanZero(skuTemplates);
            if (skuTemplates.isEmpty()) {
                log.info("findPushItems part is empty. brandId:({}), startTime:({})", brandId, startTime);
                return;
            }
            log.info("findPushItems part has data size({}). brandId:({}), startTime:({})",skuTemplates.size(), brandId, startTime);
            List<SpuAttribute> spuAttributes = spuAttributeDao.findBySpuIds(spuIds);
            Map<Long, SpuAttribute> spuAttributeMap = spuAttributes.stream().collect(Collectors.toMap(SpuAttribute::getSpuId, Function.identity()));
            Map<Long, BackCategory> backCategoryMap = Maps.newHashMap();
            for (Spu spu : partition) {
                BackCategory backCategory = backCategoryCacher.findBackCategoryById(spu.getCategoryId());//先用着吧
                backCategoryMap.put(spu.getId(), backCategory);
            }

            result.addAll(skuTemplates.stream().map(skuTemplate -> convert(skuTemplate, spuAttributeMap, backCategoryMap, brandMapping)).collect(Collectors.toList()));
        });

        return result;
    }

    private YintaiPushItemDTO convert(SkuTemplate skuTemplate, Map<Long, SpuAttribute> spuAttributeMap, Map<Long, BackCategory> backCategoryMap,BrandMapping brandMapping) {
        if (brandMapping == null) {
            return null;
        }
        YintaiPushItemDTO itemDTO = new YintaiPushItemDTO();
        Map<String, String> extra = skuTemplate.getExtra();
        String materialId = extra.get("materialId");//spuCode
        itemDTO.setSkuId(skuTemplate.getId());
        itemDTO.setBrandId(brandMapping.getBrandId());
        itemDTO.setOutBrandId(brandMapping.getChannelBrandId());
        itemDTO.setSpuCode(materialId);//货号
        itemDTO.setSkuCode(skuTemplate.getSkuCode());//条码
        itemDTO.setName(skuTemplate.getName());
        itemDTO.setPrice(MoreObjects.firstNonNull(skuTemplate.getExtraPrice().get("originPrice"), 0).toString());//吊牌价
        Brand brand = brandCacher.findBrandById(brandMapping.getBrandId());
        if (brand != null) {
            itemDTO.setBrandName(brand.getName());
        }
        //货品年份,货品季节,颜色,类别,性别

        //spu属性 年份、季节、性别
        SpuAttribute spuAttribute = spuAttributeMap.get(skuTemplate.getSpuId());
        if (spuAttribute != null) {//正常不会为null
            itemDTO.setSpuId(spuAttribute.getSpuId());
            List<GroupedOtherAttribute> otherAttrs = MoreObjects.firstNonNull(spuAttribute.getOtherAttrs(), Collections.emptyList());
            otherAttrs.stream().filter(attr -> "SPU".equals(attr.getGroup())).findFirst().ifPresent(attr->attr.getOtherAttributes().forEach(_attr->{
                if (YintaiAttributeEnum.from(_attr.getAttrKey()) != null) {
                    itemDTO.getAttrs().put(YintaiAttributeEnum.from(_attr.getAttrKey()), _attr.getAttrVal());
                }
            }));
        } else {
            log.error("[yintai] convert push item, spuAttribute miss. skuTemplateId({}) ", skuTemplate.getId());
        }

        //sku属性 颜色，尺码
        skuTemplate.getAttrs().forEach(attr->{
            if (YintaiAttributeEnum.from(attr.getAttrKey()) != null) {
                itemDTO.getAttrs().put(YintaiAttributeEnum.from(attr.getAttrKey()), attr.getAttrVal());
            }
        });

        //后台类目 类别
        BackCategory backCategory = backCategoryMap.get(skuTemplate.getSpuId());
        if (backCategory != null) {//正常不会为null
            itemDTO.getAttrs().put(YintaiAttributeEnum.CATEGORY, backCategory.getName());
        } else {
            log.error("[yintai] convert push item, backCategory miss. skuTemplateId({}) , spuId({})", skuTemplate.getId(), skuTemplate.getSpuId());
        }

        return itemDTO;
    }

    /**
     * 获取sku更新时间大于startTime的
     * @param skuTemplates
     * @param startTime
     * @return
     */
    private List<SkuTemplate> getGreaterThanStartTime(List<SkuTemplate> skuTemplates, Date startTime) {
        if (CollectionUtils.isEmpty(skuTemplates) || startTime == null) {
            return skuTemplates;
        }
        List<SkuTemplate> result = skuTemplates.stream().filter(skuTemplate -> startTime.before(skuTemplate.getUpdatedAt())).collect(Collectors.toList());
        log.info("[getGreaterThanStartTime]  skuTemplates size:({}),  startTime:({})", skuTemplates.size(), startTime);
        return result;
    }

    /**
     * 获取中台从未同步给银泰
     * @param skuTemplates
     * @return
     */
    private List<SkuTemplate> getNeverPushToYintai(List<SkuTemplate> skuTemplates, List<Long> spuIds) {
        if (CollectionUtils.isEmpty(skuTemplates)) {
            return skuTemplates;
        }
        OpenClientItemMappingService itemService = mappingCenter.getItemService(MiddleChannel.YINTAI.getValue());
        Set<String> needFilter = spuIds.stream().flatMap(spuId -> {
            Response<List<ItemMapping>> itemMappingResp = itemService.findItemListByChannelAndItemId(MiddleChannel.YINTAI.getValue(), spuId);
            if (!itemMappingResp.isSuccess()) {
                return Stream.empty();
            }
            return itemMappingResp.getResult().stream();
        }).map(itemMapping -> itemMapping.getItemId() + ":" + itemMapping.getSkuCode()).collect(Collectors.toSet());

        List<SkuTemplate> result = skuTemplates.stream().filter(skuTemplate -> !needFilter.contains(skuTemplate.getSpuId() + ":" + skuTemplate.getSkuCode())).collect(Collectors.toList());
        log.info("[getNeverPushToYintai]  skuTemplates size:({}), result size:({})", skuTemplates.size(), result.size());
        return result;
    }

    /**
     * 获取sku在每个门店的可用库存总和大于0
     * @param skuTemplates
     * @return
     */
    private List<SkuTemplate> getAllShopInventoryGreaterThanZero(List<SkuTemplate> skuTemplates) {
        if (CollectionUtils.isEmpty(skuTemplates)) {
            return skuTemplates;
        }
        //门店
        Response<List<OpenShop>> openShopResp = openShopReadService.findByChannel(MiddleChannel.YINTAI.getValue());
        if (!openShopResp.isSuccess()) {
            log.error("find yintai shop fail, error:({})", openShopResp.getError());
            return Collections.emptyList();
        }
        if (CollectionUtils.isEmpty(openShopResp.getResult())) {
            log.info("open shop is empty, skip.");
            return Collections.emptyList();
        }
        //银泰只有一个门店,这里直接取第一个
        OpenShop openShop = openShopResp.getResult().get(0);
        if (!OpenShopEnum.enable_open_shop_enum.getIndex().equals(openShop.getStatus())) {
            log.info("enable open shop is empty, skip.");
            return Collections.emptyList();
        }

        //获取默认发货规则下的仓库列表
        Response<List<Long>> warehouseIdsResp = warehouseRulesClient.findWarehouseIdsByShopId(openShop.getId());
        if (!warehouseIdsResp.isSuccess()) {
            log.error("find warehouses by shop fail. shopId({}), error:({})", openShop.getId(), warehouseIdsResp.getError());
        }
        List<Long> warehouseIds = warehouseIdsResp.getResult();
        if (CollectionUtils.isEmpty(warehouseIds)) {
            log.info("yintai shop warehouses is empty, skip.");
            return Collections.emptyList();
        }
        log.info("[getAllShopInventoryGreaterThanZero] yintai shop warehouseIds:({}) ", warehouseIds);

        List<SkuTemplate> result = Lists.newArrayList();
        //sku在每个店铺下的可用库存数是否>0
        skuTemplates.forEach(skuTemplate -> {
            //门店+sku库存推送规则
            Response<ShopStockRuleDto> shopStockRuleResp = warehouseShopRuleClient.findByShopIdAndSku(openShop.getId(), skuTemplate.getSkuCode());
            if (!shopStockRuleResp.isSuccess()) {
                log.error("find shop stock push rule fail. shop(id={}), error:{}", openShop.getId(), shopStockRuleResp.getError());
                return;
            }
            log.info("shop stock rule is sku({}), shopId({}), shopStockRule({})", skuTemplate.getSkuCode(), openShop.getId(), JSON.toJSONString(shopStockRuleResp.getResult()));
            ShopStockRuleDto shopStockRule = shopStockRuleResp.getResult();
            if (shopStockRule.getShopRule().getStatus() != 1) {
                log.info("shopStockRule status({}) not enable", shopStockRule.getShopRule().getStatus());
                return;
            }
            if (shopStockRule.getShopRule().getIsSubtractWaitHandle() == null) {
                log.info("shopStockRule IsSubtractWaitHandle is null, 库存推送规则未设置");
                return;
            }
            //推送库存时计数的可用库存数 //仓库+sku
            Long quantity = stockPusherLogic.calculateStock(openShop.getId(), skuTemplate.getSkuCode(), warehouseIds, shopStockRule);
            if (quantity > 0) {//库存数>0
                result.add(skuTemplate);
                log.info("sku inventory greaterThan 0, sku({}), quantity({}), ship", skuTemplate.getSkuCode(), quantity);
            } else {
                log.info("sku inventory lessThan 0, sku({}), ship", skuTemplate.getSkuCode());
            }
        });
        log.info("[getAllShopInventoryGreaterThanZero]  skuTemplates size:({}), result size:({})", skuTemplates.size(), result.size());

        return result;
    }

    /**
     * 保存映射
     * @param uploadItemList
     * @param sourceList
     * @return
     */
    public Response<Boolean> createItemMapping(OpenShop openShop, List<OpenClientItemYintai> uploadItemList, List<YintaiPushItemDTO> sourceList) {
        Map<String, YintaiPushItemDTO> sourceMapping = sourceList.stream()
                .collect(Collectors.toMap(o -> YintaiOutIdBuilder.getOutId(o.getSkuCode(), o.getSpuCode()), Function.identity(), (a,b)->b));
        val channel = MiddleChannel.YINTAI.getValue();
        OpenClientItemMappingService itemService = mappingCenter.getItemService(channel);

        List<ItemMapping> newItemMappings = uploadItemList.stream().map(item -> {
            YintaiPushItemDTO itemDTO = sourceMapping.get(item.getOuterId());
            ItemMapping mapping = new ItemMapping();
            mapping.setChannel(channel);
            mapping.setOpenShopId(openShop.getId());//
            mapping.setOpenShopName(openShop.getShopName());
            mapping.setItemId(itemDTO.getSpuId());
            mapping.setItemName(itemDTO.getSpuCode());
            mapping.setChannelItemId("");//银泰平台没提供商品纬度外部编码
            mapping.setSkuCode(itemDTO.getSkuCode());
            mapping.setChannelSkuId(item.getCspuId());
            mapping.setSkuAttributes(itemDTO.getAttrs().entrySet().stream().map(attr->{
                OpenClientSkuAttribute openAttr = new OpenClientSkuAttribute();
                openAttr.setAttributeKey(attr.getKey().getValue());
                openAttr.setAttributeValue(attr.getValue());
                return openAttr;
            }).collect(Collectors.toList()));
            mapping.setStatus(1);
            OpenClientSkuAttribute price = new OpenClientSkuAttribute();
            price.setAttributeKey("price");
            price.setAttributeValue(itemDTO.getPrice());
            mapping.getSkuAttributes().add(price);
            OpenClientSkuAttribute channelBrandId = new OpenClientSkuAttribute();
            channelBrandId.setAttributeKey("channelBrandId");
            channelBrandId.setAttributeValue(itemDTO.getOutBrandId());
            mapping.getSkuAttributes().add(channelBrandId);
            return mapping;
        }).collect(Collectors.toList());

        //已存在的更新
        for (ItemMapping newItemMapping : newItemMappings) {
            Response<Optional<ItemMapping>> findMapping = itemService.findByChannelSkuIdAndOpenShopId(newItemMapping.getChannelSkuId(), openShop.getId());
            if (findMapping.getResult().isPresent()) {
                //做删除操作
                ItemMapping originItemMapping = findMapping.getResult().get();
                if (Objects.equals(originItemMapping.getSkuCode(),newItemMapping.getSkuCode())){
                    middleYintaiLogService.saveLog(newItemMapping, ItemPushStatus.SUCCESS, null);
                    continue;
                }else{
                    //删除原有的映射关系
                    log.info("delete existed item mapping channelSkuId({}), originSkuCode is {} and newSkuCode is {}",newItemMapping.getChannelSkuId(), originItemMapping.getSkuCode(),newItemMapping.getSkuCode());
                    Response<ItemMapping> dR = itemService.delete(originItemMapping.getId());
                    if (!dR.isSuccess()){
                        continue;
                    }
                }
            }
            Response<Long> createdResp = itemService.createItemMapping(newItemMapping);
            if (!createdResp.isSuccess()) {
                log.error("yintai createItemMapping fail newItemMapping:({}), error:({})", newItemMapping, createdResp.getError());
            } else {
                //推库存,这里按店铺纬度提交
                List<InventoryChangeDTO> inventoryChanges = Lists.newArrayList();
                inventoryChanges.add(new InventoryChangeDTO(null, newItemMapping.getSkuCode(), openShop.getId()));
                inventoryChangeProducer.handleInventoryChange(inventoryChanges);
                log.info("yintai createItemMapping success, channelSkuId({}), itemId({}), skuCode({})", newItemMapping.getChannelSkuId(), newItemMapping.getItemId(), newItemMapping.getSkuCode());
            }
            middleYintaiLogService.saveLog(newItemMapping, createdResp.isSuccess() ? ItemPushStatus.SUCCESS:ItemPushStatus.FAIL, createdResp.getError());
        }

        return Response.ok(Boolean.TRUE);
    }

    /**
     * 针对开始时间的商品变动，执行上传任务
     * @param minusHour 时间范围， null表示无限制
     */
    public void uploadTask(Integer minusHour) {
        log.info("uploadTask handle start  ");
        try {
            OpenClientBrandMappingService brandService = mappingCenter.getBrandService(MiddleChannel.YINTAI.getValue());
            Response<List<BrandMapping>> brandResp = brandService.findBrandListByChannel(MiddleChannel.YINTAI.getValue());
            if (!brandResp.isSuccess()) {
                log.error("yintai push new item job fail , error:({})", brandResp.getError());
                return;
            }

            List<BrandMapping> filter = brandResp.getResult().stream().filter(brand -> !Strings.isNullOrEmpty(brand.getChannelBrandId())).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(filter)) {
                log.info("yintai brand mapping is empty, not need do. raw brand({})", JSON.toJSONString(brandResp.getResult()));
                return;
            }
            Date startTime = null;
            if (minusHour != null && minusHour >= 0) {
                startTime = DateTime.now().minusHours(minusHour).toDate();
            }

            yintaiMessageProducer.sendItemPush(filter, startTime);
        } catch (Exception e) {
            log.error("uploadTask handle fail, cause:({})", e);
        }
        log.info("uploadTask handle end  ");
    }

    /**
     * 指定skuCodes上传,忽略部分校验
     * 运维操作支持
     * @param skuCodes
     * @return
     */
    public Response<Map</*skuCode*/String, /*结果*/String>> uploadBySkuCodes(List<String> skuCodes) {
        log.info("[uploadBySkuCodes]  skuCodes:({})", skuCodes);
        List<SkuTemplate> skuTemplates = skuTemplateDao.findBySkuCodes(skuCodes);
        List<Long> spuIds = skuTemplates.stream().map(SkuTemplate::getSpuId).collect(Collectors.toList());
        skuTemplates = getNeverPushToYintai(skuTemplates, spuIds);
        skuTemplates = getAllShopInventoryGreaterThanZero(skuTemplates);
        if (skuTemplates.isEmpty()) {
            return Response.fail("没有符合sku在每个门店的可用库存总和大于0的");
        }

        OpenClientBrandMappingService brandService = mappingCenter.getBrandService(MiddleChannel.YINTAI.getValue());
        List<Spu> spus = spuDao.findByIds(spuIds);
        List<SpuAttribute> spuAttributes = spuAttributeDao.findBySpuIds(spuIds);
        Map<Long, SpuAttribute> spuAttributeMap = spuAttributes.stream().collect(Collectors.toMap(SpuAttribute::getSpuId, Function.identity()));
        Map<Long, BackCategory> backCategoryMap = Maps.newHashMap();
        Map<Long, BrandMapping> brandMap = Maps.newHashMap();
        for (Spu spu : spus) {
            Response<Map<String, BrandMapping>> brandMappingResp = brandService.findBrandMapping(spu.getBrandId());
            if (brandMappingResp.isSuccess()) {
                brandMap.put(spu.getId(), brandMappingResp.getResult().get(MiddleChannel.YINTAI.getValue()));
            }
            BackCategory backCategory = backCategoryCacher.findBackCategoryById(spu.getCategoryId());//先用着吧
            backCategoryMap.put(spu.getId(), backCategory);
        }

        List<YintaiPushItemDTO> itemDTOList = skuTemplates.stream()
                .map(skuTemplate -> convert(skuTemplate, spuAttributeMap, backCategoryMap, brandMap.get(skuTemplate.getSpuId())))
                .collect(Collectors.toList());

        Map<String, String> result = Maps.newHashMap();
        for (YintaiPushItemDTO itemDTO : itemDTOList) {
            Response<Boolean> response = uploadHandle(Lists.newArrayList(itemDTO));
            result.put(itemDTO.getSkuCode(), response.isSuccess()?response.getResult().toString():response.getError());
        }

        return Response.ok(result);
    }


    /**
     * 执行上传操作，保存映射结果
     * @param pushItems
     * @return
     */
    public Response<Boolean> uploadHandle(List<YintaiPushItemDTO> pushItems) {
        log.info("[uploadHandle] start, pushItems({})", pushItems.stream().map(YintaiPushItemDTO::getSkuCode).collect(Collectors.toList()));
        Response<List<OpenShop>> openShopResp = openShopReadService.findByChannel(MiddleChannel.YINTAI.getValue());
        if (!openShopResp.isSuccess() || CollectionUtils.isEmpty(openShopResp.getResult())) {
            log.error("yintai shop not exist, skip push item.");
            return Response.fail("yintai.shop.not.exist");
        }
        OpenShop openShop = openShopResp.getResult().get(0);//银泰店铺只有一个。//商品上传不需要店柜id。

        List<OpenClientSkuYintai> collect = pushItems.stream().map(this::convert).collect(Collectors.toList());
        List<List<OpenClientSkuYintai>> partition = Lists.partition(collect, 20);//银泰限制20条
        for (List<OpenClientSkuYintai> part : partition) {
            try {
                CompletableFuture.runAsync(()->{
                    try {
                        Stopwatch stopwatch = Stopwatch.createStarted();
                        log.info("add item batch to yintai start");
                        OpenClientItemService openClientItemService = itemServiceRegistryCenter.getItemService(MiddleChannel.YINTAI.getValue());

                        Response<List<OpenClientItemYintai>> updated = openClientItemService.addItemBatchToYintai(openShop.getId(), part);
                        stopwatch.stop();
                        log.info("add item batch to yintai end, cost:({}), result:({})", stopwatch.elapsed(TimeUnit.MILLISECONDS), JSON.toJSONString(updated));

                        if (!updated.isSuccess()) {
                            log.error("add item batch to yintai fail, error:({})", updated.getError());
                            throw new ServiceException(updated.getError());
                        }
                        //重复上传会返回多条重复数据
                        List<OpenClientItemYintai> updatedList = updated.getResult().stream().unordered().distinct().collect(Collectors.toList());
                        log.info("create item mapping updatedList size:({})", updatedList.size());
                        Response<Boolean> created = createItemMapping(openShop, updatedList, pushItems);
                        if (!created.isSuccess()) {
                            log.error("create item mapping fail to yintai");
                        }
                        else {
                            log.info("create item mapping to yintai end");
                        }
                    } catch (Exception e) {
                        middleYintaiLogService.saveErrorLog(new RecordLog(pushItems, e.getMessage()));
                        log.error("add item batch fail to yintai, cause:({})", e);
                        throw new ServiceException("upload or save item mapping fail");
                    }

                }, executorService);
            } catch (Exception e) {
                log.error("[uploadHandle]  pushItems size:({}), cause:({})", pushItems.size(), e);
            }
        }
        return Response.ok(Boolean.TRUE);
    }

    private OpenClientSkuYintai convert(YintaiPushItemDTO item) {
        OpenClientSkuYintai openItem = new OpenClientSkuYintai();
        openItem.setArtNo(item.getSpuCode());
        openItem.setBarcode(item.getSkuCode());
        openItem.setPrice(item.getPrice());
        openItem.setItemName(item.getName());
        openItem.setTitle(item.getName());
        openItem.setSkuAttributes(item.getAttrs().entrySet().stream().map(attr->{
            OpenClientSkuAttribute openAttr = new OpenClientSkuAttribute();
            openAttr.setAttributeKey(attr.getKey().getValue());
            openAttr.setAttributeValue(attr.getValue());
            return openAttr;
        }).collect(Collectors.toList()));
        openItem.setBrandId(item.getOutBrandId());
        openItem.setBrandName(item.getBrandName());
        return openItem;
    }

    //如果是银泰的关联品牌下的，尝试创建银泰的商品映射
    public boolean stockAdjustHandle(String skuCode) {
        List<SkuTemplate> skuTemplates = skuTemplateDao.findBySkuCodes(Lists.newArrayList(skuCode));
        if (CollectionUtils.isEmpty(skuTemplates)) {
            return false;
        }

        boolean flag = false;
        List<YintaiBrand> yintaiBrandList = middleYintaiBrandService.getYintaiBrandList();
        Set<String> collect = yintaiBrandList.stream().map(YintaiBrand::getBrandId).collect(Collectors.toSet());
        for (SkuTemplate skuTemplate : skuTemplates) {
            Spu spu = spuDao.findById(skuTemplate.getSpuId());
            if (!collect.contains(spu.getBrandId().toString())) {
                continue;
            }
            log.info("sku has yintai brand mapping, brand({}), skuCode:({})",spu.getBrandId(), skuCode);
            yintaiMessageProducer.sendItemPush(skuCode);
            flag = true;
        }
        return flag;
    }
}
