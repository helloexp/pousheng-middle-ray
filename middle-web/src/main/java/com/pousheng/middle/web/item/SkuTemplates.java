package com.pousheng.middle.web.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.erp.component.MaterialPusher;
import com.pousheng.erp.model.SpuMaterial;
import com.pousheng.erp.service.SpuMaterialReadService;
import com.pousheng.erp.service.SpuMaterialWriteService;
import com.pousheng.middle.group.model.ItemGroupSku;
import com.pousheng.middle.group.service.ItemGroupSkuWriteService;
import com.pousheng.middle.item.constant.PsItemConstants;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.enums.PsItemGroupSkuType;
import com.pousheng.middle.item.enums.PsSpuType;
import com.pousheng.middle.item.service.PsSkuTemplateWriteService;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.task.dto.ItemGroupTask;
import com.pousheng.middle.task.model.ScheduleTask;
import com.pousheng.middle.task.service.ScheduleTaskWriteService;
import com.pousheng.middle.web.events.item.*;
import com.pousheng.middle.web.item.component.PushMposItemComponent;
import com.pousheng.middle.web.utils.MapFilter;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.utils.task.ScheduleTaskUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.JsonMapper;
import io.terminus.common.utils.Splitters;
import io.terminus.parana.common.constants.JacksonType;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.search.dto.SearchedItemWithAggs;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import io.terminus.parana.spu.service.SpuReadService;
import io.terminus.search.api.model.WithAggregations;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Nullable;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by songrenfei on 2017/6/30
 */
@Api(description = "货品管理API")
@OperationLogModule(OperationLogModule.Module.POUSHENG_ITEMS)
@RestController
@Slf4j
public class SkuTemplates {

    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;
    @RpcConsumer
    private PsSkuTemplateWriteService psSkuTemplateWriteService;
    @RpcConsumer
    private SpuReadService spuReadService;
    @Autowired
    private EventBus eventBus;
    @Autowired
    private PushMposItemComponent pushMposItemComponent;
    @Value("${gateway.parana.host}")
    private String paranaGateway;
    @RpcConsumer
    private SkuTemplateSearchReadService skuTemplateSearchReadService;
    @Autowired
    private SpuMaterialReadService spuMaterialReadService;
    @Autowired
    private SpuMaterialWriteService spuMaterialWriteService;
    @RpcConsumer
    private ItemGroupSkuWriteService itemGroupSkuWriteService;
    @RpcConsumer
    private ScheduleTaskWriteService scheduleTaskWriteService;
    @Autowired
    private MaterialPusher materialPusher;

    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();

    @RequestMapping(value = "/api/sku-template/batch/update", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> batchUpdate(@RequestParam String ids) {
       if(log.isDebugEnabled()){
          log.debug("API-SKU-TEMPLATE-BATCH-UPDATE-START param: ids [{}] ",ids);
       }
       List<Long> idss =  Splitters.splitToLong(ids,Splitters.COMMA);

        Response<List<SkuTemplate>> listRes = skuTemplateReadService.findByIds(idss);

        List<SkuTemplate> skuTemplates = listRes.getResult();

        List<SkuTemplate> updates = Lists.newArrayListWithCapacity(skuTemplates.size());


        for (SkuTemplate exist : skuTemplates) {
            Map<String, String> extra = exist.getExtra();
            SkuTemplate toUpdate = new SkuTemplate();
            toUpdate.setId(exist.getId());
            toUpdate.setType(2);
            //如果本来不包含折扣，默认设置折扣
            if (Objects.equals(2, PsSpuType.MPOS.value()) && !extra.containsKey(PsItemConstants.MPOS_DISCOUNT)) {
                extra.put(PsItemConstants.MPOS_DISCOUNT, "100");
                toUpdate.setExtra(extra);
                Integer originPrice = 0;
                if (exist.getExtraPrice() != null && exist.getExtraPrice().containsKey(PsItemConstants.ORIGIN_PRICE_KEY)) {
                    originPrice = exist.getExtraPrice().get(PsItemConstants.ORIGIN_PRICE_KEY);
                }

                if (Objects.equals(originPrice, 0)) {
                    log.error("[PRICE-INVALID]:sku template:(id:{}) price  code:{} invalid", exist.getId(), exist.getSkuCode());
                }

                toUpdate.setPrice(originPrice);
            }
            updates.add(toUpdate);
        }
        Response<Boolean>  resp = psSkuTemplateWriteService.updateBatch(updates);
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-BATCH-UPDATE-END param: ids [{}] ,resp: [{}]",ids,resp.getResult());
        }
        return resp;
    }


    @ApiOperation("根据id查询商品信息")
    @RequestMapping(value = "/api/sku-template/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<SkuTemplate> findShopByUserId(@PathVariable("id") Long id) {
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-FINDSHOPBYUSERID-START param: id [{}] ",id);
        }
        Response<SkuTemplate> resp = skuTemplateReadService.findById(id);
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-FINDSHOPBYUSERID-END param: id [{}] ,resp: [{}]",id,resp.getResult());
        }
        return resp;
    }

    @ApiOperation("根据id集合查询商品集合")
    @RequestMapping(value = "api/sku-templates",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<List<SkuTemplate>> findSkuByIds(@RequestParam String skuIds){
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-FINDSKUBYIDS-START param: skuIds [{}] ",skuIds);
        }
        List<Long> ids  = Splitters.splitToLong(skuIds,Splitters.COMMA);
        Response<List<SkuTemplate>> resp= skuTemplateReadService.findByIds(ids);
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-FINDSKUBYIDS-END param: skuIds [{}] ,resp: [{}]",skuIds,JsonMapper.nonEmptyMapper().toJson(resp.getResult()));
        }
        return resp;
    }

    /**
     * 获取当前sku code对应sku的spu下的全部sku模板
     *
     * @param skuCode 商品编码
     * @return sku模板
     */
    @ApiOperation("根据货品条码查询")
    @RequestMapping(value = "/api/sku-templates-spu", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SkuTemplate> findSkuTemplates(@RequestParam(name = "skuCode", required = false) String skuCode, @RequestParam(required = false, name = "skuCodes") List<String> skuCodes) {
        List<String> originSkuCodes = Lists.newArrayList();
        if (!StringUtils.isEmpty(skuCode)) {
            originSkuCodes.add(skuCode);
        } else {
            originSkuCodes = skuCodes;
        }
        Response<List<SkuTemplate>> skuTemplateRes = skuTemplateReadService.findBySkuCodes(originSkuCodes);
        if (!skuTemplateRes.isSuccess()) {
            log.error("find sku template by sku code:{} fail,error:{}", skuCode, skuTemplateRes.getError());
            throw new JsonResponseException(skuTemplateRes.getError());
        }
        List<SkuTemplate> skuTemplates = skuTemplateRes.getResult();
        if (CollectionUtils.isEmpty(skuTemplates)) {
            log.error("not find sku template by sku code:{}", skuCode);
            throw new JsonResponseException("sku.template.not.exist");
        }

        SkuTemplate skuTemplate = skuTemplates.get(0);

        Response<Spu> spuRes = spuReadService.findById(skuTemplate.getSpuId());
        if (!spuRes.isSuccess()) {
            log.error("find spu by id:{} fail,error:{}", skuTemplate.getSpuId(), spuRes.getError());
            throw new JsonResponseException(spuRes.getError());
        }


        Response<List<SkuTemplate>> spuSkuTemplatesRes = skuTemplateReadService.findBySpuId(skuTemplate.getSpuId());
        if (!spuSkuTemplatesRes.isSuccess()) {
            log.error("find sku template by spu id:{} fail,error:{}", skuTemplate.getSpuId());
            throw new JsonResponseException(spuSkuTemplatesRes.getError());
        }

        List<SkuTemplate> skuTemplatesList = spuSkuTemplatesRes.getResult();
        skuTemplatesList.forEach(skuTemplate1 -> skuTemplate1.setName(spuRes.getResult().getName()));
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-FINDSKUTEMPLATES-END param: skuCode [{}] skuCodes [{}] ,resp: [{}]",skuCode,skuCodes,JsonMapper.nonEmptyMapper().toJson(spuSkuTemplatesRes.getResult()));
        }
        return spuSkuTemplatesRes.getResult();
    }

    @ApiOperation("货品分页查询基于mysql")
    @RequestMapping(value = "/api/sku-template/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<SkuTemplate> pagination(@RequestParam(value = "ids", required = false) List<Long> ids, @RequestParam(value = "skuCode", required = false) String skuCode,
                                          @RequestParam(value = "name", required = false) String name,
                                          @RequestParam(value = "spuId", required = false) Long spuId,
                                          @RequestParam(value = "type", required = false) Integer type,
                                          @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                          @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                          @RequestParam(value = "statuses", required = false) List<Integer> statuses){
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-PAGINATION-START param: ids [{}] skuCode [{}] name [{}] spuId [{}] type [{}] pageNo [{}] pageSize [{}] statuses [{}]",
                    ids,skuCode,name,spuId,type,pageNo,pageSize,statuses);
        }
        Map<String, Object> params = Maps.newHashMap();
        if (Objects.nonNull(ids)) {
            params.put("ids", ids);
        }
        if (StringUtils.hasText(skuCode)) {
            params.put("skuCode", skuCode);
        }
        if (StringUtils.hasText(name)) {
            params.put("name", name);
        }
        if (spuId != null) {
            params.put("spuId", spuId);
        }

        if (type != null) {
            params.put("type", type);
        }
        if (Objects.isNull(statuses)) {
            params.put("statuses", Lists.newArrayList(1, -3));
        } else if (!statuses.isEmpty()) {
            params.put("statuses", statuses);
        }
        Response<Paging<SkuTemplate>> r = skuTemplateReadService.findBy(pageNo, pageSize, params);
        if (!r.isSuccess()) {
            log.error("failed to pagination skuTemplates with params({}), error code:{}", params, r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-PAGINATION-START param: ids [{}] skuCode [{}] name [{}] spuId [{}] type [{}] pageNo [{}] pageSize [{}] statuses [{}] ,resp: [{}]",
                    ids,skuCode,name,spuId,type,pageNo,pageSize,statuses,JsonMapper.nonEmptyMapper().toJson(r.getResult()));
        }
        return r.getResult();

    }


    @ApiOperation("对货品打mops打标")
    @RequestMapping(value = "/api/sku-template/{id}/make/flag", method = RequestMethod.PUT)
    public void makeMposFlag(@PathVariable Long id) {
        log.info("start  mpos flag id:{} by user id:{}", id, UserUtil.getUserId());
        val rExist = skuTemplateReadService.findById(id);
        if (!rExist.isSuccess()) {
            log.error("find sku template by id:{} fail,error:{}", id, rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        SkuTemplate exist = rExist.getResult();

        SkuTemplate toUpdate = new SkuTemplate();
        toUpdate.setId(exist.getId());
        toUpdate.setType(PsSpuType.MPOS.value());
        Response<Boolean> resp = psSkuTemplateWriteService.update(toUpdate);
        if (!resp.isSuccess()) {
            log.error("update SkuTemplate failed error={}", resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }

        //更新搜索
        postUpdateSearchEvent(id);

        Map<String, String> extra = getSkuTemplateExtra(exist);
        // MPOS打标后默认折扣为1，即不设折扣不打折
        if (!extra.containsKey(PsItemConstants.MPOS_DISCOUNT)) {
            setDiscount(id, 100);
        }

        //同步电商
        pushMposItemComponent.push(exist);
        log.info("end  mpos flag id:{} by user id:{}",id,UserUtil.getUserId());

    }

    @ApiOperation("取消货品mops打标")
    @RequestMapping(value = "/api/sku-template/{id}/cancel/flag", method = RequestMethod.PUT)
    public void cancelMposFlag(@PathVariable Long id) {

        log.info("start cancel mpos flag id:{} by user id:{}", id, UserUtil.getUserId());

        val rExist = skuTemplateReadService.findById(id);
        if (!rExist.isSuccess()) {
            log.error("find sku template by id:{} fail,error:{}", id, rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        SkuTemplate exist = rExist.getResult();
        SkuTemplate toUpdate = new SkuTemplate();
        toUpdate.setId(exist.getId());
        toUpdate.setType(PsSpuType.POUSHENG.value());
        Response<Boolean> resp = psSkuTemplateWriteService.update(toUpdate);
        if (!resp.isSuccess()) {
            log.error("update SkuTemplate failed error={}", resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        //更新搜索
        postUpdateSearchEvent(id);

        //同步电商
        pushMposItemComponent.del(Lists.newArrayList(exist));
        log.info("end cancel mpos flag id:{} by user id:{}",id,UserUtil.getUserId());
    }


    @ApiOperation("设置mpos货品折扣")
    @RequestMapping(value = "/api/sku-template/{id}/discount/setting", method = RequestMethod.PUT)
    public void setDiscount(@PathVariable Long id, @RequestParam Integer discount) {
        log.info("start batch  set discount:{} skuTemplateId:{} by user id:{}", discount, id, UserUtil.getUserId());

        val rExist = skuTemplateReadService.findById(id);
        if (!rExist.isSuccess()) {
            log.error("find sku template by id:{} fail,error:{}", id, rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        SkuTemplate exist = rExist.getResult();
        Integer originPrice = 0;
        if (exist.getExtraPrice() != null && exist.getExtraPrice().containsKey(PsItemConstants.ORIGIN_PRICE_KEY)) {
            originPrice = exist.getExtraPrice().get(PsItemConstants.ORIGIN_PRICE_KEY);
        }
        Map<String, String> extra = setMopsDiscount(exist, discount);

        SkuTemplate toUpdate = new SkuTemplate();
        toUpdate.setId(exist.getId());
        toUpdate.setExtra(extra);
        toUpdate.setPrice(calculatePrice(discount, originPrice));
        Response<Boolean> resp = psSkuTemplateWriteService.update(toUpdate);
        if (!resp.isSuccess()) {
            log.error("update SkuTemplate failed error={}", resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }

        //同步电商
        pushMposItemComponent.updatePrice(Lists.newArrayList(exist),toUpdate.getPrice());
        log.info("end batch  set discount:{} skuTemplateId:{} by user id:{}",discount,id,UserUtil.getUserId());
    }

    @ApiOperation("对mpos货品批量设置折扣")
    @RequestMapping(value = "/api/sku-template/batch/discount/setting", method = RequestMethod.PUT)
    public void batchSetMposDiscount(@RequestParam String skuTemplateIds, @RequestParam Integer discount) {
        log.info("start batch  set discount:{} skuTemplateIds data:{} by user id:{}", discount, skuTemplateIds, UserUtil.getUserId());
        List<Long> ids = Splitters.splitToLong(skuTemplateIds, Splitters.COMMA);
        for (Long id : ids) {
            setDiscount(id, discount);
        }
        log.info("end batch  set discount:{} skuTemplateIds data:{} by user id:{}",discount,skuTemplateIds,UserUtil.getUserId());

    }

    @RequestMapping(value = "/api/sku-template/batch/make/falge", method = RequestMethod.PUT)
    public void batchSetFlage(@RequestParam String skuTemplateIds) {
        log.info("start batch  mpos flag data:{} by user id:{}", skuTemplateIds, UserUtil.getUserId());
        List<Long> ids = Splitters.splitToLong(skuTemplateIds, Splitters.COMMA);
        Response<List<SkuTemplate>> response = skuTemplateReadService.findByIds(ids);
        pushMposItemComponent.batchMakeFlag(response.getResult(),PsSpuType.MPOS.value());
        log.info("end batch  mpos flag data:{} by user id:{}",skuTemplateIds,UserUtil.getUserId());
    }


    @ApiOperation("对货品批量打mops打标")
    @RequestMapping(value = "/api/sku-template/batch/make/flag", method = RequestMethod.PUT)
    public void batchMakeMposFlag(@RequestParam String skuTemplateIds) {
        log.info("start batch  mpos flag data:{} by user id:{}",skuTemplateIds,UserUtil.getUserId());
        List<Long> ids = Splitters.splitToLong(skuTemplateIds, Splitters.COMMA);
        for (Long id : ids) {
            makeMposFlag(id);
        }
        log.info("end batch  mpos flag data:{} by user id:{}",skuTemplateIds,UserUtil.getUserId());
    }

    @ApiOperation("批量取消货品mops打标")
    @RequestMapping(value = "/api/sku-template/batch/cancel/flag", method = RequestMethod.PUT)
    public void batchCancelMposFlag(@RequestParam String skuTemplateIds) {
        log.info("start batch cancel mpos flag data:{} by user id:{}",skuTemplateIds,UserUtil.getUserId());
        List<Long> ids  = Splitters.splitToLong(skuTemplateIds,Splitters.COMMA);
        for (Long id : ids){
            cancelMposFlag(id);
        }
        log.info("end batch cancel mpos flag data:{} by user id:{}",skuTemplateIds,UserUtil.getUserId());
    }

    @ApiOperation("上传货品图片（单个和批量）")
    @RequestMapping(value = "/api/sku-template/image/upload", method = RequestMethod.PUT)
    public void uploadImage(@RequestParam String skuTemplateIds,@RequestParam String imageUrl) {
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-IMAGE-UPLOAD-START param: skuTemplateIds [{}] imageUrl [{}]",skuTemplateIds,imageUrl);
        }
        List<Long> ids  = Splitters.splitToLong(skuTemplateIds,Splitters.COMMA);
        if(Strings.isNullOrEmpty(imageUrl)){
            throw new JsonResponseException("image.url.invalid");
        }

        Response<List<SkuTemplate>> skuTemplateRes = skuTemplateReadService.findByIds(ids);
        if (!skuTemplateRes.isSuccess()) {
            log.error("failed to find skuTemplate: by ids:{}, error:{}", ids, skuTemplateRes.getError());
            throw new JsonResponseException(skuTemplateRes.getError());
        }

        Response<Boolean> response = psSkuTemplateWriteService.updateImageByIds(ids, imageUrl);
        if (!response.isSuccess()) {
            log.error("failed to update skuTemplate:(ids:{}) image to:{}, error:{}", ids, imageUrl, response.getError());
            throw new JsonResponseException(response.getError());
        }

        //同步电商
        pushMposItemComponent.updateImage(skuTemplateRes.getResult(),imageUrl);
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-IMAGE-UPLOAD-END param: skuTemplateIds [{}] imageUrl [{}]",skuTemplateIds,imageUrl);
        }
    }

    @ApiOperation("异步对货品批量mpos打标")
    @RequestMapping(value = "/api/sku-template/batch/async/make/flag",method = RequestMethod.PUT)
    public void asyncMakeMposFlag(@RequestParam Map<String,String> params){
        log.info("start asyncMakeMposFlag params:{} by user id:{}",params.toString(),UserUtil.getUserId());
        Map<String,String> fliter = MapFilter.filterNullOrEmpty(params);
        if(CollectionUtils.isEmpty(fliter)){
            log.error("make flag param:{} invalid",params.toString());
            throw new JsonResponseException("at.least.one.condition");
        }
        BatchAsyncHandleMposFlagEvent event = new BatchAsyncHandleMposFlagEvent();
        event.setParams(fliter);
        event.setType(PsSpuType.MPOS.value());
        event.setCurrentUserId(UserUtil.getUserId());
        eventBus.post(event);
        log.info("end asyncMakeMposFlag params:{} by user id:{}",params.toString(),UserUtil.getUserId());
    }


    @ApiOperation("导入文件-商品打标")
    @RequestMapping(value = "/api/sku-template/batch/import/file/mpos/flag",method = RequestMethod.POST)
    public void asyncImportMposFlageFile(@RequestParam(value="upload_excel") MultipartFile multipartFile){
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-BATCH-IMPORT-FILE-MPOS-FLAG-START param: ");
        }
        if(multipartFile == null){
            log.error("the upload file is null");
            throw new JsonResponseException("the upload file is null");
        }
        String fileName = multipartFile.getOriginalFilename();
        if (!fileName.endsWith(".xls") && !fileName.endsWith(".xlsx")) {
            log.error("the upload file is not a excel");
            throw new JsonResponseException("the upload file is not a excel");
        }
        BatchAsyncImportMposFlagEvent event = new BatchAsyncImportMposFlagEvent();
        event.setFile(multipartFile);
        event.setCurrentUserId(UserUtil.getUserId());
        eventBus.post(event);
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-BATCH-IMPORT-FILE-MPOS-FLAG-END param: ");
        }
    }

    @ApiOperation("异步批量取消货品mpos打标")
    @RequestMapping(value = "/api/sku-template/batch/async/cancel/flag", method = RequestMethod.PUT)
    public void asyncCancelMposFlag(@RequestParam Map<String, String> params) {
        log.info("start async cancel mpos flag,params:{}", params);
        BatchAsyncHandleMposFlagEvent event = new BatchAsyncHandleMposFlagEvent();
        event.setParams(params);
        event.setType(PsSpuType.POUSHENG.value());
        event.setCurrentUserId(UserUtil.getUserId());
        eventBus.post(event);
        log.info("end async cancel mpos flag,params:{}",params);
    }

    @ApiOperation("导入文件")
    @RequestMapping(value = "/api/sku-template/batch/import/file",method = RequestMethod.POST)
    public void asyncImportFile(@RequestParam(value="upload_excel") MultipartFile multipartFile){
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-BATCH-IMPORT-FILE-START param: ");
        }
        if(multipartFile == null){
            log.error("the upload file is null");
            throw new JsonResponseException("the upload file is null");
        }
        String fileName = multipartFile.getOriginalFilename();
        if(!fileName.endsWith(".xls") && !fileName.endsWith(".xlsx")){
            log.error("the upload file is not a excel");
            throw new JsonResponseException("the upload file is not a excel");
        }
        BatchAsyncImportMposDiscountEvent event = new BatchAsyncImportMposDiscountEvent();
        event.setFile(multipartFile);
        event.setCurrentUserId(UserUtil.getUserId());
        eventBus.post(event);
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-BATCH-IMPORT-FILE-END param: ");
        }
    }

    @ApiOperation("导出文件")
    @RequestMapping(value = "/api/sku-template/batch/export/file",method = RequestMethod.PUT)
    public void asyncExportFile(@RequestParam Map<String,String> params){
        String paramStr = JsonMapper.nonEmptyMapper().toJson(params);
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-BATCH-EXPORT-FILE-START param: params [{}]",paramStr);
        }
        BatchAsyncExportMposDiscountEvent event = new BatchAsyncExportMposDiscountEvent();
        event.setParams(params);
        event.setCurrentUserId(UserUtil.getUserId());
        eventBus.post(event);
        if(log.isDebugEnabled()){
            log.debug("API-SKU-TEMPLATE-BATCH-EXPORT-FILE-END param: params [{}]",paramStr);
        }
    }


    //修复中台打标mpos总单同步缺少商品
    @RequestMapping(value = "api/fix/mpos/shop/item",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> fixMposItems(@RequestParam(required = false) String skuIds){
        if(log.isDebugEnabled()){
            log.debug("API-FIX-MPOS-SHOP-ITEM-START param: skuIds [{}]",skuIds);
        }
        int pageNo = 1;
        Map<String, Object> params = Maps.newHashMap();
        if (!Strings.isNullOrEmpty(skuIds)) {
            List<Long> ids = Splitters.splitToLong(skuIds, Splitters.COMMA);
            params.put("ids", ids);
        }
        params.put("type", 2);
        params.put("statuses", Lists.newArrayList(1));

        boolean next = batchHandle(pageNo, 3000, params);
        while (next) {
            pageNo++;
            next = batchHandle(pageNo, 3000, params);
        }

        if(log.isDebugEnabled()){
            log.debug("API-FIX-MPOS-SHOP-ITEM-END param: skuIds [{}]",skuIds);
        }

        return Response.ok(Boolean.TRUE);


    }


    /**
     * 修复spu和货号关联错误
     *
     * @param data 货号
     */
    @RequestMapping(value = "/api/fix/spu/material",method = RequestMethod.PUT)
    public void fixSpuMaterial(@RequestParam String data){
        log.info("START-FIX-SPU-MATERIAL data:{}",data);
        List<String> materialIds = Splitters.COMMA.splitToList(data);
        List<String> errorMaterialIds = Lists.newArrayListWithCapacity(materialIds.size());
        for (String materialId : materialIds) {

            //1、根据货号和尺码查询 spuCode=20171214001&attrs=年份:2017
            String templateName = "ps_search.mustache";
            Map<String, String> params = Maps.newHashMap();
            params.put("spuCode", materialId);
            Response<? extends SearchedItemWithAggs<SearchSkuTemplate>> response = skuTemplateSearchReadService.searchWithAggs(1, 20, templateName, params, SearchSkuTemplate.class);
            if (!response.isSuccess()) {
                log.error("query sku template by materialId:{} fail,error:{}", materialId, response.getError());
                throw new JsonResponseException(response.getError());
            }

            List<SearchSkuTemplate> searchSkuTemplates = response.getResult().getEntities().getData();
            if (CollectionUtils.isEmpty(searchSkuTemplates)) {
                log.error("middle not find sku template by materialId:{} ", materialId);
                errorMaterialIds.add(materialId);
                continue;
            }
            SearchSkuTemplate searchSkuTemplate = searchSkuTemplates.get(0);

            Response<com.google.common.base.Optional<SpuMaterial>> materialRes = spuMaterialReadService.findbyMaterialCode(materialId);
            if (!materialRes.isSuccess()) {
                log.error("find spu material by material id:{} fail,error:{}", materialId, materialRes.getError());
                errorMaterialIds.add(materialId);

                continue;
            }

            if (!materialRes.getResult().isPresent()) {
                log.error("not find spu material by material id:{}", materialId);
                errorMaterialIds.add(materialId);
                continue;
            }
            SpuMaterial spuMaterial = materialRes.getResult().get();

            if (Objects.equals(spuMaterial.getSpuId(), searchSkuTemplate.getSpuId())) {
                log.error("spu material(id:{}) spu id:{} equal search spu id:{}", materialId, spuMaterial.getSpuId(), searchSkuTemplate.getSpuId());
                errorMaterialIds.add(materialId);
                continue;
            }

            //更新spu id

            SpuMaterial update = new SpuMaterial();
            update.setId(spuMaterial.getId());
            update.setSpuId(searchSkuTemplate.getSpuId());
            Response<Boolean> updateRes = spuMaterialWriteService.update(update);
            if (!updateRes.isSuccess()) {
                log.error("update spu material:{} fail,error:{}", update, updateRes.getError());
            }

            log.info("ERROR-MATERIAL:{}", errorMaterialIds);

            log.info("END-FIX-SPU-MATERIAL data:{}", data);
        }
    }

    @SuppressWarnings("unchecked")
    private boolean batchHandle(int pageNo, int size, Map<String, Object> params) {

        Response<Paging<SkuTemplate>> pagingRes = skuTemplateReadService.findBy(pageNo, size, params);
        if (!pagingRes.isSuccess()) {
            log.error("paging sku template order fail,criteria:{},error:{}", params, pagingRes.getError());
            return Boolean.FALSE;
        }

        Paging<SkuTemplate> paging = pagingRes.getResult();
        List<SkuTemplate> skuTemplates = paging.getData();

        if (paging.getTotal().equals(0L) || CollectionUtils.isEmpty(skuTemplates)) {
            return Boolean.FALSE;
        }


        Map<String, SkuTemplate> skuTemplateMap = skuTemplates.stream().filter(Objects::nonNull)
                .collect(Collectors.toMap(SkuTemplate::getSkuCode, it -> it));

        List<String> skuCodes = Lists.transform(skuTemplates, new Function<SkuTemplate, String>() {
            @Nullable
            @Override
            public String apply(@Nullable SkuTemplate input) {
                return input.getSkuCode();
            }
        });

        //查询mpos总店是否有该商品

        Map<String, Object> mposParams = Maps.newHashMap();
        String skuCodeStr = Joiners.COMMA.join(skuCodes);
        mposParams.put("skuCodes", skuCodeStr);
        String skuJson = postQueryMposItem(mposParams);
        log.info("check sku codes is exist:{} ,result:{}", skuCodes, skuJson);

        try {
            List<String> mposSkuCodes = objectMapper.readValue(skuJson, JacksonType.LIST_OF_STRING);

            for (String skuCode : skuCodes) {
                //mpos缺少
                if (!mposSkuCodes.contains(skuCode)) {
                    log.info("MPOS-SKU-NOT-EXIST sku code:{} mpos not exist", skuCode);
                    //同步电商
                    pushMposItemComponent.push(skuTemplateMap.get(skuCode));
                }
            }
        } catch (IOException e) {
            log.error("analysis sku json:{} fail,cause:{}", skuJson, Throwables.getStackTraceAsString(e));
        }
        int current = skuTemplates.size();
        return current == size;  // 判断是否存在下一个要处理的批次
    }


    //打标或取消打标
    private Map<String, String> getSkuTemplateExtra(SkuTemplate exist) {
        Map<String, String> extra = exist.getExtra();
        if (CollectionUtils.isEmpty(extra)) {
            extra = Maps.newHashMap();
        }
        return extra;

    }

    //设置折扣
    private Map<String, String> setMopsDiscount(SkuTemplate exist, Integer discount) {
        Map<String, String> extra = exist.getExtra();
        if (CollectionUtils.isEmpty(extra)) {
            extra = Maps.newHashMap();
        }
        extra.put(PsItemConstants.MPOS_DISCOUNT, discount.toString());
        return extra;

    }

    private void postUpdateSearchEvent(Long skuTemplateId) {
        SkuTemplateUpdateEvent updateEvent = new SkuTemplateUpdateEvent();
        updateEvent.setSkuTemplateId(skuTemplateId);
        eventBus.post(updateEvent);
    }


    private static Integer calculatePrice(Integer discount, Integer originPrice) {
        // 百分比的倍率
        BigDecimal ratio = new BigDecimal("100");
        BigDecimal discountDecimal = new BigDecimal(discount);
        BigDecimal percentDecimal = discountDecimal.divide(ratio, 2, BigDecimal.ROUND_HALF_UP);
        return percentDecimal.multiply(BigDecimal.valueOf(originPrice)).intValue();
    }


    private String postQueryMposItem(Map<String, Object> params) {
        return HttpRequest.post(paranaGateway + "/api/query/mpos/item").connectTimeout(1000000).readTimeout(1000000).form(params).body();
    }

    private void checkFile(MultipartFile multipartFile) {
        if (multipartFile == null) {
            log.error("the upload file is null");
            throw new JsonResponseException("the upload file is null");
        }
        String fileName = multipartFile.getOriginalFilename();
        if (!fileName.endsWith(".xls") && !fileName.endsWith(".xlsx")) {
            log.error("the upload file is not a excel");
            throw new JsonResponseException("the upload file is not a excel");
        }
    }


    private Map<String, String> checkParams(Map<String, String> params) {
        Map<String, String> filter = MapFilter.filterNullOrEmpty(params);
        if (CollectionUtils.isEmpty(filter)) {
            log.error("make flag param:{} invalid", params.toString());
            throw new JsonResponseException("at.least.one.condition");
        }
        return filter;
    }


    /**
     * 获取当前有效的skuCode
     *
     * @param skuCode 商品编码
     * @return sku模板
     */
    @ApiOperation("根据有效货品条码查询")
    @RequestMapping(value="/api/valid/sku-templates",method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public SkuTemplate findValidSkuTemplates(@RequestParam(name = "skuCode",required = false) String skuCode) {
        if(log.isDebugEnabled()){
            log.debug("API-VALID-SKU-TEMPLATES-START param: skuCode [{}]",skuCode);
        }
        Response<List<SkuTemplate>> skuTemplateRes = skuTemplateReadService.findBySkuCodes(Lists.newArrayList(skuCode));
        if(!skuTemplateRes.isSuccess()){
            log.error("find sku template by sku code:{} fail,error:{}",skuCode,skuTemplateRes.getError());
            throw new JsonResponseException(skuTemplateRes.getError());
        }
        //获取有效的货品条码
        Optional<SkuTemplate> skuTemplateOptional= skuTemplateRes.getResult().stream().filter(skuTemplate -> !Objects.equals(skuTemplate.getStatus(),-3)).findAny();
        if(!skuTemplateOptional.isPresent()){
            log.error("not find sku template by sku code:{}",skuCode);
            throw new JsonResponseException("sku.template.not.exist");
        }
        if(log.isDebugEnabled()){
            log.debug("API-VALID-SKU-TEMPLATES-END param: skuCode [{}] ,resp: [{}]",skuCode,skuTemplateOptional.get());
        }
        return skuTemplateOptional.get();
    }

    @ApiOperation("批量将货品加入到分组或分组的排除商品")
    @PutMapping(value = "/api/sku-template/batch/make/group")
    public void batchMakeGroup(@RequestParam String skuTemplateIds, @RequestParam Long groupId,
                               @RequestParam Integer type) {
        log.info("start batch add item group data:{} groupId:{}", skuTemplateIds, groupId);
        List<Long> ids = Splitters.splitToLong(skuTemplateIds, Splitters.COMMA);
        for (Long id : ids) {
            addGroup(id, groupId, type);
        }
    }

    @ApiOperation("将货品批量移出分组")
    @PutMapping(value = "/api/sku-template/batch/cancel/group")
    public void batchCancelGroup(@RequestParam String skuTemplateIds, @RequestParam Long groupId) {
        log.info("start batch cancel item group data:{} groupId:{}", skuTemplateIds, groupId);
        List<Long> ids = Splitters.splitToLong(skuTemplateIds, Splitters.COMMA);
        for (Long id : ids) {
            cancelGroup(id, groupId);
        }
    }


    @ApiOperation("将货品移出分组")
    @PutMapping(value = "/api/sku-template/{id}/cancel/group")
    public void cancelGroup(@PathVariable Long id, Long groupId) {

        log.info("start cancel group id:{}  groupId:{}", id, groupId);
        val rExist = skuTemplateReadService.findById(id);
        if (!rExist.isSuccess()) {
            log.error("find sku template by id:{} fail,error:{}", id, rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        Response<Boolean> resp = itemGroupSkuWriteService.deleteByGroupIdAndSkuId(groupId, id);
        if (!resp.isSuccess()) {
            log.error("delete item group sku failed error={}", resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        noticeCancelHk(rExist.getResult(),groupId);
        //更新搜索
        postUpdateSearchEvent(id);
    }


    @ApiOperation("将货品加入到分组或分组的排除商品")
    @PutMapping(value = "/api/sku-template/{id}/add/group")
    public void addGroup(@PathVariable Long id, Long groupId, Integer type) {

        log.info("start add group id:{}  groupId:{}", id, groupId);
        val rExist = skuTemplateReadService.findById(id);
        if (!rExist.isSuccess()) {
            log.error("find sku template by id:{} fail,error:{}", id, rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        ItemGroupSku itemGroupSku = new ItemGroupSku().groupId(groupId).skuId(id).type(type);
        Response<Long> resp = itemGroupSkuWriteService.createItemGroupSku(itemGroupSku);
        if (!resp.isSuccess()) {
            log.error("add item group sku failed error={}", resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        if(PsItemGroupSkuType.GROUP.value().equals(type)){
            materialPusher.addSpus(Lists.newArrayList(rExist.getResult().getSpuId()));
        }
        //更新搜索
        postUpdateSearchEvent(id);
    }


    @ApiOperation("异步对商品批量分组")
    @PutMapping(value = "api/sku-template/batch/async/make/group/{groupId}")
    public void asyncMakeGroup(@PathVariable("groupId") Long groupId, @RequestParam Map<String, String> params) {
        Map<String, String> filter = checkParams(params);
        ScheduleTask task = ScheduleTaskUtil.transItemGroupTask(new ItemGroupTask().params(filter).groupId(groupId).type(PsItemGroupSkuType.GROUP.value())
                .mark(true).userId(UserUtil.getUserId()));
        Response<Long> resp = scheduleTaskWriteService.create(task);
        if (!resp.isSuccess()) {
            throw new JsonResponseException(resp.getError());
        }
    }


    @ApiOperation("异步批量添加排除商品")
    @PutMapping(value = "api/sku-template/batch/async/make/group/exclude/{groupId}")
    public void asyncMakeExclude(@PathVariable("groupId") Long groupId, @RequestParam Map<String, String> params) {
        Map<String, String> filter = checkParams(params);
        ScheduleTask task = ScheduleTaskUtil.transItemGroupTask(new ItemGroupTask().params(filter).groupId(groupId).type(PsItemGroupSkuType.EXCLUDE.value())
                .mark(true).userId(UserUtil.getUserId()));

        Response<Long> resp = scheduleTaskWriteService.create(task);
        if (!resp.isSuccess()) {
            throw new JsonResponseException(resp.getError());
        }
    }


    @ApiOperation("导入文件-商品分组")
    @PostMapping(value = "/api/sku-template/batch/import/group/file")
    public void asyncImportGroupFile(@RequestParam String fileUrl,
                                     @RequestParam Long groupId, @RequestParam Integer type) {

        ScheduleTask task = ScheduleTaskUtil.transItemGroupImportTask(new ItemGroupTask().groupId(groupId)
                .type(PsItemGroupSkuType.GROUP.value())
                .type(type).mark(true).fileUrl(fileUrl).userId(UserUtil.getUserId()));
        Response<Long> resp = scheduleTaskWriteService.create(task);
        if (!resp.isSuccess()) {
            throw new JsonResponseException(resp.getError());
        }
    }

    //商品移除分组时通知恒康
    private void noticeCancelHk(SkuTemplate skuTemplate,Long groupId){
        String templateName = "ps_search.mustache";
        Map<String, String> params = new HashMap<>();
        params.put("spuCode", skuTemplate.getExtra().get("materialId"));
        Response<WithAggregations<SearchSkuTemplate>> response = skuTemplateSearchReadService.doSearchWithAggs(1, 1000, templateName, params, SearchSkuTemplate.class);
        if (!response.isSuccess()) {
            log.error("query sku template by materialId:{} fail,error:{}", skuTemplate.getExtra().get("materialId"), response.getError());
            throw new JsonResponseException(response.getError());
        }
        List<SearchSkuTemplate> searchSkuTemplates = response.getResult().getData();
        Set<String> materialIds = searchSkuTemplates.stream().filter(e -> e.getGroupIds().size()==1&&e.getGroupIds().contains(groupId)).map(SearchSkuTemplate::getSkuCode).collect(Collectors.toSet());
        if(!CollectionUtils.isEmpty(materialIds)){
            materialPusher.removeMaterialIds(Lists.newArrayList(materialIds));
        }
    }
}
