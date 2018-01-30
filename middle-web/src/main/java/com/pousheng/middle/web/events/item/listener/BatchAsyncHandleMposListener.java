package com.pousheng.middle.web.events.item.listener;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.pousheng.middle.item.constant.PsItemConstants;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.service.PsSkuTemplateWriteService;
import com.pousheng.middle.item.service.PsSpuAttributeReadService;
import com.pousheng.middle.item.service.SkuTemplateDumpService;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.web.events.item.BatchAsyncExportMposDiscountEvent;
import com.pousheng.middle.web.events.item.BatchAsyncHandleMposFlagEvent;
import com.pousheng.middle.web.events.item.BatchAsyncImportMposDiscountEvent;
import com.pousheng.middle.web.events.item.SkuTemplateUpdateEvent;
import com.pousheng.middle.web.export.SearchSkuTemplateEntity;
import com.pousheng.middle.web.item.batchhandle.AbnormalRecord;
import com.pousheng.middle.web.item.batchhandle.ExcelExportHelper;
import com.pousheng.middle.web.item.batchhandle.ExcelUtil;
import com.pousheng.middle.web.item.component.PushMposItemComponent;
import com.pousheng.middle.web.item.component.SkutemplateScrollSearcher;
import com.pousheng.middle.web.utils.export.*;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.SpuAttribute;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import io.terminus.search.api.model.Pagination;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import redis.clients.jedis.Jedis;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 监听异步处理mpos相关事件
 * @author penghui
 * @since 2017/12/11
 */
@Slf4j
@Component
public class BatchAsyncHandleMposListener {

    @Autowired
    private EventBus eventBus;

    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;

    @RpcConsumer
    private PsSkuTemplateWriteService psSkuTemplateWriteService;

    @RpcConsumer
    private PsSpuAttributeReadService psSpuAttributeReadService;

    @Autowired
    private JedisTemplate jedisTemplate;

    @Autowired
    private AzureOSSBlobClient azureOssBlobClient;

    @Autowired
    private PushMposItemComponent pushMposItemComponent;

    @Autowired
    private SkutemplateScrollSearcher skutemplateScrollSearcher;

    @Autowired
    private SkuTemplateDumpService skuTemplateDumpService;


    private static final Integer BATCH_SIZE = 100;

    private static final int BATCH_RECORD_EXPIRE_TIME = 2592000;

    private static final String DEFAULT_CLOUD_PATH = "export";


    @PostConstruct
    private void register() {
        this.eventBus.register(this);
    }

    /**
     * 监听打标，取消打标事件
     * @param batchMakeMposFlagEvent
     */
    @Subscribe
    public void onBatchHandleMposFlag(BatchAsyncHandleMposFlagEvent batchMakeMposFlagEvent){
        int pageNo = 1;
        Map<String,String> params = batchMakeMposFlagEvent.getParams();
        Long userId = batchMakeMposFlagEvent.getCurrentUserId();
        String operateType = batchMakeMposFlagEvent.getType();
        String key = toFlagKey(userId,operateType);
        ExcelExportHelper<AbnormalRecord> helper;
        String contextId= String.valueOf(DateTime.now().getMillis());
        List<Long> skuTemplateIds = Lists.newArrayList();
        try{
            helper = ExcelExportHelper.newExportHelper(AbnormalRecord.class);
            log.info("async handle mpos flag task start");
            //1.开始的时候记录状态
            recordToRedis(key,PsItemConstants.EXECUTING,userId);
            boolean next = batchHandleMposFlag(pageNo,BATCH_SIZE,params,operateType,helper,contextId,skuTemplateIds);
            while(next){
                pageNo++;
                next = batchHandleMposFlag(pageNo,BATCH_SIZE, params,operateType,helper,contextId,skuTemplateIds);
                log.info("async handle mpos flag " + pageNo * 100);
                if(pageNo % 10 == 0){
                    skuTemplateDumpService.batchDump(skuTemplateIds);
                    log.info("update sku template index...");
                    skuTemplateIds.clear();
                }
            }
            skuTemplateDumpService.batchDump(skuTemplateIds);
            //3.结束后判断是否有异常记录，无显示完成，有显示有异常，并显示异常记录。
            if(helper.size() > 0){
                String url = this.uploadToAzureOSS(helper.transformToFile());
                recordToRedis(key,PsItemConstants.EXECUTE_ERROR + "~" + url,userId);
                log.error("async handle mpos flag task abnormality");
            }else{
                recordToRedis(key,PsItemConstants.EXECUTED,userId);
            }
            log.info("async handle mpos flag task end");
        }catch (Exception e){
            log.error("async handle mpos flag task error",Throwables.getStackTraceAsString(e));
            recordToRedis(key,PsItemConstants.SYSTEM_ERROR + "~" + e.getMessage(),userId);
        }
    }

    /**
     * 分批次查询货品并进行处理
     * @param pageNo
     * @param size
     * @param params
     * @param operateType
     * @return
     */
    private Boolean batchHandleMposFlag(int pageNo,int size,Map<String,String> params,String operateType,ExcelExportHelper helper,String contextId,List<Long> skuTemplateIds){
        String templateName = "search.mustache";
        Response<? extends Pagination<SearchSkuTemplate>> response =skutemplateScrollSearcher.searchWithScroll(contextId,pageNo,size, templateName, params, SearchSkuTemplate.class);
        if(!response.isSuccess()){
            log.error("fail to batch handle mpos flag，param={},cause:{}",params,response.getError());
            return Boolean.FALSE;
        }
        List<SearchSkuTemplate> searchSkuTemplates = response.getResult().getData();
        if(response.getResult().getTotal().equals(0L) || CollectionUtils.isEmpty(searchSkuTemplates)){
            return Boolean.FALSE;
        }
        for (SearchSkuTemplate searchSkuTemplate:searchSkuTemplates) {
            operateMposFlag(searchSkuTemplate.getId(),operateType,helper);
            skuTemplateIds.add(searchSkuTemplate.getId());
        }
        int current = searchSkuTemplates.size();
        return current == size;
    }

    /**
     * 打标／取消打标
     * @param id            货品ID
     * @param operateType   0 打标 1 取消打标
     * @param helper        导出excel辅助类
     */
    private void operateMposFlag(Long id, String operateType, ExcelExportHelper helper){
        val rExist = skuTemplateReadService.findById(id);
        if (!rExist.isSuccess()) {
            log.error("find sku template by id:{} fail,error:{}",id,rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        SkuTemplate exist = rExist.getResult();
        Map<String,String> extra = operationMopsFlag(exist, operateType);
        SkuTemplate toUpdate = new SkuTemplate();
        toUpdate.setId(exist.getId());
        toUpdate.setExtra(extra);
        Response<Boolean> resp = psSkuTemplateWriteService.update(toUpdate);
        if (!resp.isSuccess()) {
            log.error("update SkuTemplate failed,cause:{}",resp.getError());
            AbnormalRecord ar = new AbnormalRecord();
            ar.setCode(exist.getExtra().get("materialCode"));
            ar.setReason(resp.getError());
            helper.appendToExcel(ar);
        }
        //同步电商
        if(Objects.equals(PsItemConstants.MPOS_ITEM,operateType)){
            //mpos打标设置默认折扣
            if(!extra.containsKey(PsItemConstants.MPOS_DISCOUNT)){
                setDiscount(id,100);
            }
            pushMposItemComponent.push(exist);
        }else{
            pushMposItemComponent.del(Lists.newArrayList(exist));
        }
    }

    /**
     * 封装货品mpos类型
     * @param exist 货品
     * @param type  类型
     */
    private Map<String,String> operationMopsFlag(SkuTemplate exist,String type){
        Map<String,String> extra = exist.getExtra();
        if(org.springframework.util.CollectionUtils.isEmpty(extra)){
            extra = Maps.newHashMap();
        }
        extra.put(PsItemConstants.MPOS_FLAG,type);
        return extra;
    }

    /**
     * 监听导出文件事件
     * @param exportMposDiscountEvent
     * @throws IOException
     */
    @Subscribe
    public void onExportMposDiscount(BatchAsyncExportMposDiscountEvent exportMposDiscountEvent) throws IOException {
        int pageNo = 1;
        Map<String,String> params = exportMposDiscountEvent.getParams();
        Long userId = exportMposDiscountEvent.getCurrentUserId();
        ExcelExportHelper<SearchSkuTemplateEntity> helper = ExcelExportHelper.newExportHelper(SearchSkuTemplateEntity.class);
        log.info("async export mpos task start");
        String fileName = DateTime.now().toString("yyyyMMddHHmmss") + ".xls";
        String key = toUploadKey(userId,fileName,PsItemConstants.EXPORT_TASK);
        String contextId= String.valueOf(DateTime.now().getMillis());
        recordToRedis(key,PsItemConstants.EXECUTING,userId);
        boolean next = batchExportMposDiscount(pageNo,BATCH_SIZE,params,helper,contextId);
        while(next){
            pageNo++;
            next = batchExportMposDiscount(pageNo,BATCH_SIZE, params,helper,contextId);
            log.debug(pageNo + "exported");
        }
        File file = helper.transformToFile(fileName);
        String uploadUrl = this.uploadToAzureOSS(file);
        recordToRedis(key,PsItemConstants.EXECUTED + "~" + uploadUrl,userId);
        log.info("async export mpos task end");
    }

    /**
     *  分批次处理数据
     * @param pageNo
     * @param size
     * @param params
     * @return
     */
    public Boolean batchExportMposDiscount(int pageNo, int size, Map<String,String> params, ExcelExportHelper helper,String contextId){
        String templateName = "search.mustache";

        Response<? extends Pagination<SearchSkuTemplate>> response =skutemplateScrollSearcher.searchWithScroll(contextId,pageNo,size, templateName, params, SearchSkuTemplate.class);

        if(!response.isSuccess()){
            log.error("fail to batch handle mpos flag，param={},cause:{}",params,response.getError());
            return Boolean.FALSE;
        }
        List<SearchSkuTemplate> searchSkuTemplates = response.getResult().getData();
        if(response.getResult().getTotal().equals(0L) || CollectionUtils.isEmpty(searchSkuTemplates)){
            return Boolean.FALSE;
        }
        assembleSkuInfo(searchSkuTemplates);
        for (SearchSkuTemplate searchSkuTemplate:searchSkuTemplates) {
            //过滤掉价格为空的数据
            if(searchSkuTemplate.getOriginPrice() != null && searchSkuTemplate.getOriginPrice() > 0){
                //转换成符合格式的实体类
                SearchSkuTemplateEntity entity = new SearchSkuTemplateEntity(searchSkuTemplate);
                helper.appendToExcel(entity);
            }
        }
        int current = searchSkuTemplates.size();
        return current == size;
    }

    /**
     * 文件上传至微软云
     * @param file  文件
     * @return      文件url
     */
    private String uploadToAzureOSS(File file){
        String url = null;
        try{
            url = azureOssBlobClient.upload(file, DEFAULT_CLOUD_PATH);
            log.info("the azure blob url:{}", url);
            log.info("delete local file:{}", file.getPath());
            if (!file.delete())
                log.warn("delete local file fail:{}", file.getPath());
        }catch (Exception e){
            log.error(" fail upload file {} to azure,cause:{}",file.getName(),Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("fail upload file to azure");
        }
        return url;
    }


    /**
     * 监听导入文件设置折扣事件
     * @param event
     */
    @Subscribe
    public void onImportMposDiscount(BatchAsyncImportMposDiscountEvent event) {
        log.info("async import mpos discount task start");
        MultipartFile file = event.getFile();
        Long userId = event.getCurrentUserId();
        String key = toImportKey(userId,file.getOriginalFilename());
        ExcelExportHelper<SearchSkuTemplateEntity> helper;
        List<String[]> list;
        try{
            recordToRedis(key,PsItemConstants.EXECUTING,userId);
            helper = ExcelExportHelper.newExportHelper(SearchSkuTemplateEntity.class);
            list = ExcelUtil.readerExcel(file.getInputStream(),"Sheet0",15);
            if(CollectionUtils.isEmpty(list) || list.get(0).length != 15)
                throw new Exception();
        }catch (Exception e){
            log.error("illegal file");
            recordToRedis(key,PsItemConstants.IMPORT_FILE_ILLEGAL,userId);
            throw new JsonResponseException("illegal file");
        }
        for (int i = 1;i<list.size();i++) {
            String[] strs = list.get(i);
            if(!Strings.isNullOrEmpty(strs[11]) && !"\"\"".equals(strs[11])){
                try{
                    strs[14] = "";
                    Long id = Long.parseLong(strs[0].replace("\"",""));
                    Integer discount = Integer.valueOf(strs[11].replace("\"",""));
                    if(discount > 100 || discount < 1){
                        throw new JsonResponseException(PsItemConstants.ERROR_NUMBER_ILLEGAL);
                    }
                    setDiscount(id,discount);
                }catch (NumberFormatException nfe){
                    log.error("set discount fail,spucode={},discount={},cause:{}",strs[1],strs[11], Throwables.getStackTraceAsString(nfe));
                    strs[14] = PsItemConstants.ERROR_FORMATE_ERROR;
                }catch (JsonResponseException jre){
                    strs[14] = jre.getMessage();
                }finally{
                    if(!StringUtils.isEmpty(strs[14])){
                        SearchSkuTemplateEntity entity = new SearchSkuTemplateEntity(strs);
                        helper.appendToExcel(entity);
                    }
                }
            }
        }
        if(helper.size() > 0){
            String url = this.uploadToAzureOSS(helper.transformToFile());
            recordToRedis(key,PsItemConstants.EXECUTE_ERROR + "~" + url,userId);
            log.error("async import mpos discount task abnormality");
        }else{
            recordToRedis(key,PsItemConstants.EXECUTED,userId);
        }
        log.info("async import mpos discount task end");
    }

    /**
     * 设置折扣
     * @param id        货品id
     * @param discount  折扣
     */
    private void setDiscount(Long id, Integer discount) {
        val rExist = skuTemplateReadService.findById(id);
        if (!rExist.isSuccess()) {
            log.error("find sku template by id:{} fail,error:{}",id,rExist.getError());
            throw new JsonResponseException(PsItemConstants.ERROR_NOT_FIND);
        }
        SkuTemplate exist = rExist.getResult();
        Map<String,String> extra = setMopsDiscount(exist,discount);
        SkuTemplate toUpdate = new SkuTemplate();
        Integer originPrice = 0;
        if (exist.getExtraPrice() != null&&exist.getExtraPrice().containsKey(PsItemConstants.ORIGIN_PRICE_KEY)) {
            originPrice = exist.getExtraPrice().get(PsItemConstants.ORIGIN_PRICE_KEY);
        }
        toUpdate.setId(exist.getId());
        toUpdate.setExtra(extra);
        toUpdate.setPrice(calculatePrice(discount,originPrice));
        Response<Boolean> resp = psSkuTemplateWriteService.update(toUpdate);
        if (!resp.isSuccess()) {
            log.error("update SkuTemplate failed error={}",resp.getError());
            throw new JsonResponseException(PsItemConstants.ERROR_UPDATE_FAIL);
        }
        //同步电商
        pushMposItemComponent.updatePrice(Lists.newArrayList(exist),toUpdate.getPrice());
    }

    /**
     * 设置折扣
     * @param exist     货品
     * @param discount  折扣
     * @return
     */
    private Map<String,String> setMopsDiscount(SkuTemplate exist,Integer discount){
        Map<String,String> extra = exist.getExtra();
        if(CollectionUtils.isEmpty(extra)){
            extra = Maps.newHashMap();
        }
        extra.put(PsItemConstants.MPOS_DISCOUNT,discount.toString());
        return extra;
    }

    private static Integer calculatePrice(Integer discount, Integer originPrice){
        BigDecimal ratio = new BigDecimal("100");  // 百分比的倍率
        BigDecimal discountDecimal = new BigDecimal(discount);
        BigDecimal percentDecimal =  discountDecimal.divide(ratio,2, BigDecimal.ROUND_HALF_UP);
        return percentDecimal.multiply(BigDecimal.valueOf(originPrice)).intValue();
    }

    /**
     *  封装价格和销售属性信息
     *  @param data 未整理数据
     */
    private void assembleSkuInfo(List<SearchSkuTemplate> data) {
        if(CollectionUtils.isEmpty(data)){
            return;
        }
        Map<Long,SkuTemplate> groupSkuTemplateById = groupSkuTemplateById(data);
        Map<Long, SpuAttribute> groupSpuAttributebySpuId = groupSpuAttributebySpuId(data);
        for (SearchSkuTemplate searchSkuTemplate : data){
            SkuTemplate skuTemplate = groupSkuTemplateById.get(searchSkuTemplate.getId());
            if (skuTemplate.getExtraPrice() != null) {
                searchSkuTemplate.setOriginPrice(skuTemplate.getExtraPrice().get(PsItemConstants.ORIGIN_PRICE_KEY));
            }
            searchSkuTemplate.setPrice(skuTemplate.getPrice());
            Map<String,String> extra = skuTemplate.getExtra();
            if(!CollectionUtils.isEmpty(extra)&&extra.containsKey(PsItemConstants.MPOS_DISCOUNT)){
                searchSkuTemplate.setDiscount(Integer.valueOf(extra.get(PsItemConstants.MPOS_DISCOUNT)));
            }
            SpuAttribute spuAttribute = groupSpuAttributebySpuId.get(searchSkuTemplate.getSpuId());
            if(Arguments.notNull(spuAttribute)){
                searchSkuTemplate.setOtherAttrs(spuAttribute.getOtherAttrs());
            }
            searchSkuTemplate.setAttrs(skuTemplate.getAttrs());
            searchSkuTemplate.setMainImage(skuTemplate.getImage_());
        }
    }

    /**
     * 根据ID查询货品详情
     * @param data
     * @return
     */
    private Map<Long,SkuTemplate> groupSkuTemplateById(List<SearchSkuTemplate> data){
        List<Long> skuTemplateIds = Lists.transform(data, new Function<SearchSkuTemplate, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable SearchSkuTemplate input) {
                return input.getId();
            }
        });
        Response<List<SkuTemplate>>  skuTemplateRes = skuTemplateReadService.findByIds(skuTemplateIds);
        if(!skuTemplateRes.isSuccess()){
            log.error("find sku template by ids:{} fail,error:{}",skuTemplateIds,skuTemplateRes.getError());
            throw new JsonResponseException(skuTemplateRes.getError());
        }
        return Maps.uniqueIndex(skuTemplateRes.getResult(), new Function<SkuTemplate, Long>() {
            @Override
            public Long apply(SkuTemplate skuTemplate) {
                return skuTemplate.getId();
            }
        });
    }

    /**
     * 根据ID获取货品属性
     * @param data
     * @return
     */
    private Map<Long, SpuAttribute> groupSpuAttributebySpuId(List<SearchSkuTemplate> data){
        List<Long> spuIds = Lists.transform(data, new Function<SearchSkuTemplate, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable SearchSkuTemplate input) {
                return input.getSpuId();
            }
        });
        Response<List<SpuAttribute>> spuAttributeRes = psSpuAttributeReadService.findBySpuIds(spuIds);
        if(!spuAttributeRes.isSuccess()){
            log.error("find sku spu attribute by spu ids:{} fail,error:{}",spuIds,spuAttributeRes.getError());
            throw new JsonResponseException(spuAttributeRes.getError());
        }
        return Maps.uniqueIndex(spuAttributeRes.getResult(), new Function<SpuAttribute, Long>() {
            @Override
            public Long apply(SpuAttribute spuAttribute) {
                return spuAttribute.getSpuId();
            }
        });
    }

    /**
     * 生成key
     * @param userId
     * @param operatorType
     * @return
     */
    private String toFlagKey(Long userId,String operatorType){
        return "mpos:" + userId + ":flag:" + operatorType + "~" + DateTime.now().toDate().getTime();
    }

    private String toUploadKey(Long userId,String fileName,String operateType){
        return "mpos:" + userId + ":" + operateType + ":" + fileName + "~" + DateTime.now().toDate().getTime();
    }

    private String toImportKey(Long userId,String fileName){
        return "mpos:" + userId + ":" + PsItemConstants.IMPORT_TASK + ":"+ fileName + "~" + DateTime.now().toDate().getTime();
    }

    /**
     * 保存记录
     * @param key
     * @param value
     * @param userId
     */
    private void recordToRedis(String key,String value,Long userId){
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                if(userId == null){
                    log.error("fail save record to redis cause:can not get current user");
                    throw new JsonResponseException("fail save record to redis");
                }
                try{
                    jedis.setex(key,BATCH_RECORD_EXPIRE_TIME,value);
                    log.info(key + " save to redis success");
                }catch(Exception e){
                    log.error(key + "record save to redis fail");
                    throw new JsonResponseException("fail save record to redis");
                }
            }
        });
    }
}
