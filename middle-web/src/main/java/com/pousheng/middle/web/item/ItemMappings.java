package com.pousheng.middle.web.item;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.pousheng.erp.model.SpuMaterial;
import com.pousheng.erp.service.SpuMaterialReadService;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.order.dto.PoushengCompensateBizCriteria;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import com.pousheng.middle.warehouse.companent.WarehouseShopRuleClient;
import com.pousheng.middle.warehouse.dto.ShopStockRule;
import com.pousheng.middle.web.item.component.CalculateRatioComponent;
import com.pousheng.middle.web.item.dto.ItemMappingInfo;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.open.client.center.MappingServiceRegistryCenter;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.OpenClientItemMappingService;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;


/**
 * Description: 商品映射推送比例设置
 * User: support 9
 * Date: 2018/8/31
 */
@Api(description = "商品映射API")
@RestController
@RequestMapping("/api/open-client")
@Slf4j
public class ItemMappings {

    private PoushengCompensateBizReadService poushengCompensateBizReadService;

    @Autowired
    private WarehouseShopRuleClient warehouseShopRuleClient;
    @Autowired
    private CalculateRatioComponent calculateRatioComponent;
    @Autowired
    private SpuMaterialReadService spuMaterialReadService;
    @Autowired
    private SkuTemplateReadService skuTemplateReadService;
    @Autowired
    private OpenShopCacher openShopCacher;

    private MappingServiceRegistryCenter mappingService;


    @Autowired
    public ItemMappings(MappingServiceRegistryCenter mappingService,
                        PoushengCompensateBizReadService poushengCompensateBizReadService) {
        this.mappingService = mappingService;
        this.poushengCompensateBizReadService = poushengCompensateBizReadService;

    }
    @Autowired
    private CompensateBizLogic compensateBizLogic;

    @ApiOperation("设置商品推送比例")
    @PostMapping(value = "/item-mapping/{id}/ratio", produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("设置商品推送比例")
    public boolean updateSkuCode(@PathVariable("id") Long id,
                                 @RequestParam(required = false) String ratio) {
        if (log.isDebugEnabled()) {
            log.debug("update item-mapping(id:{}) set ratio:{}", id, ratio);
        }
        Integer rat = null;
        try {
            if (!StringUtils.isEmpty(ratio)) {
                rat = Integer.valueOf(ratio);
            }
        } catch (NumberFormatException nfe) {
            log.error("ratio:{} is not number", ratio);
            throw new JsonResponseException("ratio.setting.illegal");
        }
        if (Objects.nonNull(rat) && (rat <= 0 || rat > 100)) {
            log.error("fail to setting ratio:{}", ratio);
            throw new JsonResponseException("ratio.setting.illegal");
        }
        OpenClientItemMappingService itemService = mappingService.getItemService(null);
        Response<Boolean> response = itemService.updatePushRatio(id, rat);
        if (!response.getResult()) {
            log.error("fail to update item-mapping(id:{}) ratio:{}, cause: {}", id, ratio, response.getError());
            throw new JsonResponseException(response.getError());
        }
        if (log.isDebugEnabled()) {
            log.debug("update item-mapping(id:{}) set ratio:{}, res:{}", id, ratio, response.getResult());
        }
        return response.getResult();
    }

    @ApiOperation("批量导入设置商品推送比例")
    @PostMapping(value = "/item-mapping/import")
    @OperationLogType("批量导入设置商品推送比例")
    public void importFile(String url) {
        if (StringUtils.isEmpty(url)) {
            log.error("faild import file(path:{}) ,cause: file path is empty", url);
            throw new JsonResponseException("file.path.is.empty");
        }
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.IMPORT_ITEM_PUSH_RATIO.toString());
        biz.setContext(url);
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        compensateBizLogic.createBizAndSendMq(biz,MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC);
    }

    /**
     * 查询导入文件的处理记录
     *
     * @param pageNo 第几页
     * @param pageSize 分页大小
     * @return 查询结果
     */
    @ApiOperation("查询导入文件的处理记录")
    @RequestMapping(value = "/import/result/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("查询导入文件的处理记录")
    public Paging<PoushengCompensateBiz> create(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                                @RequestParam(required = false, value = "pageSize") Integer pageSize) {
        PoushengCompensateBizCriteria criteria = new PoushengCompensateBizCriteria();
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);
        criteria.setBizType(PoushengCompensateBizType.IMPORT_ITEM_PUSH_RATIO.name());
        Response<Paging<PoushengCompensateBiz>> response = poushengCompensateBizReadService.pagingForShow(criteria);
        if (!response.isSuccess()) {
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    @ApiOperation("查询店铺相同条码不同宝贝的推送比例")
    @RequestMapping(value = "/shop/sku/ratio", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ItemMapping> queryShopSkuRatio(@RequestParam Long shopId,
                                                @RequestParam String skuCode) {
        String channel = null;
        if (shopId != null) {
            OpenShop openShop = openShopCacher.findById(shopId);
            if (openShop != null) {
                channel = openShop.getChannel();
            }
        }
        OpenClientItemMappingService itemService = mappingService.getItemService(channel);
        Response<List<ItemMapping>> response = itemService.listBySkuCodeAndOpenShopId(skuCode,shopId);
        if (!response.isSuccess()){
           log.error("find item mapping by sku code:{} and open shop id:{} fail,error:{}",skuCode,shopId,response.getError());
            throw new JsonResponseException(response.getError());
        }

        ShopStockRule shopStockRule = warehouseShopRuleClient.findByShopId(shopId);

        List<ItemMapping> itemMappings = response.getResult();

        for (ItemMapping itemMapping : itemMappings){
            itemMapping.setRatio(calculateRatioComponent.getRatio(itemMapping,shopStockRule));
        }

        return itemMappings;

    }



    @ApiOperation("商品映射分页查询")
    @GetMapping(value = "/item-mapping/pousheng", produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<ItemMappingInfo> findItemMapping(@RequestParam(value = "itemId", required = false) @ApiParam("中台商品ID") Long itemId,
                                                   @RequestParam(value = "spuCode", required = false)@ApiParam("货号") String spuCode,
                                                   @RequestParam(value = "itemName", required = false)@ApiParam("电商商品名称") String itemName,
                                                   @RequestParam(value = "skuCode", required = false)@ApiParam("条码") String skuCode,
                                                   @RequestParam(value = "channelItemId", required = false)@ApiParam("电商商品货号") String channelItemId,
                                                   @RequestParam(value = "channelSkuId", required = false)@ApiParam("电商商品SKU") String channelSkuId,
                                                   @RequestParam(value = "openShopId", required = false)@ApiParam("第三方店铺") Long openShopId,
                                                   @RequestParam(value = "status", required = false)@ApiParam("状态") Integer status,
                                                   @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
                                                   @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {

        //判断货号是否为空，不为空则将货号转化为对应的itemId
        if (!Strings.isNullOrEmpty(spuCode)){

            Response<Optional<SpuMaterial>> materialRes = spuMaterialReadService.findbyMaterialCode(spuCode);
            if (!materialRes.isSuccess()){
                log.error("find spu material by material code:{} fail,error:{}",skuCode,materialRes.getError());
                return Paging.empty();
            }

            Optional<SpuMaterial> optional = materialRes.getResult();

            if (!optional.isPresent()){
                log.warn("not find spu material by material code:{} fail",skuCode);
                return Paging.empty();
            }

            itemId = optional.get().getSpuId();
        }
        String channel = null;
        if (openShopId != null) {
            OpenShop openShop = openShopCacher.findById(openShopId);
            if (openShop != null) {
                channel = openShop.getChannel();
            }
        }
        OpenClientItemMappingService itemService = mappingService.getItemService(channel);
        Response<Paging<ItemMapping>> findR = itemService.findBy(openShopId, status, itemId, itemName, skuCode,
                channelItemId, channelSkuId, pageNo, pageSize);
        if (!findR.isSuccess()) {
            log.error("fail to find item mapping by openShopId={},status={},itemId={},itemName={},skuCode={},channelItemId={},channelSkuId={},pageNo={}.pageSize={},cause:{}",
                    openShopId, status, itemId, itemName, skuCode, channelItemId, channelSkuId, pageNo, pageSize, findR.getError());
            throw new JsonResponseException(findR.getError());
        }
        return settingRatioAndMaterialCode(findR.getResult(),spuCode);
    }

    private Paging<ItemMappingInfo> settingRatioAndMaterialCode(Paging<ItemMapping> result,String spuCode) {

        Paging<ItemMappingInfo> infoPaging = new Paging<>();
        infoPaging.setTotal(result.getTotal());
        List<ItemMappingInfo> itemMappingInfos = Lists.newArrayListWithCapacity(result.getData().size());

        for (ItemMapping itemMapping : result.getData()){

            ItemMappingInfo info = new ItemMappingInfo();
            BeanUtils.copyProperties(itemMapping, info);

            //接下来的逻辑说明存在多条映射记录
            //店铺库存推送规则是否启用
            ShopStockRule shopStockRule = warehouseShopRuleClient.findByShopId(itemMapping.getOpenShopId());
            //店铺推送比例是否存
            if (null == shopStockRule){
                log.error("not find shop rule by shop id:{}",itemMapping.getOpenShopId());
                info.setIsAverageRatio(Boolean.FALSE);//如果没有设置店铺规则则默认为false，正常情况下都会设置
                if (Arguments.isNull(itemMapping.getRatio())){
                    info.setRatio(100);
                }
            } else {

                info.setRatio(calculateRatioComponent.getRatio(itemMapping,shopStockRule));
                info.setIsAverageRatio(shopStockRule.getIsAverageRatio());

            }
            SkuTemplate skuTemplate = getMaterialCode(info.getSkuCode());
            //设置货号
            String materialCode;
            if (Strings.isNullOrEmpty(spuCode)){
                Map<String, String> extra = skuTemplate.getExtra();
                materialCode = extra != null ? extra.get("materialCode") : "";
            } else{
                materialCode = spuCode;
            }
            info.setSpuCode(materialCode);
            //商品名
            info.setItemName(skuTemplate.getName());

            itemMappingInfos.add(info);
        }
        infoPaging.setData(itemMappingInfos);

        return infoPaging;
    }

    private SkuTemplate getMaterialCode(String skuCode) {

        if (Strings.isNullOrEmpty(skuCode)){
            return new SkuTemplate();
        }
        Response<List<SkuTemplate>> response = skuTemplateReadService.findBySkuCodes(Lists.newArrayList(skuCode));
        if (!response.isSuccess()){
            log.error("find sku template by sku code:{} fail,error:{}",skuCode,response.getError());
            return new SkuTemplate();
        }

        for (SkuTemplate skuTemplate : response.getResult()){
            if (Objects.equals(skuTemplate.getStatus(),1) && Objects.equals(skuTemplate.getSkuCode(),skuCode)){
                Map<String, String>  extra = skuTemplate.getExtra();
                if (CollectionUtils.isEmpty(extra)){
                    return new SkuTemplate();
                }
                return skuTemplate;
            }
        }

        return new SkuTemplate();

    }


}
