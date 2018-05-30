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
import com.pousheng.middle.item.enums.PsSpuType;
import com.pousheng.middle.item.service.PsSkuTemplateWriteService;
import com.pousheng.middle.item.service.PsSpuAttributeReadService;
import com.pousheng.middle.item.service.SkuTemplateDumpService;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.web.events.item.BatchAsyncExportMposDiscountEvent;
import com.pousheng.middle.web.events.item.BatchAsyncHandleMposFlagEvent;
import com.pousheng.middle.web.events.item.BatchAsyncImportMposDiscountEvent;
import com.pousheng.middle.web.events.item.BatchAsyncImportMposFlagEvent;
import com.pousheng.middle.web.export.SearchSkuTemplateEntity;
import com.pousheng.middle.web.item.batchhandle.AbnormalRecord;
import com.pousheng.middle.web.item.batchhandle.BatchHandleMposLogic;
import com.pousheng.middle.web.item.batchhandle.ExcelExportHelper;
import com.pousheng.middle.web.item.batchhandle.ExcelUtil;
import com.pousheng.middle.web.item.component.PushMposItemComponent;
import com.pousheng.middle.web.item.component.SkutemplateScrollSearcher;
import com.pousheng.middle.web.utils.export.AzureOSSBlobClient;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.search.dto.SearchedItemWithAggs;
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
    @RpcConsumer
    private SkuTemplateSearchReadService skuTemplateSearchReadService;

    @Autowired
    private BatchHandleMposLogic batchHandleMposLogic;



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
        Integer operateType = batchMakeMposFlagEvent.getType();
        String key = toFlagKey(userId,operateType);
        String contextId= String.valueOf(DateTime.now().getMillis());
        List<Long> skuTemplateIds = Lists.newArrayList();
        try{
            log.info("async handle mpos flag task start......");
            //1.开始的时候记录状态
            recordToRedis(key,PsItemConstants.EXECUTING,userId);
            boolean next = batchHandleMposFlag(pageNo,BATCH_SIZE,params,operateType,contextId,skuTemplateIds);
            while(next){
                pageNo++;
                next = batchHandleMposFlag(pageNo,BATCH_SIZE, params,operateType,contextId,skuTemplateIds);
                log.info("async handle mpos flag " + pageNo * 100);
                if(pageNo % 10 == 0){

                    Response<List<SkuTemplate>> listRes = skuTemplateReadService.findByIds(skuTemplateIds);
                    if(!listRes.isSuccess()){
                        log.error("find sku template by ids:{} fail,error:{}",skuTemplateIds,listRes.getError());
                        continue;
                    }
                    List<SkuTemplate> skuTemplateLists = listRes.getResult();
                    skuTemplateDumpService.batchDump(skuTemplateLists,operateType);
                    pushMposItemComponent.batchMakeFlag(skuTemplateLists,operateType);
                    skuTemplateIds.clear();
                }
            }

            //非1000条的更新下
            if(!CollectionUtils.isEmpty(skuTemplateIds)){

                Response<List<SkuTemplate>> listRes = skuTemplateReadService.findByIds(skuTemplateIds);
                if(!listRes.isSuccess()){
                    log.error("find sku template by ids:{} fail,error:{}",skuTemplateIds,listRes.getError());
                    return;
                }
                List<SkuTemplate> skuTemplateLists = listRes.getResult();

                //批量更新es
                skuTemplateDumpService.batchDump(skuTemplateLists,operateType);
                //批量打标
                pushMposItemComponent.batchMakeFlag(skuTemplateLists,operateType);
            }
            recordToRedis(key,PsItemConstants.EXECUTED,userId);
            log.info("async handle mpos flag task end......");
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
    private Boolean batchHandleMposFlag(int pageNo,int size,Map<String,String> params,Integer operateType,String contextId,List<Long> skuTemplateIds){
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
            syncParanaMposSku(searchSkuTemplate.getId(),operateType);
            skuTemplateIds.add(searchSkuTemplate.getId());
        }
        int current = searchSkuTemplates.size();
        return current == size;
    }

    /**
     * 打标／取消打标
     * @param id            货品ID
     * @param operateType   1 非mpos 2mpos
     */
    private void syncParanaMposSku(Long id, Integer operateType){
        val rExist = skuTemplateReadService.findById(id);
        if (!rExist.isSuccess()) {
            log.error("find sku template by id:{} fail,error:{}",id,rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        SkuTemplate exist = rExist.getResult();
        //同步电商
        if(Objects.equals(PsSpuType.MPOS.value(),operateType)){
            pushMposItemComponent.push(exist);
        }else{
            pushMposItemComponent.del(Lists.newArrayList(exist));
        }
    }


    /**
     * 同步电商
     * @param exist 货品
     */
    private Response<Boolean> syncParanaMposSku(SkuTemplate exist){
        return pushMposItemComponent.syncParanaMposItem(exist);
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
                    batchHandleMposLogic.setDiscount(id,discount);
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
     * 监听导入文件打标事件
     * @param event
     */
    @Subscribe
    public void onImportMposFlag(BatchAsyncImportMposFlagEvent event) {
        log.info("async import mpos flag task start");
        MultipartFile file = event.getFile();
        Long userId = event.getCurrentUserId();
        String key = toFlagKey(userId,2);
        ExcelExportHelper<AbnormalRecord> helper;
        List<String[]> list;
        try{
            recordToRedis(key,PsItemConstants.EXECUTING,userId);
            helper = ExcelExportHelper.newExportHelper(AbnormalRecord.class);
            list = ExcelUtil.readerExcel(file.getInputStream(),"Sheet0",5);
            if(CollectionUtils.isEmpty(list)){
                log.error("import excel is empty so skip");
                throw new JsonResponseException("excel.content.is.empty");
            }
            log.info("import excel size:{}",list.size());
        }catch (Exception e){
            log.error("read import excel file fail,causeL:{}",Throwables.getStackTraceAsString(e));
            recordToRedis(key,PsItemConstants.IMPORT_FILE_ILLEGAL,userId);
            throw new JsonResponseException("read.excel.fail");
        }

        List<SkuTemplate> skuTemplates = Lists.newArrayList();

        for (int i = 0;i<list.size();i++) {
            String[] strs = list.get(i);
            if(!Strings.isNullOrEmpty(strs[3]) && !"\"\"".equals(strs[3])){
                    //sku编码
                    String skuCode = strs[3].replace("\"", "");
                try {
                    SearchSkuTemplate searchSkuTemplate = findMposSkuTemplate(skuCode);
                    //不存在记录日志
                    if (Arguments.isNull(searchSkuTemplate)) {
                        appendErrorToExcel(helper,strs,"打标失败：中台不存在该商品");
                        log.error("import make sku code:{} flag fail, error:{}",skuCode,"中台不存在该商品");
                        continue;
                    }
                    Long skuTemplateId = searchSkuTemplate.getId();

                    //判断商品是否有效
                    Response<SkuTemplate> skuTemplateRes = skuTemplateReadService.findById(skuTemplateId);
                    if(!skuTemplateRes.isSuccess()){
                        log.error("find sku template by id:{} fail,error:{}",skuTemplateId,skuTemplateRes.getError());
                        appendErrorToExcel(helper,strs,"打标失败："+skuTemplateRes.getError());
                        continue;
                    }

                    SkuTemplate skuTemplate = skuTemplateRes.getResult();

                    //同步电商
                    //syncParanaMposSku(skuTemplateId, PsSpuType.MPOS.value());
                    Response<Boolean> syncParanaRes = syncParanaMposSku(skuTemplate);
                    if(!syncParanaRes.isSuccess()){
                        log.error("sync parana mpos item(sku template id:{}) fail",skuTemplateId);
                        appendErrorToExcel(helper,strs,"同步电商失败："+syncParanaRes.getError());
                        continue;
                    }

                    //设置默认折扣 和价格
                    /*Response<Boolean> makeFlagRes = batchHandleMposLogic.makeFlagAndSetDiscount(skuTemplate,PsSpuType.MPOS.value());
                    if(!makeFlagRes.isSuccess()){
                        log.error("make flag (sku template id:{}) fail",skuTemplateId);
                        appendErrorToExcel(helper,strs,"打标失败："+makeFlagRes.getError());
                        continue;
                    }*/


                    skuTemplates.add(skuTemplate);

                    //每1000条更新下mysql和search
                    if (i % 1000 == 0) {
                        //更新es
                        skuTemplateDumpService.batchDump(skuTemplates,2);
                        //设置默认折扣 和价格
                        pushMposItemComponent.batchMakeFlag(skuTemplates,PsSpuType.MPOS.value());
                        skuTemplates.clear();
                    }
                }catch (Exception jre){
                    appendErrorToExcel(helper,strs,"处理失败");
                    log.error("import make sku code:{} flag fail, cause:{}",skuCode,Throwables.getStackTraceAsString(jre));
                }
            }
        }

        //非1000条的更新下
        if(!CollectionUtils.isEmpty(skuTemplates)){
            skuTemplateDumpService.batchDump(skuTemplates,2);
            pushMposItemComponent.batchMakeFlag(skuTemplates,PsSpuType.MPOS.value());
        }
        if(helper.size() > 0){
            String url = this.uploadToAzureOSS(helper.transformToFile());
            recordToRedis(key,PsItemConstants.EXECUTE_ERROR + "~" + url,userId);
            log.error("async import mpos discount task abnormality");
        }else{
            recordToRedis(key,PsItemConstants.EXECUTED,userId);
        }
        log.info("async import mpos flag task end");
    }

    private void appendErrorToExcel(ExcelExportHelper<AbnormalRecord> helper,String[] strs,String error){
        AbnormalRecord abnormalRecord = new AbnormalRecord();
        abnormalRecord.setCode(strs[0].replace("\"", ""));
        abnormalRecord.setSize(strs[1].replace("\"", ""));
        abnormalRecord.setName(strs[2].replace("\"", ""));
        abnormalRecord.setSkuCode(strs[3].replace("\"", ""));
        abnormalRecord.setReason(error);
        helper.appendToExcel(abnormalRecord);
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
    private String toFlagKey(Long userId,Integer operatorType){
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

    private SearchSkuTemplate findMposSkuTemplate(String skuCode){

        //1、根据货号和尺码查询 spuCode=20171214001&attrs=年份:2017
        String templateName = "search.mustache";
        Map<String,String> params = Maps.newHashMap();
        params.put("skuCode",skuCode);
        Response<? extends SearchedItemWithAggs<SearchSkuTemplate>> response =skuTemplateSearchReadService.searchWithAggs(1,20, templateName, params, SearchSkuTemplate.class);
        if(!response.isSuccess()){
            log.error("query sku template by skuCode:{} fail,error:{}",skuCode,response.getError());
            throw new JsonResponseException(response.getError());
        }

        List<SearchSkuTemplate> searchSkuTemplates = response.getResult().getEntities().getData();
        if(CollectionUtils.isEmpty(searchSkuTemplates)){
            return null;
        }

        return searchSkuTemplates.get(0);

    }
}
