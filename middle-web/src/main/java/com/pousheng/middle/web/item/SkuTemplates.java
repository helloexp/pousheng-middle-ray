package com.pousheng.middle.web.item;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.item.constant.PsItemConstants;
import com.pousheng.middle.item.service.PsSkuTemplateWriteService;
import com.pousheng.middle.web.events.item.BatchAsyncExportMposDiscountEvent;
import com.pousheng.middle.web.events.item.BatchAsyncHandleMposFlagEvent;
import com.pousheng.middle.web.events.item.BatchAsyncImportMposDiscountEvent;
import com.pousheng.middle.web.events.item.SkuTemplateUpdateEvent;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Splitters;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import io.terminus.parana.spu.service.SpuReadService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Created by songrenfei on 2017/6/30
 */
@Api(description = "货品管理API")
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




    @ApiOperation("根据id查询商品信息")
    @RequestMapping(value = "/api/sku-template/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<SkuTemplate> findShopByUserId(@PathVariable("id") Long id) {
        return skuTemplateReadService.findById(id);
    }


    /**
     * 获取当前sku code对应sku的spu下的全部sku模板
     * @param skuCode 商品编码
     * @return sku模板
     */
    @ApiOperation("根据货品条码查询")
    @RequestMapping(value="/api/sku-templates-spu",method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SkuTemplate> findSkuTemplates(@RequestParam(name = "skuCode") String skuCode) {
        Response<List<SkuTemplate>> skuTemplateRes = skuTemplateReadService.findBySkuCodes(Lists.newArrayList(skuCode));
        if(!skuTemplateRes.isSuccess()){
            log.error("find sku template by sku code:{} fail,error:{}",skuCode,skuTemplateRes.getError());
            throw new JsonResponseException(skuTemplateRes.getError());
        }
        List<SkuTemplate> skuTemplates = skuTemplateRes.getResult();
        if(CollectionUtils.isEmpty(skuTemplates)){
            log.error("not find sku template by sku code:{}",skuCode);
            throw new JsonResponseException("sku.template.not.exist");
        }

        SkuTemplate skuTemplate = skuTemplates.get(0);

        Response<Spu> spuRes = spuReadService.findById(skuTemplate.getSpuId());
        if(!spuRes.isSuccess()){
            log.error("find spu by id:{} fail,error:{}",skuTemplate.getSpuId(),spuRes.getError());
            throw new JsonResponseException(spuRes.getError());
        }


        Response<List<SkuTemplate>> spuSkuTemplatesRes = skuTemplateReadService.findBySpuId(skuTemplate.getSpuId());
        if(!spuSkuTemplatesRes.isSuccess()){
            log.error("find sku template by spu id:{} fail,error:{}",skuTemplate.getSpuId());
            throw new JsonResponseException(spuSkuTemplatesRes.getError());
        }

        List<SkuTemplate> skuTemplatesList = spuSkuTemplatesRes.getResult();
        skuTemplatesList.forEach(skuTemplate1 -> skuTemplate1.setName(spuRes.getResult().getName()));
        return spuSkuTemplatesRes.getResult();
    }

    @ApiOperation("货品分页查询基于mysql")
    @RequestMapping(value="/api/sku-template/paging",method=RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<SkuTemplate> pagination(@RequestParam(value ="skuCode", required = false) String skuCode,
                                          @RequestParam(value="name",  required = false) String name,
                                          @RequestParam(value = "spuId",required = false) Long spuId,
                                          @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                          @RequestParam(value = "pageSize", required = false) Integer pageSize){

        Map<String, Object> params = Maps.newHashMap();

        if(StringUtils.hasText(skuCode)){
            params.put("skuCode", skuCode);
        }
        if(StringUtils.hasText(name)){
            params.put("name", name);
        }
        if (spuId!=null){
            params.put("spuId",spuId);
        }
        params.put("statuses",Lists.newArrayList(1,-3));
        Response<Paging<SkuTemplate>> r = skuTemplateReadService.findBy(pageNo, pageSize, params);
        if(!r.isSuccess()){
            log.error("failed to pagination skuTemplates with params({}), error code:{}", params, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();

    }



    @ApiOperation("对货品打mops打标")
    @RequestMapping(value = "/api/sku-template/{id}/make/flag", method = RequestMethod.PUT)
    public void makeMposFlag(@PathVariable Long id) {

        val rExist = skuTemplateReadService.findById(id);
        if (!rExist.isSuccess()) {
            log.error("find sku template by id:{} fail,error:{}",id,rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        SkuTemplate exist = rExist.getResult();
        Map<String,String> extra = operationMopsFlag(exist,PsItemConstants.MPOS_ITEM);

        SkuTemplate toUpdate = new SkuTemplate();
        toUpdate.setId(exist.getId());
        toUpdate.setExtra(extra);
        Response<Boolean> resp = psSkuTemplateWriteService.update(toUpdate);
        if (!resp.isSuccess()) {
            log.error("update SkuTemplate failed error={}",resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }

        //更新搜索
        postUpdateSearchEvent(id);

    }

    @ApiOperation("取消货品mops打标")
    @RequestMapping(value = "/api/sku-template/{id}/cancel/flag", method = RequestMethod.PUT)
    public void cancelMposFlag(@PathVariable Long id) {

        val rExist = skuTemplateReadService.findById(id);
        if (!rExist.isSuccess()) {
            log.error("find sku template by id:{} fail,error:{}",id,rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        SkuTemplate exist = rExist.getResult();
        Map<String,String> extra = operationMopsFlag(exist,PsItemConstants.NOT_MPOS_ITEM);

        SkuTemplate toUpdate = new SkuTemplate();
        toUpdate.setId(exist.getId());
        toUpdate.setExtra(extra);
        Response<Boolean> resp = psSkuTemplateWriteService.update(toUpdate);
        if (!resp.isSuccess()) {
            log.error("update SkuTemplate failed error={}",resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        //更新搜索
        postUpdateSearchEvent(id);
    }



    @ApiOperation("设置mpos货品折扣")
    @RequestMapping(value = "/api/sku-template/{id}/discount/setting", method = RequestMethod.PUT)
    public void setDiscount(@PathVariable Long id,@RequestParam Integer discount) {

        val rExist = skuTemplateReadService.findById(id);
        if (!rExist.isSuccess()) {
            log.error("find sku template by id:{} fail,error:{}",id,rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        SkuTemplate exist = rExist.getResult();
        Integer originPrice = 0;
        if (exist.getExtraPrice() != null&&exist.getExtraPrice().containsKey(PsItemConstants.ORIGIN_PRICE_KEY)) {
            originPrice = exist.getExtraPrice().get(PsItemConstants.ORIGIN_PRICE_KEY);
        }
        Map<String,String> extra = setMopsDiscount(exist,discount);

        SkuTemplate toUpdate = new SkuTemplate();
        toUpdate.setId(exist.getId());
        toUpdate.setExtra(extra);
        toUpdate.setPrice(calculatePrice(discount,originPrice));
        Response<Boolean> resp = psSkuTemplateWriteService.update(toUpdate);
        if (!resp.isSuccess()) {
            log.error("update SkuTemplate failed error={}",resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
    }

    @ApiOperation("对mpos货品批量设置折扣")
    @RequestMapping(value = "/api/sku-template/batch/discount/setting", method = RequestMethod.PUT)
    public void batchSetMposDiscount(@RequestParam String skuTemplateIds,@RequestParam Integer discount) {
        List<Long> ids  = Splitters.splitToLong(skuTemplateIds,Splitters.COMMA);
        for (Long id : ids){
            setDiscount(id,discount);
        }
    }


    @ApiOperation("对货品批量打mops打标")
    @RequestMapping(value = "/api/sku-template/batch/make/flag", method = RequestMethod.PUT)
    public void batchMakeMposFlag(@RequestParam String skuTemplateIds) {
        List<Long> ids  = Splitters.splitToLong(skuTemplateIds,Splitters.COMMA);
        for (Long id : ids){
            makeMposFlag(id);
        }
    }

    @ApiOperation("批量取消货品mops打标")
    @RequestMapping(value = "/api/sku-template/batch/cancel/flag", method = RequestMethod.PUT)
    public void batchCancelMposFlag(@RequestParam String skuTemplateIds) {
        List<Long> ids  = Splitters.splitToLong(skuTemplateIds,Splitters.COMMA);
        for (Long id : ids){
            cancelMposFlag(id);
        }
    }

    @ApiOperation("上传货品图片（单个和批量）")
    @RequestMapping(value = "/api/sku-template/image/upload", method = RequestMethod.PUT)
    public void uploadImage(@RequestParam String skuTemplateIds,@RequestParam String imageUrl) {
        List<Long> ids  = Splitters.splitToLong(skuTemplateIds,Splitters.COMMA);
        if(Strings.isNullOrEmpty(imageUrl)){
            throw new JsonResponseException("image.url.invalid");
        }
        Response<Boolean> response = psSkuTemplateWriteService.updateImageByIds(ids,imageUrl);
        if(!response.isSuccess()){
            log.error("failed to update skuTemplate:(ids:{}) image to:{}, error:{}", ids,imageUrl, response.getError());
            throw new JsonResponseException(response.getError());
        }
    }

    @ApiOperation("异步对货品批量mpos打标")
    @RequestMapping(value = "/api/sku-template/batch/async/make/flag",method = RequestMethod.PUT)
    public void asyncMakeMposFlag(@RequestParam Map<String,String> params){
        BatchAsyncHandleMposFlagEvent event = new BatchAsyncHandleMposFlagEvent();
        event.setParams(params);
        event.setType(PsItemConstants.MPOS_ITEM);
        event.setCurrentUserId(UserUtil.getUserId());
        eventBus.post(event);
    }

    @ApiOperation("异步批量取消货品mpos打标")
    @RequestMapping(value = "/api/sku-template/batch/async/cancel/flag",method = RequestMethod.PUT)
    public void asyncCancelMposFlag(@RequestParam Map<String,String> params){
        BatchAsyncHandleMposFlagEvent event = new BatchAsyncHandleMposFlagEvent();
        event.setParams(params);
        event.setType(PsItemConstants.NOT_MPOS_ITEM);
        event.setCurrentUserId(UserUtil.getUserId());
        eventBus.post(event);
    }

    @ApiOperation("导入文件")
    @RequestMapping(value = "/api/sku-template/batch/import/file",method = RequestMethod.POST)
    public void asyncImportFile(@RequestParam(value="upload_excel") MultipartFile multipartFile){
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
    }

    @ApiOperation("导出文件")
    @RequestMapping(value = "/api/sku-template/batch/export/file",method = RequestMethod.PUT)
    public void asyncExportFile(@RequestParam Map<String,String> params){
        BatchAsyncExportMposDiscountEvent event = new BatchAsyncExportMposDiscountEvent();
        event.setParams(params);
        event.setCurrentUserId(UserUtil.getUserId());
        eventBus.post(event);
    }

    //打标或取消打标
    private Map<String,String> operationMopsFlag(SkuTemplate exist,String type){
        Map<String,String> extra = exist.getExtra();
        if(CollectionUtils.isEmpty(extra)){
            extra = Maps.newHashMap();
        }
        extra.put(PsItemConstants.MPOS_FLAG,type);
        return extra;

    }

    //设置折扣
    private Map<String,String> setMopsDiscount(SkuTemplate exist,Integer discount){
        Map<String,String> extra = exist.getExtra();
        if(CollectionUtils.isEmpty(extra)){
            extra = Maps.newHashMap();
        }
        extra.put(PsItemConstants.MPOS_DISCOUNT,discount.toString());
        return extra;

    }

    private void postUpdateSearchEvent(Long skuTemplateId){
        SkuTemplateUpdateEvent updateEvent = new SkuTemplateUpdateEvent();
        updateEvent.setSkuTemplateId(skuTemplateId);
        eventBus.post(updateEvent);
    }


    private static Integer calculatePrice(Integer discount, Integer originPrice){
        BigDecimal ratio = new BigDecimal("100");  // 百分比的倍率
        BigDecimal discountDecimal = new BigDecimal(discount);
        BigDecimal percentDecimal =  discountDecimal.divide(ratio,2, BigDecimal.ROUND_HALF_UP);
        return percentDecimal.multiply(BigDecimal.valueOf(originPrice)).intValue();
    }
}
