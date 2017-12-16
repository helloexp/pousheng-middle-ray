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
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.web.events.item.BatchAsyncExportMposDiscountEvent;
import com.pousheng.middle.web.events.item.BatchAsyncHandleMposFlagEvent;
import com.pousheng.middle.web.events.item.BatchAsyncImportMposDiscountEvent;
import com.pousheng.middle.web.events.item.SkuTemplateUpdateEvent;
import com.pousheng.middle.web.export.SearchSkuTemplateEntity;
import com.pousheng.middle.web.item.batchhandle.AbnormalRecord;
import com.pousheng.middle.web.item.batchhandle.ExcelExportHelper;
import com.pousheng.middle.web.item.batchhandle.ExcelUtil;
import com.pousheng.middle.web.utils.export.*;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.parana.search.dto.SearchedItemWithAggs;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;
import redis.clients.jedis.Jedis;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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
    private SkuTemplateSearchReadService skuTemplateSearchReadService;

    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;

    @RpcConsumer
    private PsSkuTemplateWriteService psSkuTemplateWriteService;

    @Autowired
    private JedisTemplate jedisTemplate;

    @Autowired
    private AzureOSSBlobClient azureOssBlobClient;

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
        ExcelExportHelper<AbnormalRecord> helper = ExcelExportHelper.newExportHelper(AbnormalRecord.class);
        log.info("asnc handle mpos flag task start");
        //1.开始的时候记录状态
        recordToRedis(key,PsItemConstants.EXECUTING,userId);
        boolean next = batchHandleMposFlag(pageNo,BATCH_SIZE,params,operateType,helper);
        while(next && pageNo < 100){
            pageNo++;
            next = batchHandleMposFlag(pageNo,BATCH_SIZE, params,operateType,helper);
        }
        //3.结束后判断是否有异常记录，无显示完成，有显示有异常，并显示异常记录。
        if(helper.size() > 0){
            String url = this.uploadToAzureOSS(helper.transformToFile());
            recordToRedis(key,PsItemConstants.EXECUTE_ERROR + ":" + url,userId);
            log.error("async handle mpos flag task abnormality");
        }else{
            recordToRedis(key,PsItemConstants.EXECUTED,userId);
        }
        log.info("async handle mpos flag task end");
    }

    /**
     * 分批次查询货品并进行处理
     * @param pageNo
     * @param size
     * @param params
     * @param operateType
     * @return
     */
    private Boolean batchHandleMposFlag(int pageNo,int size,Map<String,String> params,String operateType,ExcelExportHelper helper){
        String templateName = "search.mustache";
        Response<? extends SearchedItemWithAggs<SearchSkuTemplate>> response = skuTemplateSearchReadService.searchWithAggs(pageNo,size, templateName, params, SearchSkuTemplate.class);
        if(!response.isSuccess()){
            log.error("fail to batch handle mpos flag，param={},cause:{}",params,response.getError());
            return Boolean.FALSE;
        }
        List<SearchSkuTemplate> searchSkuTemplates = response.getResult().getEntities().getData();
        if(response.getResult().getEntities().getTotal().equals(0L) || CollectionUtils.isEmpty(searchSkuTemplates)){
            return Boolean.FALSE;
        }
        for (SearchSkuTemplate searchSkuTemplate:searchSkuTemplates) {
            operateMposFlag(searchSkuTemplate.getId(),operateType,helper);
        }
        int current = searchSkuTemplates.size();
        return current == size;
    }

    /**
     * 打标／取消打标
     * @param id
     * @param operateType 0 打标 1 取消打标
     * @taskId 任务ID 用户记录异常日志
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
        postUpdateSearchEvent(id);
    }

    /**
     * 打标／取消打标
     * @param exist
     * @param type
     * @return
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
     * 更新搜索
     * @param skuTemplateId
     */
    private void postUpdateSearchEvent(Long skuTemplateId){
        SkuTemplateUpdateEvent updateEvent = new SkuTemplateUpdateEvent();
        updateEvent.setSkuTemplateId(skuTemplateId);
        eventBus.post(updateEvent);
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
        boolean next = batchExportMposDiscount(pageNo,BATCH_SIZE,params,helper);
        while(next && pageNo < 100){
            pageNo++;
            next = batchExportMposDiscount(pageNo,BATCH_SIZE, params,helper);
            log.debug(pageNo + "exported");
        }
        File file = helper.transformToFile();
        String uploadUrl = this.uploadToAzureOSS(file);
        String fileName = file.getName();
        if (fileName.contains(File.separator)) {
            fileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1);
        }
        recordToRedis(toUploadKey(userId,uploadUrl,"export"),uploadUrl,userId);
        log.info("async export mpos task end");
    }



    /**
     *  分批次处理数据
     * @param pageNo
     * @param size
     * @param params
     * @return
     */
    public Boolean batchExportMposDiscount(int pageNo, int size, Map<String,String> params, ExcelExportHelper helper){
        String templateName = "search.mustache";
        Response<? extends SearchedItemWithAggs<SearchSkuTemplate>> response =skuTemplateSearchReadService.searchWithAggs(pageNo,size, templateName, params, SearchSkuTemplate.class);
        if(!response.isSuccess()){
            log.error("fail to batch handle mpos flag，param={},cause:{}",params,response.getError());
            return Boolean.FALSE;
        }
        List<SearchSkuTemplate> searchSkuTemplates = response.getResult().getEntities().getData();
        if(response.getResult().getEntities().getTotal().equals(0L) || CollectionUtils.isEmpty(searchSkuTemplates)){
            return Boolean.FALSE;
        }
        assembleSkuInfo(searchSkuTemplates);
        for (SearchSkuTemplate searchSkuTemplate:searchSkuTemplates) {
            //转换成符合格式的实体类
            SearchSkuTemplateEntity entity = new SearchSkuTemplateEntity(searchSkuTemplate);
            helper.appendToExcel(entity);
        }
        int current = searchSkuTemplates.size();
        return current == size;
    }

    /**
     * 文件上传至微软云
     * @param file
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


    @Subscribe
    public void onImportMposDiscount(BatchAsyncImportMposDiscountEvent event) throws OpenXML4JException, ParserConfigurationException, SAXException, IOException {
        log.info("async import mpos discount task start");
        MultipartFile file = event.getFile();
        Long userId = event.getCurrentUserId();
        String key = toImportKey(userId,file.getOriginalFilename());
        ExcelExportHelper<SearchSkuTemplateEntity> helper = ExcelExportHelper.newExportHelper(SearchSkuTemplateEntity.class);
        recordToRedis(key,PsItemConstants.EXECUTING,userId);
        List<String[]> list = ExcelUtil.readerExcel(file.getInputStream(),"Sheet0",11);
        for (int i = 1;i<list.size();i++) {
            String[] strs = list.get(i);
            if(!Strings.isNullOrEmpty(strs[8]) && !"\"\"".equals(strs[8])){
                try{
                    Long id = Long.parseLong(strs[0].replace("\"",""));
                    Integer discount = Integer.valueOf(strs[8]);
                    setDiscount(id,discount);
                }catch (NumberFormatException nfe){
                    log.error("set discount fail,spucode={},discount={},cause:{}",strs[1],strs[8], Throwables.getStackTraceAsString(nfe));
                    strs[11] = "格式错误";
                }catch (JsonResponseException jre){
                    strs[11] = jre.getMessage();
                }finally{
                    if(!StringUtils.isEmpty(strs[11])){
                        SearchSkuTemplateEntity entity = new SearchSkuTemplateEntity(strs);
                        helper.appendToExcel(entity);
                    }
                }
            }
        }
        if(helper.size() > 0){
            recordToRedis(key,PsItemConstants.EXECUTE_ERROR,userId);
        }else{
            recordToRedis(key,PsItemConstants.EXECUTED,userId);
        }
        log.info("async import mpos discount task end");
    }

    /**
     * 设置折扣
     * @param id
     * @param discount
     */
    private void setDiscount(Long id, Integer discount) {
        val rExist = skuTemplateReadService.findById(id);
        if (!rExist.isSuccess()) {
            log.error("find sku template by id:{} fail,error:{}",id,rExist.getError());
            throw new JsonResponseException("未找到该货品");
        }
        SkuTemplate exist = rExist.getResult();
        Map<String,String> extra = setMopsDiscount(exist,discount);
        SkuTemplate toUpdate = new SkuTemplate();
        toUpdate.setId(exist.getId());
        toUpdate.setExtra(extra);
        Response<Boolean> resp = psSkuTemplateWriteService.update(toUpdate);
        if (!resp.isSuccess()) {
            log.error("update SkuTemplate failed error={}",resp.getError());
            throw new JsonResponseException("更新失败");
        }
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

    /**
     * 封装商品信息
     * @param data
     */
    private void assembleSkuInfo(List<SearchSkuTemplate> data) {
        Map<Long,SkuTemplate> groupSkuTemplateById = groupSkuTemplateById(data);
        for (SearchSkuTemplate searchSkuTemplate : data){
            SkuTemplate skuTemplate = groupSkuTemplateById.get(searchSkuTemplate.getId());
            if (skuTemplate.getExtraPrice() != null) {
                searchSkuTemplate.setOriginPrice(skuTemplate.getExtraPrice().get(PsItemConstants.ORIGIN_PRICE_KEY));
            }
            searchSkuTemplate.setPrice(skuTemplate.getPrice());
            Map<String,String> extra = skuTemplate.getExtra();
            if(CollectionUtils.isEmpty(extra) && extra.containsKey(PsItemConstants.MPOS_DISCOUNT)){
                searchSkuTemplate.setDiscount(Integer.valueOf(extra.get(PsItemConstants.MPOS_DISCOUNT)));
            }
            searchSkuTemplate.setAttrs(skuTemplate.getAttrs());
        }
    }

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
        return "mpos:" + userId + ":import:"+ fileName + "~" + DateTime.now().toDate().getTime();
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
