package com.pousheng.middle.web.erp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.pousheng.erp.cache.ErpBrandCacher;
import com.pousheng.erp.component.BrandImporter;
import com.pousheng.erp.component.MposWarehousePusher;
import com.pousheng.erp.component.SpuImporter;
import com.pousheng.erp.model.PoushengMaterial;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.item.dto.ItemNameAndStock;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.enums.PsSpuType;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.warehouse.model.MposSkuStock;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import com.pousheng.middle.warehouse.service.MposSkuStockReadService;
import com.pousheng.middle.warehouse.service.WarehouseSkuReadService;
import com.pousheng.middle.web.warehouses.component.WarehouseImporter;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.common.utils.Splitters;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import io.terminus.parana.brand.model.Brand;
import io.terminus.parana.search.dto.SearchedItemWithAggs;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
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

    private final ErpBrandCacher brandCacher;

    private final MposWarehousePusher mposWarehousePusher;

    private final QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;

    private final WarehouseSkuReadService warehouseSkuReadService;

    private final MposSkuStockReadService mposSkuStockReadService;

    @RpcConsumer
    private SkuTemplateSearchReadService skuTemplateSearchReadService;


    private final DateTimeFormatter dft;
    @RpcConsumer
    private MappingReadService mappingReadService;
    @Autowired
    public FireCall(SpuImporter spuImporter, BrandImporter brandImporter,
                    WarehouseImporter warehouseImporter, ErpBrandCacher brandCacher, MposWarehousePusher mposWarehousePusher, QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi, WarehouseSkuReadService warehouseSkuReadService, MposSkuStockReadService mposSkuStockReadService) {
        this.spuImporter = spuImporter;
        this.brandImporter = brandImporter;
        this.warehouseImporter = warehouseImporter;
        this.brandCacher = brandCacher;
        this.mposWarehousePusher = mposWarehousePusher;
        this.queryHkWarhouseOrShopStockApi = queryHkWarhouseOrShopStockApi;
        this.warehouseSkuReadService = warehouseSkuReadService;
        this.mposSkuStockReadService = mposSkuStockReadService;

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
    public String synchronizeWarehouse(@RequestParam String start,
                                       @RequestParam(name = "end", required = false) String end){
        Date from = dft.parseDateTime(start).toDate();
        Date to = new Date();
        if (StringUtils.hasText(end)) {
            to = dft.parseDateTime(end).toDate();
        }
        log.info("begin to synchronize warehouse from {} to {}", start, end);
        int warehouseCount = warehouseImporter.process(from, to);
        log.info("synchronized {} warehouses", warehouseCount);
        return "ok";

    }

    @RequestMapping(value = "/spu/by/sku/code", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String synchronizeSpuByBarCode(@RequestParam String skuCode){
        int spuCount =spuImporter.processPullMarterials(skuCode);
        log.info("synchronized {} spus", spuCount);
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
                    Long lockStock = findWarehouseSkuStockLockQuantity(hkSkuStockInfo.getBusinessId(),skuAndQuantityInfo.getBarcode());
                    total+=skuAndQuantityInfo.getQuantity()-lockStock;
                }
            }else {
                for (HkSkuStockInfo.SkuAndQuantityInfo skuAndQuantityInfo : hkSkuStockInfo.getMaterial_list()){
                    Long lockStock = findMposSkuStockLockQuantity(hkSkuStockInfo.getBusinessId(),skuAndQuantityInfo.getBarcode());
                    total+=skuAndQuantityInfo.getQuantity()-lockStock;
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
            Response<Paging<ItemMapping>> r =  mappingReadService.findByOpenShopId(openShopId,pageNo,pageSize);
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

}


