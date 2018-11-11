package com.pousheng.middle.web.erp;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.pousheng.erp.cache.ErpBrandCacher;
import com.pousheng.erp.component.BrandImporter;
import com.pousheng.erp.component.MaterialPusher;
import com.pousheng.erp.component.MposWarehousePusher;
import com.pousheng.erp.component.SpuImporter;
import com.pousheng.erp.model.PoushengMaterial;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.item.dto.ItemNameAndStock;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.enums.PsSpuType;
import com.pousheng.middle.item.service.SkuTemplateDumpService;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.open.mpos.MposOrderHandleLogic;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dispatch.component.MposSkuStockLogic;
import com.pousheng.middle.order.dto.PoushengCompensateBizCriteria;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.order.service.MiddleOrderWriteService;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.shop.dto.ShopExtraInfo;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.WarehouseRulesClient;
import com.pousheng.middle.web.events.trade.listener.AutoCreateShipmetsListener;
import com.pousheng.middle.web.item.cacher.GroupRuleCacherProxy;
import com.pousheng.middle.web.item.component.PushMposItemComponent;
import com.pousheng.middle.web.mq.warehouse.InventoryChangeProducer;
import com.pousheng.middle.web.mq.warehouse.model.InventoryChangeDTO;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.vip.SyncVIPLogic;
import com.pousheng.middle.web.warehouses.component.WarehouseImporter;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.JsonMapper;
import io.terminus.common.utils.Splitters;
import io.terminus.open.client.center.event.OpenClientOrderSyncEvent;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import io.terminus.open.client.order.dto.OpenClientOrderItem;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.OrderReadService;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.order.service.ShopOrderReadService;
import io.terminus.parana.search.dto.SearchedItemWithAggs;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.joda.time.format.DateTimeParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-31
 */
@RestController
@Slf4j
@RequestMapping("/api/middle/task")
public class FireCall {

    private final SpuImporter spuImporter;

    private final BrandImporter brandImporter;

    private final WarehouseImporter warehouseImporter;

    private final MaterialPusher materialPusher;

    private final ErpBrandCacher brandCacher;

    private final MposWarehousePusher mposWarehousePusher;

    private final QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;

    @RpcConsumer
    @Setter
    private SkuTemplateSearchReadService skuTemplateSearchReadService;
    @Autowired
    private WarehouseRulesClient warehouseRulesClient;
    private final ShopCacher shopCacher;
    private final WarehouseCacher warehouseCacher;

    private final DateTimeFormatter dft;
    @RpcConsumer
    @Setter
    private MappingReadService mappingReadService;
    @RpcConsumer
    private ShipmentReadService shipmentReadService;
    @Autowired
    private ShipmentWiteLogic shipmentWiteLogic;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private AutoCreateShipmetsListener autoCreateShipmetsListener;
    @Autowired
    private SkuTemplateDumpService skuTemplateDumpService;

    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;
    @Autowired
    private PushMposItemComponent pushMposItemComponent;
    @Autowired
    @Setter
    private MiddleShopCacher middleShopCacher;
    @Autowired
    private MposOrderHandleLogic mposOrderHandleLogic;
    @Autowired
    @Setter
    private GroupRuleCacherProxy GroupRuleCacherProxy;
    @Autowired
    private InventoryChangeProducer inventoryChangeProducer;
    @Autowired
    private MposSkuStockLogic mposSkuStockLogic;
    @Autowired
    private SyncVIPLogic syncVIPLogic;

    @Autowired
    private ShopMaxOrderLogic shopMaxOrderLogic;
    @Autowired
    private ShopOrderReadService shopOrderReadService;
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @Autowired
    private PoushengCompensateBizReadService compensateBizReadService;
    @RpcConsumer
    private MiddleOrderWriteService middleOrderWriteService;


    private ExecutorService fixTmallFeeService =new ThreadPoolExecutor(6, 10, 60L, TimeUnit.MINUTES,
            new LinkedBlockingQueue<>(500000),
            new ThreadFactoryBuilder().setNameFormat("tmall-presale-fee-executor-%d").build(),
            (r, executor) -> log.error("fixTmallFee task {} is rejected", r));


    @Autowired
    public FireCall(SpuImporter spuImporter, BrandImporter brandImporter,
                    WarehouseImporter warehouseImporter, MaterialPusher materialPusher, ErpBrandCacher brandCacher, MposWarehousePusher mposWarehousePusher, QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi,
                    SkuTemplateSearchReadService skuTemplateSearchReadService, ShopCacher shopCacher, WarehouseCacher warehouseCacher) {
        this.spuImporter = spuImporter;
        this.brandImporter = brandImporter;
        this.warehouseImporter = warehouseImporter;
        this.materialPusher = materialPusher;
        this.brandCacher = brandCacher;
        this.mposWarehousePusher = mposWarehousePusher;
        this.queryHkWarhouseOrShopStockApi = queryHkWarhouseOrShopStockApi;
        this.skuTemplateSearchReadService = skuTemplateSearchReadService;
        this.shopCacher = shopCacher;
        this.warehouseCacher = warehouseCacher;

        DateTimeParser[] parsers = {
                DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").getParser(),
                DateTimeFormat.forPattern("yyyy-MM-dd").getParser()};
        dft = new DateTimeFormatterBuilder().append(null, parsers).toFormatter();
    }

    @RequestMapping(value = "/spu/import", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String spuInput(@RequestParam String file) {
        try {
            ObjectMapper mapper = JsonMapper.nonEmptyMapper().getMapper();
            String path = "/Users/songrenfei/Downloads/";
            path = path + file + ".txt";

            log.info("handle process {}", path);
            File file1 = new File(path);
            String content = readFile(file1);
            JsonNode root = mapper.readTree(content);
            boolean success = root.findPath("retCode").asInt() == 0;
            if (!success) {
                log.error(root.findPath("retMessage").textValue());
                return "fail";
            }
            List<PoushengMaterial> poushengMaterials = mapper.readValue(root.findPath("list").toString(),
                    new TypeReference<List<PoushengMaterial>>() {
                    });

            return String.valueOf(poushengMaterials.size());
            /*for (PoushengMaterial poushengMaterial : poushengMaterials) {
                Brand brand = brandCacher.findByCardName(poushengMaterial.getCard_name());
                spuImporter.doProcess(poushengMaterial, brand);
            }*/

        } catch (Exception e) {
            log.error("failed to sync material from erp ", e);
        }
        return "ok";
    }

    @RequestMapping(value = "/brand", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String synchronizeBrand(@RequestParam String start,
                                   @RequestParam(name = "end", required = false) String end) {
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-BRAND-START param: start [{}] end [{}]", start, end);
        }
        Date from = dft.parseDateTime(start).toDate();
        Date to = new Date();
        if (StringUtils.hasText(end)) {
            to = dft.parseDateTime(end).toDate();
        }
        int cardCount = brandImporter.process(from, to);
        log.info("synchronized {} brands", cardCount);
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-BRAND-END param: start [{}] end [{}] ,resp: [{}]", start, end, "ok");
        }
        return "ok";
    }


    @RequestMapping(value = "/spu", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String synchronizeSpu(@RequestParam String start,
                                 @RequestParam(name = "end", required = false) String end) {
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-SPU-START param: start [{}] end [{}]", start, end);
        }
        Date from = dft.parseDateTime(start).toDate();
        Date to = new Date();
        if (StringUtils.hasText(end)) {
            to = dft.parseDateTime(end).toDate();
        }
        //log.info("synchronize brand first");
        //int cardCount = brandImporter.process(from, to);
        //log.info("synchronized {} brands", cardCount);
        int spuCount = spuImporter.process(from, to);
        log.info("synchronized {} spus", spuCount);
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-SPU-END param: start [{}] end [{}] ,resp: [{}]", start, end, "ok");
        }
        return "ok";
    }

    @RequestMapping(value = "/warehouse", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean synchronizeWarehouse(@RequestParam(name = "start", required = false, defaultValue = "") String start,
                                        @RequestParam(name = "end", required = false, defaultValue = "") String end) {
        Date from = DateTime.now().withTimeAtStartOfDay().toDate();
        if (StringUtils.hasText(start)) {
            from = dft.parseDateTime(start).toDate();
        }
        Date to = DateTime.now().withTimeAtStartOfDay().plusDays(1).minusSeconds(1).toDate();
        if (StringUtils.hasText(end)) {
            to = dft.parseDateTime(end).toDate();
        }
        log.info("begin to synchronize warehouse from {} to {}", from, to);
        int warehouseCount = warehouseImporter.process(from, to);
        log.info("synchronized {} warehouses", warehouseCount);
        return Boolean.TRUE;

    }


    @RequestMapping(value = "/spu/stock", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String synchronizeSpuStock(@RequestParam Long spuId) {
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-SPU-STOCK-START param: spuId [{}] ", spuId);
        }
        //向库存那边推送这个信息, 表示要关注这个商品对应的单据
        materialPusher.addSpus(Lists.newArrayList(spuId));
        //调用恒康抓紧给我返回库存信息
        materialPusher.pushItemForStock(spuId);
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-SPU-STOCK-END param: spuId [{}] ,resp: [{}]", spuId, "ok");
        }
        return "ok";
    }

    @RequestMapping(value = "/shop/spu/stock", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String synchronizeShopSpuStock(@RequestParam(name = "shopId") Long shopId, @RequestParam(name = "limit", required = false, defaultValue = "1000") Integer limit) {
        try {
            log.info("begin to synchronizeShopSpuStock shopId is {}", shopId);
            int pageNo = 1;
            int pageSize = (null == limit || limit > 1000) ? 1000 : limit;
            while (true) {
                Response<Paging<Long>> res = mappingReadService.findItemIdByShopId(shopId, 1, pageNo, pageSize);
                if (!res.isSuccess()) {
                    log.error("fail to find item mapping by shopId={} pageNo={},pageSize={},error:{}", shopId,
                            pageNo, pageSize, res.getError());
                    break;
                }

                Paging<Long> page = res.getResult();
                if (null == page) {
                    break;
                }
                List<Long> itemIds = page.getData();
                if (CollectionUtils.isEmpty(itemIds)) {
                    break;
                }
                log.info("start to push hk item shopId [{}] pageNo [{}] find itemIds []", shopId, pageNo, itemIds.toString());
                try {
                    //向库存那边推送这个信息, 表示要关注这个商品对应的单据
                    materialPusher.addSpus(itemIds);
                } catch (Exception e) {
                    log.error("synchronizeShopSpuStock has an error:{}", Throwables.getStackTraceAsString(e));
                }
                pageNo++;
            }

        } catch (Exception e) {
            log.error("call synchronizeShopSpuStock fail,cause:{}", Throwables.getStackTraceAsString(e));
        }
        log.info("end synchronizeShopSpuStock shopId is {}", shopId);
        return "ok";
    }

    @RequestMapping(value = "/add/mpos/warehouse", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String syncMposWarehouse(@RequestParam String companyId,
                                    @RequestParam String stockId) {
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-ADD-MPOS-WAREHOUSE-START param: companyId [{}] stockId [{}]", companyId, stockId);
        }
        mposWarehousePusher.addWarehouses(companyId, stockId);
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-ADD-MPOS-WAREHOUSE-END param: companyId [{}] stockId [{}] ,resp: [{}]", companyId, stockId, "ok");
        }
        return "ok";
    }

    @RequestMapping(value = "/del/mpos/warehouse", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String delMposWarehouse(@RequestParam String companyId,
                                   @RequestParam String stockId) {
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-DEL-MPOS-WAREHOUSE-START param: companyId [{}] stockId [{}]", companyId, stockId);
        }
        mposWarehousePusher.removeWarehouses(companyId, stockId);
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-DEL-MPOS-WAREHOUSE-END param: companyId [{}] stockId [{}] ,resp: [{}]", companyId, stockId, "ok");
        }
        return "ok";
    }

    @RequestMapping(value = "/spu/by/sku/code", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String synchronizeSpuByBarCode(@RequestParam String skuCode) {
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-SPU-BY-SKU-CODE-START param: skuCode [{}] ", skuCode);
        }
        int spuCount = spuImporter.processPullMarterials(skuCode);
        log.info("synchronized {} spus", spuCount);
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-SPU-BY-SKU-CODE-END param: skuCode [{}] ,resp: [{}]", skuCode, "ok");
        }
        return "ok";
    }

    @RequestMapping(value = "/query/stock", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<HkSkuStockInfo> queryStock(@RequestParam(required = false) String stockCodes,
                                           @RequestParam String skuCodes,
                                           @RequestParam(required = false, defaultValue = "0") Integer stockType) {
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-QUERY-STOCK-START param: stockCodes [{}] skuCodes [{}] stockType [{}]", stockCodes, skuCodes, stockType);
        }
        List<String> stockCodesList = null;
        if (!Strings.isNullOrEmpty(stockCodes)) {
            stockCodesList = Splitters.COMMA.splitToList(stockCodes);
        }
        List<String> skuCodesList = Splitters.COMMA.splitToList(skuCodes);
        List<HkSkuStockInfo> stockinfos = queryHkWarhouseOrShopStockApi.doQueryStockInfo(stockCodesList, skuCodesList, stockType);
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-QUERY-STOCK-END param: stockCodes [{}] skuCodes [{}] stockType [{}] ,resp: [{}]", stockCodes, skuCodes, stockType, JsonMapper.nonEmptyMapper().toJson(stockinfos));
        }
        return stockinfos;

    }


    @ApiOperation("计算库存")
    @RequestMapping(value = "/count/stock", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long countStock(@RequestParam(required = false) String stockCodes,
                           @RequestParam String skuCodes,
                           @RequestParam(required = false, defaultValue = "0") Integer stockType) {
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-COUNT-STOCK-START param: stockCodes [{}] skuCodes [{}] stockType [{}]", stockCodes, skuCodes, stockType);
        }
        Long total = 0L;
        List<String> stockCodesList = null;
        if (!Strings.isNullOrEmpty(stockCodes)) {
            stockCodesList = Splitters.COMMA.splitToList(stockCodes);
        }
        List<String> skuCodesList = Splitters.COMMA.splitToList(skuCodes);
        List<HkSkuStockInfo> skuStockInfos = queryHkWarhouseOrShopStockApi.doQueryStockInfo(stockCodesList, skuCodesList, stockType);
        for (HkSkuStockInfo hkSkuStockInfo : skuStockInfos) {
            for (HkSkuStockInfo.SkuAndQuantityInfo skuAndQuantityInfo : hkSkuStockInfo.getMaterial_list()) {
                total += skuAndQuantityInfo.getQuantity();
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-COUNT-STOCK-END param: stockCodes [{}] skuCodes [{}] stockType [{}] ,resp: [{}]", stockCodes, skuCodes, stockType, total);
        }
        return total;
    }


    /**
     * 根据货号和尺码查询商品信息
     *
     * @param materialId 货号
     * @param attr       指定属性
     * @param companyId  公司编号
     * @return 商品信息
     * @outerId outerId  外码
     */
    @ApiOperation("根据货号查询")
    @GetMapping(value = "/search/for/mpos", produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<SearchSkuTemplate> searchSize(@RequestParam String materialId,
                                                @RequestParam(required = false) String attr,
                                                @RequestParam Long companyId, @RequestParam String outerId) {
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-SEARCH-SKU-TEMPLATE-START param: materialId [{}] attr [{}] companyId [{}] outerId [{}]", materialId, attr, companyId, outerId);
        }
        Shop currentShop = middleShopCacher.findByOuterIdAndBusinessId(outerId, companyId);
        ShopExtraInfo currentShopExtraInfo = ShopExtraInfo.fromJson(currentShop.getExtra());
        Long openShopId = currentShopExtraInfo.getOpenShopId();
        if (Arguments.isNull(openShopId)) {
            log.error("shop(id:{}) not mapping open shop", currentShop.getId());
            throw new JsonResponseException("shop.not.mapping.open.shop");
        }
        List<Long> groupIds = GroupRuleCacherProxy.findByShopId(openShopId);
        if (CollectionUtils.isEmpty(groupIds)) {
            return new Paging<>(0L, Lists.newArrayList());
        }
        String templateName = "ps_search.mustache";
        Map<String, String> params = Maps.newHashMap();
        params.put("spuCode", materialId);
        params.put("groupIds", Joiners.COMMA.join(GroupRuleCacherProxy.findByShopId(openShopId)));
        if (!StringUtils.isEmpty(attr)) {
            params.put("attrs", attr);
        }
        Response<? extends SearchedItemWithAggs<SearchSkuTemplate>> response = skuTemplateSearchReadService.searchWithAggs(1, 30, templateName, params, SearchSkuTemplate.class);
        if (!response.isSuccess()) {
            log.error("query sku template by materialId:{} and size:{} fail,error:{}", materialId, response.getError());
            throw new JsonResponseException(response.getError());
        }
        List<SearchSkuTemplate> list = response.getResult().getEntities().getData();
        if (!CollectionUtils.isEmpty(list)) {
            for (SearchSkuTemplate e : list) {
                List<SkuAttribute> attrs = Lists.newArrayList();
                for (String info : e.getAttributes()) {
                    SkuAttribute skuAttr = new SkuAttribute();
                    List<String> kv = Splitters.COLON.splitToList(info);
                    if (kv.size() > 1) {
                        skuAttr.setAttrKey(kv.get(0));
                        skuAttr.setAttrVal(kv.get(1));
                        attrs.add(skuAttr);
                    }
                }
                e.setAttrs(attrs);
            }
        }

        Paging<SearchSkuTemplate> paging = response.getResult().getEntities();

        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-SEARCH-SKU-TEMPLATE-END param: materialId [{}] attr [{}] companyId [{}] outerId [{}] result:{}", materialId, attr, companyId, outerId, paging.getData());
        }
        return paging;
    }


    /**
     * 根据货号和尺码查询
     *
     * @param materialId 货号
     * @param size       尺码
     * @return 商品信息
     */
    @RequestMapping(value = "/count/stock/for/mpos", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ItemNameAndStock countStock(@RequestParam String materialId, @RequestParam String size,
                                       @RequestParam Long companyId, @RequestParam String outerId) {

        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-COUNT-STOCK-FOR-MPOS-START param: materialId [{}] size [{}] companyId [{}] outerId [{}]", materialId, size, companyId, outerId);
        }

        //1、查询店铺是否存在
        Shop currentShop = middleShopCacher.findByOuterIdAndBusinessId(outerId, companyId);
        ShopExtraInfo currentShopExtraInfo = ShopExtraInfo.fromJson(currentShop.getExtra());
        Long openShopId = currentShopExtraInfo.getOpenShopId();
        if (Arguments.isNull(openShopId)) {
            log.error("shop(id:{}) not mapping open shop", currentShop.getId());
            throw new JsonResponseException("shop.not.mapping.open.shop");
        }

        //2、根据货号和尺码查询 spuCode=20171214001&attrs=年份:2017
        String templateName = "ps_search.mustache";
        List<Long> groupIds = GroupRuleCacherProxy.findByShopId(openShopId);
        if (CollectionUtils.isEmpty(groupIds)) {
            log.error("middle not find group ids by openShopId:{}", openShopId);
            return new ItemNameAndStock();
        }
        Map<String, String> params = Maps.newHashMap();
        params.put("spuCode", materialId);
        params.put("attrs", "尺码:" + size);
        Response<? extends SearchedItemWithAggs<SearchSkuTemplate>> response = skuTemplateSearchReadService.searchWithAggs(1, 20, templateName, params, SearchSkuTemplate.class);
        if (!response.isSuccess()) {
            log.error("query sku template by materialId:{} and size:{} fail,error:{}", materialId, size, response.getError());
            throw new JsonResponseException(response.getError());
        }
        List<SearchSkuTemplate> searchSkuTemplates = response.getResult().getEntities().getData();
        if (CollectionUtils.isEmpty(searchSkuTemplates)) {
            log.error("middle not find sku template by materialId:{} and size:{} and shopIds:{}", materialId, size, groupIds);
            return new ItemNameAndStock();
        }
        SearchSkuTemplate searchSkuTemplate = searchSkuTemplates.get(0);
        ItemNameAndStock itemNameAndStock = new ItemNameAndStock();
        itemNameAndStock.setName(searchSkuTemplate.getName());

        //3、查询门店的发货仓范围
        Response<List<Long>> warehouseIdsRes = warehouseRulesClient.findWarehouseIdsByShopId(openShopId);
        if (!warehouseIdsRes.isSuccess()) {
            log.error("find warehouse rule item by shop id:{} fail,error:{}", openShopId, warehouseIdsRes.getError());
            throw new JsonResponseException(warehouseIdsRes.getError());
        }

        List<Long> warehouseIds = warehouseIdsRes.getResult();

        //过滤超过最大接单量的店仓
        warehouseIds=shopMaxOrderLogic.filterWarehouse(warehouseIds);

        if (CollectionUtils.isEmpty(warehouseIds)) {
            log.info("there is no warehouse to dispatch for openShopId: {} which get from shop:{}", openShopId, JSON.toJSONString(currentShop));

            itemNameAndStock.setCurrentShopQuantity(0L);
            itemNameAndStock.setStockQuantity(0L);
            return itemNameAndStock;
        }

        String skuCode = searchSkuTemplate.getSkuCode();
        Long total = 0L;
        Long currentCompanyQuantity = 0L;
        List<String> skuCodesList = Splitters.COMMA.splitToList(skuCode);
        List<HkSkuStockInfo> skuStockInfos = queryHkWarhouseOrShopStockApi.doQueryStockInfo(warehouseIds, skuCodesList, openShopId);
        for (HkSkuStockInfo hkSkuStockInfo : skuStockInfos) {
            //仓
            if (com.google.common.base.Objects.equal(2, Integer.valueOf(hkSkuStockInfo.getStock_type()))) {
                for (HkSkuStockInfo.SkuAndQuantityInfo skuAndQuantityInfo : hkSkuStockInfo.getMaterial_list()) {
                    Integer warehouseStock = skuAndQuantityInfo.getQuantity();
                    if (warehouseStock < 0) {
                        warehouseStock = 0;
                    }

                    total += warehouseStock;
                    //当前公司库存
                    if (Objects.equals(String.valueOf(companyId), hkSkuStockInfo.getCompany_id())) {
                        currentCompanyQuantity += warehouseStock;
                    }
                }
                //店
            } else {
                for (HkSkuStockInfo.SkuAndQuantityInfo skuAndQuantityInfo : hkSkuStockInfo.getMaterial_list()) {
                    Shop shop = shopCacher.findShopById(hkSkuStockInfo.getBusinessId());
                    ShopExtraInfo shopExtraInfo = ShopExtraInfo.fromJson(shop.getExtra());
                    //安全库存
//                    Integer safeStock = Arguments.isNull(shopExtraInfo.getSafeStock()) ? 0 : shopExtraInfo.getSafeStock();
//                    Integer currentShopStock = skuAndQuantityInfo.getQuantity() - safeStock;
                    Integer currentShopStock = skuAndQuantityInfo.getQuantity();
                    if (currentShopStock < 0) {
                        currentShopStock = 0;
                    }
                    total += currentShopStock;
                    //当前店铺库存
                    if (Objects.equals(shop.getId(), currentShop.getId())) {
                        itemNameAndStock.setCurrentShopQuantity(Long.valueOf(currentShopStock));
                    }
                    //当前公司库存
                    if (Objects.equals(String.valueOf(companyId), hkSkuStockInfo.getCompany_id())) {
                        currentCompanyQuantity += currentShopStock;
                    }
                }
            }

        }
        itemNameAndStock.setCurrentCompanyQuantity(currentCompanyQuantity);
        itemNameAndStock.setStockQuantity(total);
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-COUNT-STOCK-FOR-MPOS-END param: materialId [{}] size [{}] ,resp: [{}]", materialId, size, JsonMapper.nonEmptyMapper().toJson(itemNameAndStock));
        }
        return itemNameAndStock;
    }


    /**
     * 根据店铺id拉取基础货品信息
     *
     * @param openShopId
     * @return
     */
    @RequestMapping(value = "/sku/code/by/shop", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String synchronizeSpuByBarCode(@RequestParam Long openShopId) {
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-SKU-CODE-BY-SHOP-START param: openShopId [{}]", openShopId);
        }
        int pageNo = 0;
        int pageSize = 40;
        while (true) {
            Response<Paging<ItemMapping>> r = mappingReadService.findByOpenShopId(openShopId, null, pageNo, pageSize);
            Paging<ItemMapping> itemMappingPaging = r.getResult();
            List<ItemMapping> itemMappingList = itemMappingPaging.getData();
            if (itemMappingList.isEmpty()) {
                break;
            }
            for (ItemMapping itemMapping : itemMappingList) {
                if (!Objects.equals(itemMapping.getStatus(), -1)) {
                    int spuCount = spuImporter.processPullMarterials(itemMapping.getSkuCode());
                    log.info("synchronized {} spus", spuCount);
                }
            }
            pageNo++;
        }
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-SKU-CODE-BY-SHOP-END param: openShopId [{}] ,resp: [{}]", openShopId, "ok");
        }
        return "ok";
    }


    /**
     * 根据skuCode推送库存到第三方或者官网
     *
     * @param skuCode
     * @return
     */
    @RequestMapping(value = "/sync/stock/by/sku/code", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String synchronizeStockBySkuCode(@RequestParam String skuCode) {
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-SYNC-STOCK-BY-SKU-CODE-START param: skuCode [{}]", skuCode);
        }
        //stockPusher.submit(Lists.newArrayList(skuCode));
        inventoryChangeProducer.handleInventoryChange(InventoryChangeDTO.builder().skuCode(skuCode).warehouseId(null).build());
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-SYNC-STOCK-BY-SKU-CODE-END param: skuCode [{}] ,resp: [{}]", skuCode, "ok");
        }
        return "ok";
    }

    public static String readFile(File file) {
        StringBuilder result = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));//构造一个BufferedReader类来读取文件
            String s = null;
            while ((s = br.readLine()) != null) {//使用readLine方法，一次读一行
                result.append(System.lineSeparator() + s);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    /**
     * 修复skuTemplate表中
     *
     * @return
     */
    @RequestMapping(value = "/sku/extra/restore", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String skuTemplateExtraRestore(@RequestParam(required = false, value = "skuCode") String skuCode,
                                          @RequestParam(required = false, value = "type") Integer type,
                                          @RequestParam(required = false, value = "pageNo") Integer pageNo,
                                          @RequestParam(required = false, value = "pageSize") Integer pageSize) {
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-SKU-EXTRA-RESTORE-START param: skuCode [{}] type [{}] pageNo [{}] pageSize [{}]", skuCode, type, pageNo, pageSize);
        }
        boolean result = spuImporter.skuTemplateExtraRestore(skuCode, type, pageNo, pageSize);
        if (log.isDebugEnabled()) {
            log.debug("API-MIDDLE-TASK-SKU-EXTRA-RESTORE-END param: skuCode [{}] type [{}] pageNo [{}] pageSize [{}] ,resp: [{}]", skuCode, type, pageNo, pageSize, result);
        }
        if (result) {
            return "ok";
        } else {
            return "not ok";
        }
    }

    /**
     * 取消发货单
     */
    @RequestMapping(value = "/cancel/shipment", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void cancelShipments(@RequestParam String fileName) {

        String url = "/pousheng/file/" + fileName + ".csv";
        log.info("START-HANDLE-SHIPMENT-API for:{}", url);

        File file1 = new File(url);

        List<String> codes = readShipmentCode(file1);
        log.info("START-HANDLE-SHIPMENT-API for:{}", url);
        for (String code : codes) {
            log.info("START-HANDLE-SHIPMENT-CODE:{}", code);

            Response<Shipment> shipmentRes = shipmentReadService.findShipmentCode(code);
            if (!shipmentRes.isSuccess()) {
                log.error("find shipment by code :{} fail,error:{}", code, shipmentRes.getError());
                continue;
            }

            Shipment shipment = shipmentRes.getResult();
            if (!Objects.equals(shipment.getStatus(), 4)) {
                log.error("shipment code:{} status is :{} so skip cancel", code, shipment.getStatus());
                continue;
            }
            //to cancel
            log.info("try to cancel shipment, shipment code is {}", code);
            Response<Boolean> response = shipmentWiteLogic.rollbackShipment(shipment);
            if (!response.isSuccess()) {
                log.info("try to cancel shipment fail, shipment code is {},error:{}", code, response.getError());
                continue;
            }

            log.info("END-HANDLE-SHIPMENT-CODE:{}", code);
        }

        log.info("END-HANDLE-SHIPMENT-API for:{}", url);

    }


    /**
     * 创建发货单
     */
    @RequestMapping(value = "/create/shipment", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void autoShipments(@RequestParam String fileName) {

        String url = "/pousheng/file/" + fileName + ".csv";

        log.info("START-HANDLE-CREATE-SHIPMENT-API for:{}", url);
        File file1 = new File(url);

        List<String> codes = readShipmentCode(file1);
        log.info("START-HANDLE-CREATE-SHIPMENT-API for:{}", url);

        for (String code : codes) {
            log.info("START-HANDLE-CREATE-SHIPMENT-CODE:{}", code);

            Response<Shipment> shipmentRes = shipmentReadService.findShipmentCode(code);
            if (!shipmentRes.isSuccess()) {
                log.error("find shipment by code :{} fail,error:{}", code, shipmentRes.getError());
                continue;
            }

            Shipment shipment = shipmentRes.getResult();

            OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());

            OrderBase orderBase = orderReadLogic.findOrder(orderShipment.getOrderId(), OrderLevel.SHOP);
            if (!Objects.equals(orderBase.getStatus(), 1)) {
                log.error("orderBase id:{} status is :{} so skip create shipment by shipment code:{}", orderBase.getId(), orderBase.getStatus(), code);
                continue;
            }
            //to create shipment
            log.info("try to create shipment, shipment code is {} order id:{}", code, orderBase.getId());
            OpenClientOrderSyncEvent event = new OpenClientOrderSyncEvent(orderBase.getId());
            try {
                autoCreateShipmetsListener.onShipment(event);
            } catch (Exception e) {
                log.info("fail to create shipment, shipment code is {} order id:{}", code, orderBase.getId());
            }

            log.info("END-HANDLE-CREATE-SHIPMENT-CODE:{}", code);
        }

        log.info("END-HANDLE-CREATE-SHIPMENT-API for:{}", url);

    }


    /**
     * 创建发货单
     */
    @RequestMapping(value = "/create/shipment2", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void autoShipments2(@RequestParam String fileName) {

        String url = "/pousheng/file/" + fileName + ".csv";

        log.info("START-HANDLE-CREATE-SHIPMENT-API for:{}", url);
        File file1 = new File(url);

        List<String> ids = readShipmentCode(file1);
        log.info("START-HANDLE-CREATE-SHIPMENT-API for:{}", url);

        for (String idStr : ids) {
            log.info("START-HANDLE-CREATE-SHIPMENT-ID:{}", idStr);

            OrderBase orderBase = orderReadLogic.findOrder(Long.valueOf(idStr), OrderLevel.SHOP);
            if (!Objects.equals(orderBase.getStatus(), 1)) {
                log.error("orderBase id:{} status is :{} so skip create shipment by order id:{}", orderBase.getId(), orderBase.getStatus(), idStr);
                continue;
            }
            //to create shipment
            log.info("try to create shipment, order id is {} order id:{}", idStr, orderBase.getId());
            OpenClientOrderSyncEvent event = new OpenClientOrderSyncEvent(orderBase.getId());
            try {
                autoCreateShipmetsListener.onShipment(event);
            } catch (Exception e) {
                log.info("fail to create shipment, shipment code is {} order id:{}", idStr, orderBase.getId());
            }

            log.info("END-HANDLE-CREATE-SHIPMENT-ORDER-ID:{}", idStr);
        }

        log.info("END-HANDLE-CREATE-SHIPMENT-API for:{}", url);

    }


    /**
     * 修复shutemplate中extra为空或者缺少货号的数据
     */
    @RequestMapping(value = "/fix/skuTemplate", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void fixSkutemplate(@RequestParam String fileName) {
        String url = "/pousheng/file/" + fileName + ".csv";
        log.info("START-HANDLE-SKUTEMPLATE-API for:{}", url);
        File file1 = new File(url);

        List<String> skuCodes = readShipmentCode(file1);

        log.info("START-HANDLE-SKUTEMPLATE-API for:{}", url);
        for (String skuCode : skuCodes) {
            log.info("START-HANDLE-SKUTEMPLATE-CODE:{}", skuCode);
            if ("\"sku_code\"".equals(skuCode)) {
                continue;
            }
            spuImporter.dealFixSkuTemplateBySkucode(skuCode);
            log.info("END-HANDLE-SKUTEMPLATE-CODE:{}", skuCode);
        }

        log.info("END-HANDLE-SKUTEMPLATE-API for:{}", url);

    }

    @RequestMapping(value = "/fix/order/need/cancel", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void fixOrdersNeedCancel(@RequestParam String fileName,@RequestParam String outfrom){
        String url = "/pousheng/file/" + fileName + ".csv";
        log.info("START-HANDLE-ORDER-NEED-CANCEL-API for:{}", url);
        File file1 = new File(url);
        List<String> outerOrderIds = readOrderIds(file1);
        for (String outerOrderId:outerOrderIds){
            log.info("try cancel shop order from file,outerOrderId {}",outerOrderId);
            Response<Optional<ShopOrder>> optionalResponse = shopOrderReadService.findByOutIdAndOutFrom(outerOrderId,outfrom);
            if (!optionalResponse.isSuccess()){
                log.error("find order failed,outerId {},outfrom {},caused by {}",outerOrderId,outfrom,optionalResponse.getError());
            }
            Optional<ShopOrder> shopOrderOptional = optionalResponse.getResult();
            if (shopOrderOptional.isPresent()){
                try{
                    orderWriteLogic.autoCancelShopOrder(shopOrderOptional.get().getId());
                }catch (Exception e){
                    log.error("cancel shop order from file failed,outerId {},caused by {}",outerOrderId,Throwables.getStackTraceAsString(e));
                }
            }
        }
    }

    /**
     * 修复门店拒单后为更新售后单的数据
     */
    @RequestMapping(value = "/fix/shipment/reject/error", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void fixShipmentRejectError(@RequestParam String fileName) {
        String url = "/pousheng/file/" + fileName + ".csv";
        log.info("START-HANDLE-SHIPMENT-REJECT-API for:{}", url);
        File file1 = new File(url);

        List<String> shipmentCodes = readShipmentCode(file1);

        log.info("START-HANDLE-SHIPMENT-REJECT-API for:{}", url);
        for (String shipmentCode : shipmentCodes) {
            log.info("START-HANDLE-SHIPMENT-REJECT-CODE:{}", shipmentCode);


            Response<Shipment> shipmentRes = shipmentReadService.findShipmentCode(shipmentCode);
            if (!shipmentRes.isSuccess()) {
                log.error("find shipment by code :{} fail,error:{}", shipmentCode, shipmentRes.getError());
                continue;
            }

            Shipment shipment = shipmentRes.getResult();
            if (Objects.equals(shipment.getStatus(), -7) && Objects.equals(shipment.getType(), ShipmentType.EXCHANGE_SHIP.value())) {
                mposOrderHandleLogic.handleExchangeShipReject(shipment);
            } else {
                log.info("ERROR-END-HANDLE-SHIPMENT-REJECT-CODE:{} status invalid", shipmentCode);
            }

            log.info("END-HANDLE-SHIPMENT-REJECT-CODE:{}", shipmentCode);
        }

        log.info("END-HANDLE-SHIPMENT-REJECT-API for:{}", url);

    }


    /**
     * 商品打标
     */
    @RequestMapping(value = "/make/flag", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void makeFlag(@RequestParam String fileName) {

        String url = "/pousheng/file/" + fileName + ".csv";

        log.info("START-HANDLE-MAKE-FLAG-API for:{}", url);
        File file1 = new File(url);

        List<String> codes = readShipmentCode(file1);
        log.info("START-HANDLE-MAKE-FLAG-API for:{}", url);
        onImportMposFlag(codes);
        log.info("END-HANDLE-MAKE-FLAG-API for:{}", url);

    }

    private List<String> readShipmentCode(File file) {
        List<String> result = Lists.newArrayList();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));//构造一个BufferedReader类来读取文件
            String s = null;
            while ((s = br.readLine()) != null) { //使用readLine方法，一次读一行
                result.add(s);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private List<String> readOrderIds(File file) {
        List<String> result = Lists.newArrayList();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));//构造一个BufferedReader类来读取文件
            String s = null;
            while ((s = br.readLine()) != null) { //使用readLine方法，一次读一行
                result.add(s);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }


    private void onImportMposFlag(List<String> skuTemplateIds) {

        List<SkuTemplate> skuTemplates = Lists.newArrayList();

        for (int i = 0; i < skuTemplateIds.size(); i++) {

            Long skuTemplateId = Long.valueOf(skuTemplateIds.get(i));
            try {
                //判断商品是否有效
                Response<SkuTemplate> skuTemplateRes = skuTemplateReadService.findById(skuTemplateId);
                if (!skuTemplateRes.isSuccess()) {
                    log.error("make-flag-fail find sku template by id:{} fail,error:{}", skuTemplateId, skuTemplateRes.getError());
                    continue;
                }

                SkuTemplate skuTemplate = skuTemplateRes.getResult();

                //同步电商
                //syncParanaMposSku(skuTemplateId, PsSpuType.MPOS.value());
                Response<Boolean> syncParanaRes = syncParanaMposSku(skuTemplate);
                if (!syncParanaRes.isSuccess()) {
                    log.error("make-flag-fail sync parana mpos item(sku template id:{}) fail", skuTemplateId);
                    continue;
                }

                skuTemplates.add(skuTemplate);

                //每1000条更新下mysql和search
                if (i % 1000 == 0) {
                    //更新es
                    skuTemplateDumpService.batchDump(skuTemplates, 2);
                    //设置默认折扣 和价格
                    pushMposItemComponent.batchMakeFlag(skuTemplates, PsSpuType.MPOS.value());
                    skuTemplates.clear();
                }
            } catch (Exception e) {
                log.error("make-flag-fail import make sku id:{} flag fail, cause:{}", skuTemplateId, Throwables.getStackTraceAsString(e));
            }
        }

        //非1000条的更新下
        if (!CollectionUtils.isEmpty(skuTemplates)) {
            skuTemplateDumpService.batchDump(skuTemplates, 2);
            pushMposItemComponent.batchMakeFlag(skuTemplates, PsSpuType.MPOS.value());
        }
    }

    /**
     * 同步电商
     *
     * @param exist 货品
     */
    private Response<Boolean> syncParanaMposSku(SkuTemplate exist) {
        return pushMposItemComponent.syncParanaMposItem(exist);
    }


    /**
     * 同步mpos映射关系到恒康
     */
    @RequestMapping(value = "/sync/item/mapping", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void autoSyncMposMapping(@RequestParam String fileName) {

        String url = "/pousheng/file/" + fileName + ".csv";

        File file1 = new File(url);

        List<String> itemIdStrs = readShipmentCode(file1);

        log.info("START-HANDLE-SYNC-ITEM-MAPPING-API for:{}", url);

        List<Long> itemIds = Lists.newArrayList();

        int i = 0;
        for (String itemIdStr : itemIdStrs) {
            i++;
            itemIds.add(Long.valueOf(itemIdStr));
            //每1000条更新下mysql和search
            if (i % 1000 == 0) {
                log.info("start to push hk itemIds [{}]", itemIds.toString());
                try {
                    //向库存那边推送这个信息, 表示要关注这个商品对应的单据
                    materialPusher.addSpus(itemIds);
                } catch (Exception e) {
                    log.error("synchronizeShopSpuStock has an error:{}", Throwables.getStackTraceAsString(e));
                }
                itemIds.clear();
            }

        }

        //非1000条的更新下
        if (!CollectionUtils.isEmpty(itemIds)) {
            try {
                //向库存那边推送这个信息, 表示要关注这个商品对应的单据
                materialPusher.addSpus(itemIds);
            } catch (Exception e) {
                log.error("synchronizeShopSpuStock has an error:{}", Throwables.getStackTraceAsString(e));
            }
        }

        log.info("END-HANDLE-SYNC-ITEM-MAPPING-API for:{}", url);

    }

    /**
     * 电商呼叫接单和呼叫快递
     *
     * @param shipmentId
     */
    @RequestMapping(value = "/confirm/vip/delivery", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public void confirmStoreDelivery(@RequestParam Long shipmentId) {
        Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        Response<Boolean> response = syncVIPLogic.syncOrderStoreToVIP(shipment);
        if (!response.isSuccess()) {
            throw new JsonResponseException(response.getError());
        }

    }


    /**
     * 通知oxo下架
     *
     * @param orderId 订单好
     */
    @RequestMapping(value = "/confirm/vip/undercarriage", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public void confirmUnderStore(@RequestParam Long orderId) {
        Long userId = UserUtil.getUserId();

        /*if (userId == null) {
            throw new JsonResponseException("permission.check.current.user.empty");
        }*/
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(orderId);
        /*if (!shopOrder.getStatus().equals(MiddleOrderStatus.WAIT_HANDLE.getValue())) {
            throw new JsonResponseException("current.status.is.not.wait.handle");
        }*/
        if (!shopOrder.getOutFrom().equals(MiddleChannel.VIP.getValue())) {
            throw new JsonResponseException("this.order.channel.is.not.vip");
        }
        log.info("begin to undercarriage to oxo, outId is {} user is {}", shopOrder.getOutId(), userId);
        Response<Boolean> response = syncVIPLogic.confirmUndercarriage(shopOrder.getShopId(), shopOrder.getOutId());
        if (!response.isSuccess()) {
            throw new JsonResponseException(response.getError());
        }
    }


    /**
     * 修复未扣减库存发货单
     *
     * @param fileName
     */
    @RequestMapping(value = "/fix/decrease/stock", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void fixDecreaseStock(@RequestParam String fileName) {
        String url = "/pousheng/file/" + fileName + ".csv";
        File file1 = new File(url);
        List<String> codes = readShipmentCode(file1);

        log.info("START-HANDLE-FIX-DECREASE-STOCK-API for:{} COUNT:{}", url, codes.size());
        int count = 0;
        for (String shipmentCode : codes) {
            Response<Shipment> shipmentRes = shipmentReadService.findShipmentCode(shipmentCode);
            if (!shipmentRes.isSuccess()) {
                log.error("find shipment by code :{} fail,error:{}", shipmentCode, shipmentRes.getError());
                continue;
            }
            Shipment shipment = shipmentRes.getResult();

            Response<Boolean> resp = mposSkuStockLogic.decreaseStock(shipment);
            if (!resp.isSuccess()) {
                log.error("decrease stock by shipment code {} fail,error:{}", shipmentCode, shipmentRes.getError());
                continue;
            } else {
                count++;
            }
        }

        log.info("END-HANDLE-FIX-DECREASE-STOCK-API for:{}, SUCCESS RESULT:{}", url, count);
    }




    /**
     * 修复未占库存发货单
     *
     * @param fileName
     */
    @RequestMapping(value = "/fix/lock/stock", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void fixLockStock(@RequestParam String fileName) {
        String url = "/pousheng/file/" + fileName + ".csv";
        File file1 = new File(url);
        List<String> codes = readShipmentCode(file1);

        log.info("START-HANDLE-FIX-LOCK-STOCK-API for:{} COUNT:{}", url, codes.size());
        int count = 0;
        for (String shipmentCode : codes) {
            Response<Shipment> shipmentRes = shipmentReadService.findShipmentCode(shipmentCode);
            if (!shipmentRes.isSuccess()) {
                log.error("find shipment by code :{} fail,error:{}", shipmentCode, shipmentRes.getError());
                continue;
            }
            Shipment shipment = shipmentRes.getResult();

            Response<Boolean> resp = mposSkuStockLogic.lockStock(shipment,Boolean.FALSE);
            if (!resp.isSuccess()) {
                log.error("lock stock by shipment code {} fail,error:{}", shipmentCode, shipmentRes.getError());
                continue;
            } else {
                count++;
            }
        }

        log.info("END-HANDLE-FIX-LOCK-STOCK-API for:{}, SUCCESS RESULT:{}", url, count);
    }

    /**
     * 手工推送库存变化
     * @param skuCode
     * @param warehouseId
     * @param shopId
     * @return
     */
    @RequestMapping(value = "/fix/inventory/change", method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public String pushInventoryChange(@RequestParam String skuCode, @RequestParam(name = "warehouseId", required = false) Long warehouseId,
                                      @RequestParam(name = "shopId", required = false) Long shopId) {
        log.debug("API-MIDDLE-TASK-FIX-INVENTORY-CHANGE-START param: skuCode [{}] warehouseId:{},shopId:{}", skuCode,
            warehouseId, shopId);
        inventoryChangeProducer.handleInventoryChange(InventoryChangeDTO.builder()
            .skuCode(skuCode)
            .warehouseId(warehouseId)
            .shopId(shopId)
            .build());
        log.debug("API-MIDDLE-TASK-FIX-INVENTORY-CHANGE-END skuCode [{}] warehouseId:{},shopId:{}", skuCode,
            warehouseId, shopId);
        return "ok";
    }

    /**
     * 手工检查发货单里的店铺最大接单量
     * @param shipmentCode
     * @return
     */
    @RequestMapping(value = "/check/shop/max/order/qty", method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public String checkShopMaxAcceptOrderQtyByShipment(@RequestParam String shipmentCode) {
        log.debug("API-MIDDLE-TASK-CHECK-SHOP-MAX-ORDER-QTY-START param: shipmentCode:{}", shipmentCode);
        Response<Shipment> shipmentRes = shipmentReadService.findShipmentCode(shipmentCode);
        if (!shipmentRes.isSuccess()) {
            log.error("find shipment by code :{} fail,error:{}", shipmentCode, shipmentRes.getError());
            return "find shipment error";
        }

        Shipment shipment = shipmentRes.getResult();
        if (!Objects.isNull(shipment)) {
            shopMaxOrderLogic.checkMaxOrderAcceptQty(shipment);
        }
        log.debug("API-MIDDLE-TASK-CHECK-SHOP-MAX-ORDER-QTY-END.");
        return "ok";
    }

    /**
     * 天猫预售订单尾款金额修正
     * @return
     */
    @RequestMapping(value = "/fix/tmall/presale/fee", method = RequestMethod.GET,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public String fixTmallPresaleOrderPrice(){
        log.info("start fix presale orders");
        Integer pageNo = 1;
        Integer pageSize = 500;
        while (true) {
            log.info("start to fix presale order ,pageNo =====> {}",pageNo);
            PoushengCompensateBizCriteria criteria = new PoushengCompensateBizCriteria();
            criteria.setPageNo(pageNo);
            criteria.setPageSize(pageSize);
            criteria.setBizType(PoushengCompensateBizType.STEP_ORDER_NOTIFY_ERP.name());
            Response<Paging<Long>> response = compensateBizReadService.pagingIds(criteria);
            if (!response.isSuccess()) {
                pageNo++;
                continue;
            }
            List<Long> compensateBizIds = response.getResult().getData();
            if (compensateBizIds.isEmpty()) {
                break;
            }
            log.info("wait handle compensateBizIds size is {}",compensateBizIds.size());
            //轮询业务处理
            for (Long compensateBizId : compensateBizIds) {
                Response<PoushengCompensateBiz> result = compensateBizReadService.findById(compensateBizId);
                if (!result.isSuccess()){
                    continue;
                }
                PoushengCompensateBiz compensateBiz = result.getResult();
                String shopOrderId = compensateBiz.getBizId();
                //多线程处理
                this.fixTmallPresaleFee(shopOrderId);
            }
            pageNo++;
        }
        log.info("end fix presale orders");
        return "ok";
    }

    /**
     * 多线程处理，加快速度
     * @param shopOrderId
     */
    private void fixTmallPresaleFee(final String shopOrderId){
        fixTmallFeeService.submit(()->{
            try{
                ShopOrder shopOrder = orderReadLogic.findShopOrderById(Long.valueOf(shopOrderId));
                //如果不是淘宝渠道的预售单，那么直接过滤掉
                if (!Objects.equals(shopOrder.getOutFrom(),"taobao")){
                    log.warn("this is jd presale order ,orderCode {}",shopOrder.getOrderCode());
                    return;
                }
                log.info("start to fix sku order amt ,shopOrderCode is {}",shopOrder.getOrderCode());
                //更新子订单的discount
                List<SkuOrder> skuOrders = orderReadLogic.findSkuOrdersByShopOrderId(shopOrder.getId());
                for (SkuOrder skuOrder : skuOrders) {
                    if (skuOrder.getOriginFee()>0){
                        SkuOrder newSkuOrder = new SkuOrder();
                        newSkuOrder.setId(skuOrder.getId());
                        newSkuOrder.setDiscount(Long.valueOf(shopOrder.getDiscount()));
                        Response<Boolean> skuOrderR = middleOrderWriteService.updateSkuOrder(newSkuOrder);
                        if (!skuOrderR.isSuccess()) {
                            log.error("skuOrder failed,id is", newSkuOrder.getId());
                        }

                    }
                }
                //更新发货单金额
                shipmentWiteLogic.updateShipmentFee(shopOrder.getId());

            }catch (Exception e){
                log.error("fix tmall presale amt failed,caused by {}",Throwables.getStackTraceAsString(e));
            }
        });
    }
}
