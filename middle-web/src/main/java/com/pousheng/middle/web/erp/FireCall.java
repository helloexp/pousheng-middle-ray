package com.pousheng.middle.web.erp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.erp.cache.ErpBrandCacher;
import com.pousheng.erp.component.BrandImporter;
import com.pousheng.erp.component.MaterialPusher;
import com.pousheng.erp.component.MposWarehousePusher;
import com.pousheng.erp.component.SpuImporter;
import com.pousheng.erp.model.PoushengMaterial;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.item.constant.PsItemConstants;
import com.pousheng.middle.item.dto.ItemNameAndStock;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.enums.PsSpuType;
import com.pousheng.middle.item.service.SkuTemplateDumpService;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.open.StockPusher;
import com.pousheng.middle.shop.dto.ShopExtraInfo;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.model.MposSkuStock;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import com.pousheng.middle.warehouse.service.MposSkuStockReadService;
import com.pousheng.middle.warehouse.service.WarehouseSkuReadService;
import com.pousheng.middle.web.events.item.BatchAsyncImportMposFlagEvent;
import com.pousheng.middle.web.events.trade.listener.AutoCreateShipmetsListener;
import com.pousheng.middle.web.item.batchhandle.AbnormalRecord;
import com.pousheng.middle.web.item.batchhandle.ExcelExportHelper;
import com.pousheng.middle.web.item.batchhandle.ExcelUtil;
import com.pousheng.middle.web.item.component.PushMposItemComponent;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import com.pousheng.middle.web.order.component.ShipmentWiteLogic;
import com.pousheng.middle.web.warehouses.component.WarehouseImporter;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.common.utils.Splitters;
import io.terminus.open.client.center.event.OpenClientOrderSyncEvent;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.order.model.OrderBase;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.search.dto.SearchedItemWithAggs;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    private final WarehouseSkuReadService warehouseSkuReadService;

    private final MposSkuStockReadService mposSkuStockReadService;

    @RpcConsumer
    private SkuTemplateSearchReadService skuTemplateSearchReadService;
    private final ShopCacher shopCacher;
    private final WarehouseCacher warehouseCacher;


    private final DateTimeFormatter dft;
    @RpcConsumer
    private MappingReadService mappingReadService;
    @Autowired
    private StockPusher stockPusher;
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
    public FireCall(SpuImporter spuImporter, BrandImporter brandImporter,
                    WarehouseImporter warehouseImporter, MaterialPusher materialPusher, ErpBrandCacher brandCacher, MposWarehousePusher mposWarehousePusher, QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi, WarehouseSkuReadService warehouseSkuReadService, MposSkuStockReadService mposSkuStockReadService, SkuTemplateSearchReadService skuTemplateSearchReadService, ShopCacher shopCacher, WarehouseCacher warehouseCacher) {
        this.spuImporter = spuImporter;
        this.brandImporter = brandImporter;
        this.warehouseImporter = warehouseImporter;
        this.materialPusher = materialPusher;
        this.brandCacher = brandCacher;
        this.mposWarehousePusher = mposWarehousePusher;
        this.queryHkWarhouseOrShopStockApi = queryHkWarhouseOrShopStockApi;
        this.warehouseSkuReadService = warehouseSkuReadService;
        this.mposSkuStockReadService = mposSkuStockReadService;
        this.skuTemplateSearchReadService = skuTemplateSearchReadService;
        this.shopCacher = shopCacher;
        this.warehouseCacher = warehouseCacher;

        DateTimeParser[] parsers = {
                DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").getParser(),
                DateTimeFormat.forPattern("yyyy-MM-dd").getParser()};
        dft = new DateTimeFormatterBuilder().append(null, parsers).toFormatter();
    }


    @RequestMapping(value="/spu/import", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String spuInput(@RequestParam String file){
        try {
            ObjectMapper mapper = JsonMapper.nonEmptyMapper().getMapper();
            String path = "/Users/songrenfei/Downloads/";
            path=path+file+".txt";

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
                                   @RequestParam(name = "end", required = false) String end){
        Date from = dft.parseDateTime(start).toDate();
        Date to = new Date();
        if (StringUtils.hasText(end)) {
            to = dft.parseDateTime(end).toDate();
        }
        int cardCount = brandImporter.process(from, to);
        log.info("synchronized {} brands", cardCount);
        return "ok";
    }


    @RequestMapping(value = "/spu", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String synchronizeSpu(@RequestParam String start,
                                 @RequestParam(name = "end", required = false) String end) {

        Date from = dft.parseDateTime(start).toDate();
        Date to = new Date();
        if (StringUtils.hasText(end)) {
            to = dft.parseDateTime(end).toDate();
        }
        //log.info("synchronize brand first");
        //int cardCount = brandImporter.process(from, to);
        //log.info("synchronized {} brands", cardCount);
        int spuCount =spuImporter.process(from, to);
        log.info("synchronized {} spus", spuCount);
        return "ok";
    }


    @RequestMapping(value="/warehouse", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean synchronizeWarehouse(@RequestParam(name = "start",required = false,defaultValue = "") String start,
                                         @RequestParam(name = "end", required = false,defaultValue = "") String end){
        Date from= DateTime.now().withTimeAtStartOfDay().toDate();
        if (StringUtils.hasText(start)){
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
    public String synchronizeSpuStock(@RequestParam Long spuId){
        //向库存那边推送这个信息, 表示要关注这个商品对应的单据
        materialPusher.addSpus(Lists.newArrayList(spuId));
        //调用恒康抓紧给我返回库存信息
        materialPusher.pushItemForStock(spuId);
        return "ok";
    }

    @RequestMapping(value="/add/mpos/warehouse", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String syncMposWarehouse(@RequestParam String companyId,
                                       @RequestParam String stockId){
        mposWarehousePusher.addWarehouses(companyId,stockId);
        return "ok";
    }

    @RequestMapping(value="/del/mpos/warehouse", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String delMposWarehouse(@RequestParam String companyId,
                                    @RequestParam String stockId){
        mposWarehousePusher.removeWarehouses(companyId,stockId);
        return "ok";
    }

    @RequestMapping(value = "/spu/by/sku/code", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String synchronizeSpuByBarCode(@RequestParam String skuCode){
        int spuCount =spuImporter.processPullMarterials(skuCode);
        log.info("synchronized {} spus", spuCount);
        return "ok";
    }

    @RequestMapping(value="/query/stock", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<HkSkuStockInfo> queryStock(@RequestParam(required = false) String stockCodes,
                                           @RequestParam String skuCodes,
                                           @RequestParam(required = false,defaultValue = "0") Integer stockType){
        List<String> stockCodesList = null;
        if(!Strings.isNullOrEmpty(stockCodes)){
            stockCodesList = Splitters.COMMA.splitToList(stockCodes);
        }
        List<String> skuCodesList = Splitters.COMMA.splitToList(skuCodes);
        return queryHkWarhouseOrShopStockApi.doQueryStockInfo(stockCodesList,skuCodesList,stockType);
    }



    @RequestMapping(value="/count/stock", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long countStock(@RequestParam(required = false) String stockCodes,
                                           @RequestParam String skuCodes,
                                           @RequestParam(required = false,defaultValue = "0") Integer stockType){
        Long total = 0L;
        List<String> stockCodesList = null;
        if(!Strings.isNullOrEmpty(stockCodes)){
            stockCodesList = Splitters.COMMA.splitToList(stockCodes);
        }
        List<String> skuCodesList = Splitters.COMMA.splitToList(skuCodes);
        List<HkSkuStockInfo> skuStockInfos = queryHkWarhouseOrShopStockApi.doQueryStockInfo(stockCodesList,skuCodesList,stockType);
        for (HkSkuStockInfo hkSkuStockInfo : skuStockInfos){
            for (HkSkuStockInfo.SkuAndQuantityInfo skuAndQuantityInfo : hkSkuStockInfo.getMaterial_list()){
                total+=skuAndQuantityInfo.getQuantity();
            }
        }
        return total;
    }


    /**
     * 根据货号和尺码查询
     * @param materialId 货号
     * @param size 尺码
     * @return 商品信息
     */
    @RequestMapping(value="/count/stock/for/mpos", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ItemNameAndStock countStock(@RequestParam String materialId, @RequestParam String size){
        //1、根据货号和尺码查询 spuCode=20171214001&attrs=年份:2017
        String templateName = "search.mustache";
        Map<String,String> params = Maps.newHashMap();
        params.put("type", String.valueOf(PsSpuType.MPOS.value()));
        params.put("spuCode",materialId);
        params.put("attrs","尺码:"+size);
        Response<? extends SearchedItemWithAggs<SearchSkuTemplate>> response =skuTemplateSearchReadService.searchWithAggs(1,20, templateName, params, SearchSkuTemplate.class);
        if(!response.isSuccess()){
            log.error("query sku template by materialId:{} and size:{} fail,error:{}",materialId,size,response.getError());
            throw new JsonResponseException(response.getError());
        }

        List<SearchSkuTemplate> searchSkuTemplates = response.getResult().getEntities().getData();
        if(CollectionUtils.isEmpty(searchSkuTemplates)){
            log.error("middle not find sku template by materialId:{} and size:{} ",materialId,size);
            return new ItemNameAndStock();
        }
        SearchSkuTemplate searchSkuTemplate = searchSkuTemplates.get(0);
        ItemNameAndStock itemNameAndStock = new ItemNameAndStock();
        itemNameAndStock.setName(searchSkuTemplate.getName());
        String skuCode = searchSkuTemplate.getSkuCode();
        Long total = 0L;
        List<String> skuCodesList = Splitters.COMMA.splitToList(skuCode);
        List<HkSkuStockInfo> skuStockInfos = queryHkWarhouseOrShopStockApi.doQueryStockInfo(null,skuCodesList,0);
        for (HkSkuStockInfo hkSkuStockInfo : skuStockInfos){
            //仓
            if(com.google.common.base.Objects.equal(2,Integer.valueOf(hkSkuStockInfo.getStock_type()))){
                for (HkSkuStockInfo.SkuAndQuantityInfo skuAndQuantityInfo : hkSkuStockInfo.getMaterial_list()){
                    Warehouse warehouse = warehouseCacher.findById(hkSkuStockInfo.getBusinessId());
                    Map<String,String> extra = warehouse.getExtra();
                    Integer safeStock =0;
                    if(!CollectionUtils.isEmpty(extra)&&extra.containsKey("safeStock")){
                        //安全库存
                        safeStock = Integer.valueOf(extra.get("safeStock"));
                    }

                    //锁定库存
                    Long lockStock = findWarehouseSkuStockLockQuantity(hkSkuStockInfo.getBusinessId(),skuAndQuantityInfo.getBarcode());

                    total+=skuAndQuantityInfo.getQuantity()-lockStock-safeStock;
                }
            //店
            }else {
                for (HkSkuStockInfo.SkuAndQuantityInfo skuAndQuantityInfo : hkSkuStockInfo.getMaterial_list()){
                    //锁定库存
                    Long lockStock = findMposSkuStockLockQuantity(hkSkuStockInfo.getBusinessId(),skuAndQuantityInfo.getBarcode());
                    Shop shop = shopCacher.findShopById(hkSkuStockInfo.getBusinessId());
                    ShopExtraInfo shopExtraInfo = ShopExtraInfo.fromJson(shop.getExtra());
                    //安全库存
                    Integer safeStock = Arguments.isNull(shopExtraInfo.getSafeStock())?0:shopExtraInfo.getSafeStock();
                    total+=skuAndQuantityInfo.getQuantity()-lockStock-safeStock;
                }
            }

        }
        itemNameAndStock.setStockQuantity(total);
        return itemNameAndStock;
    }


    private Long findWarehouseSkuStockLockQuantity(Long warehouseId,String skuCode){
        Response<WarehouseSkuStock> response =  warehouseSkuReadService.findByWarehouseIdAndSkuCode(warehouseId,skuCode);
        if(!response.isSuccess()){
            log.error("find warehouse sku stock by warehouse id:{} sku code:{} fail,error:{}",warehouseId,skuCode,response.getError());
            return 0L;
        }
        Long lockQuantity =  response.getResult().getLockedStock();
        if(Arguments.isNull(lockQuantity)){
            return 0L;
        }
        return lockQuantity;
    }

    private Long findMposSkuStockLockQuantity(Long shopId, String skuCode){
        Response<Optional<MposSkuStock>> response = mposSkuStockReadService.findByShopIdAndSkuCode(shopId,skuCode);
        if(!response.isSuccess()){
            log.error("find mpos sku sotck by shop id:{} sku code:{} fail,error:{}",shopId,skuCode,response.getError());
            return 0L;
        }
        Optional<MposSkuStock> stockOptional = response.getResult();
        if(!stockOptional.isPresent()){
            return 0L;
        }
        Long lockQuantity =  stockOptional.get().getLockedStock();
        if(Arguments.isNull(lockQuantity)){
            return 0L;
        }
        return lockQuantity;

    }




    /**
     * 根据店铺id拉取基础货品信息
     * @param openShopId
     * @return
     */
    @RequestMapping(value = "/sku/code/by/shop", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String synchronizeSpuByBarCode(@RequestParam Long openShopId){
        int pageNo = 0;
        int pageSize= 40;
        while(true){
            Response<Paging<ItemMapping>> r =  mappingReadService.findByOpenShopId(openShopId,null,pageNo,pageSize);
            Paging<ItemMapping> itemMappingPaging = r.getResult();
            List<ItemMapping> itemMappingList = itemMappingPaging.getData();
            if (itemMappingList.isEmpty()){
                break;
            }
            for (ItemMapping itemMapping:itemMappingList){
                if (!Objects.equals(itemMapping.getStatus(),-1)){
                    int spuCount =spuImporter.processPullMarterials(itemMapping.getSkuCode());
                    log.info("synchronized {} spus", spuCount);
                }
            }
            pageNo++;
        }
        return "ok";
    }


    /**
     * 根据skuCode推送库存到第三方或者官网
     * @param skuCode
     * @return
     */
    @RequestMapping(value = "/sync/stock/by/sku/code", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String synchronizeStockBySkuCode(@RequestParam String skuCode){
        stockPusher.submit(Lists.newArrayList(skuCode));
        return "ok";
    }

    public static String readFile(File file){
        StringBuilder result = new StringBuilder();
        try{
            BufferedReader br = new BufferedReader(new FileReader(file));//构造一个BufferedReader类来读取文件
            String s = null;
            while((s = br.readLine())!=null){//使用readLine方法，一次读一行
                result.append(System.lineSeparator()+s);
            }
            br.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        return result.toString();
    }

    /**
     * 修复skuTemplate表中
     * @return
     */
    @RequestMapping(value = "/sku/extra/restore", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String skuTemplateExtraRestore(@RequestParam(required = false, value = "skuCode") String skuCode,
                                          @RequestParam(required = false, value = "type") Integer type,
                                          @RequestParam(required = false, value = "pageNo") Integer pageNo,
                                          @RequestParam(required = false, value = "pageSize") Integer pageSize){
        boolean result = spuImporter.skuTemplateExtraRestore(skuCode,type,pageNo,pageSize);
        if (result){
            return  "ok";
        }else{
            return  "not ok";
        }
    }


    /**
     * 取消发货单
     */
    @RequestMapping(value = "/cancel/shipment", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void cancelShipments(@RequestParam String fileName){

        String url = "/pousheng/file/"+ fileName + ".csv";

        File file1 = new File(url);

        List<String> codes =  readShipmentCode(file1);

        log.info("START-HANDLE-SHIPMENT-API for:{}",url);

        for (String code : codes){
            log.info("START-HANDLE-SHIPMENT-CODE:{}",code);

            Response<Shipment> shipmentRes = shipmentReadService.findShipmentCode(code);
            if (!shipmentRes.isSuccess()) {
                log.error("find shipment by code :{} fail,error:{}",code,shipmentRes.getError());
                continue;
            }

            Shipment shipment = shipmentRes.getResult();
            if (!Objects.equals(shipment.getStatus(),4)){
                log.error("shipment code:{} status is :{} so skip cancel",code,shipment.getStatus());
                continue;
            }
            //to cancel
            log.info("try to cancel shipment, shipment code is {}",code);
            Response<Boolean> response = shipmentWiteLogic.rollbackShipment(shipment);
            if (!response.isSuccess()){
                log.info("try to cancel shipment fail, shipment code is {},error:{}", code,response.getError());
                continue;
            }

            log.info("END-HANDLE-SHIPMENT-CODE:{}",code);
        }

        log.info("END-HANDLE-SHIPMENT-API for:{}",url);

    }



    /**
     * 创建发货单
     */
    @RequestMapping(value = "/create/shipment", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void autoShipments(@RequestParam String fileName){

        String url = "/pousheng/file/"+ fileName + ".csv";

        File file1 = new File(url);

        List<String> codes =  readShipmentCode(file1);

        log.info("START-HANDLE-CREATE-SHIPMENT-API for:{}",url);

        for (String code : codes){
            log.info("START-HANDLE-CREATE-SHIPMENT-CODE:{}",code);

            Response<Shipment> shipmentRes = shipmentReadService.findShipmentCode(code);
            if (!shipmentRes.isSuccess()) {
                log.error("find shipment by code :{} fail,error:{}",code,shipmentRes.getError());
                continue;
            }

            Shipment shipment = shipmentRes.getResult();

            OrderShipment orderShipment = shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());

            OrderBase orderBase = orderReadLogic.findOrder(orderShipment.getOrderId(), OrderLevel.SHOP);
            if (!Objects.equals(orderBase.getStatus(),1)){
                log.error("orderBase id:{} status is :{} so skip create shipment by shipment code:{}",orderBase.getId(),orderBase.getStatus(),code);
                continue;
            }
            //to create shipment
            log.info("try to create shipment, shipment code is {} order id:{}",code,orderBase.getId());
            OpenClientOrderSyncEvent event = new OpenClientOrderSyncEvent(orderBase.getId());
            try {
                autoCreateShipmetsListener.onShipment(event);
            } catch (Exception e){
                log.info("fail to create shipment, shipment code is {} order id:{}",code,orderBase.getId());
            }

            log.info("END-HANDLE-CREATE-SHIPMENT-CODE:{}",code);
        }

        log.info("END-HANDLE-CREATE-SHIPMENT-API for:{}",url);

    }


    /**
     * 修复shutemplate中extra为空或者缺少货号的数据
     */
    @RequestMapping(value = "/fix/skuTemplate", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void fixSkutemplate(@RequestParam String fileName){

        String url = "/pousheng/file/"+ fileName + ".csv";
        File file1 = new File(url);

        List<String> skuCodes =  readShipmentCode(file1);

        log.info("START-HANDLE-SKUTEMPLATE-API for:{}",url);

        for (String skuCode : skuCodes){
            log.info("START-HANDLE-SKUTEMPLATE-CODE:{}",skuCode);
            if ("\"sku_code\"".equals(skuCode)){
                continue;
            }
            spuImporter.dealFixSkuTemplateBySkucode(skuCode);
            log.info("END-HANDLE-SKUTEMPLATE-CODE:{}",skuCode);
        }

        log.info("END-HANDLE-SKUTEMPLATE-API for:{}",url);

    }



    /**
     * 商品打标
     */
    @RequestMapping(value = "/make/flag", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void makeFlag(@RequestParam String fileName){

        String url = "/pousheng/file/"+ fileName + ".csv";

        File file1 = new File(url);

        List<String> codes =  readShipmentCode(file1);

        log.info("START-HANDLE-MAKE-FLAG-API for:{}",url);

        onImportMposFlag(codes);


        log.info("END-HANDLE-MAKE-FLAG-API for:{}",url);

    }

    private  List<String> readShipmentCode(File file){
        List<String> result = Lists.newArrayList();
        try{
            BufferedReader br = new BufferedReader(new FileReader(file));//构造一个BufferedReader类来读取文件
            String s = null;
            while ((s = br.readLine()) != null){ //使用readLine方法，一次读一行
                result.add(s);
            }
            br.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }



    private void onImportMposFlag(List<String> skuTemplateIds) {

        List<SkuTemplate> skuTemplates = Lists.newArrayList();

        for (int i = 0;i< skuTemplateIds.size();i++) {

            Long  skuTemplateId = Long.valueOf(skuTemplateIds.get(i));
            try {
                //判断商品是否有效
                Response<SkuTemplate> skuTemplateRes = skuTemplateReadService.findById(skuTemplateId);
                if( !skuTemplateRes.isSuccess()){
                    log.error("make-flag-fail find sku template by id:{} fail,error:{}",skuTemplateId,skuTemplateRes.getError());
                    continue;
                }

                SkuTemplate skuTemplate = skuTemplateRes.getResult();

                //同步电商
                //syncParanaMposSku(skuTemplateId, PsSpuType.MPOS.value());
                Response<Boolean> syncParanaRes = syncParanaMposSku(skuTemplate);
                if( !syncParanaRes.isSuccess()){
                    log.error("make-flag-fail sync parana mpos item(sku template id:{}) fail",skuTemplateId);
                    continue;
                }

                skuTemplates.add(skuTemplate);

                //每1000条更新下mysql和search
                if (i % 1000 == 0) {
                    //更新es
                    skuTemplateDumpService.batchDump(skuTemplates,2);
                    //设置默认折扣 和价格
                    pushMposItemComponent.batchMakeFlag(skuTemplates,PsSpuType.MPOS.value());
                    skuTemplates.clear();
                }
            } catch (Exception e){
                log.error("make-flag-fail import make sku id:{} flag fail, cause:{}",skuTemplateId,Throwables.getStackTraceAsString(e));
            }
        }

        //非1000条的更新下
        if (!CollectionUtils.isEmpty(skuTemplates)){
            skuTemplateDumpService.batchDump(skuTemplates,2);
            pushMposItemComponent.batchMakeFlag(skuTemplates,PsSpuType.MPOS.value());
        }
    }

    /**
     * 同步电商
     * @param exist 货品
     */
    private Response<Boolean> syncParanaMposSku(SkuTemplate exist){
        return pushMposItemComponent.syncParanaMposItem(exist);
    }

}


