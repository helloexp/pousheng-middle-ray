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
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.search.dto.SearchedItemWithAggs;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;
import redis.clients.jedis.Jedis;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
        String taskId = this.taskId();
        Map<String,String> params = batchMakeMposFlagEvent.getParams();
        Long userId = batchMakeMposFlagEvent.getCurrentUserId();
        String operateType = batchMakeMposFlagEvent.getType();
        String key = toKey(userId,operateType,taskId);
        ExcelExportHelper<AbnormalRecord> helper = ExcelExportHelper.newExportHelper(AbnormalRecord.class);
        log.info("ayncs handle mpos flag task start");
        //1.开始的时候记录状态
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                jedis.setex(key,BATCH_RECORD_EXPIRE_TIME,PsItemConstants.EXECUTING);
            }
        });
        boolean next = batchHandleMposFlag(pageNo,BATCH_SIZE,params,operateType,taskId,userId,helper);
        while(next && pageNo < 100){
            pageNo++;
            next = batchHandleMposFlag(pageNo,BATCH_SIZE, params,operateType,taskId,userId,helper);
        }
        //3.结束后判断是否有异常记录，无显示完成，有显示有异常，并显示异常记录。
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                //1.如果有异常记录，置2，无异常，置1
                if(jedis.exists("mpos:"+ userId+":abnormal:flag:"+taskId)){
                    jedis.setex(key,BATCH_RECORD_EXPIRE_TIME,PsItemConstants.EXECUTE_ERROR);
                }else{
                    jedis.setex(key,BATCH_RECORD_EXPIRE_TIME,PsItemConstants.EXECUTED);
                }
            }
        });
        log.info("ayncs handle mpos flag task over");
    }

    /**
     * 分批次查询货品并进行处理
     * @param pageNo
     * @param size
     * @param params
     * @param operateType
     * @return
     */
    private Boolean batchHandleMposFlag(int pageNo,int size,Map<String,String> params,String operateType,String taskId,Long userId,ExcelExportHelper helper){
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
            operateMposFlag(searchSkuTemplate.getId(),operateType,taskId,userId,helper);
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
    private void operateMposFlag(Long id, String operateType, String taskId, Long userId, ExcelExportHelper helper){
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
            log.error("update SkuTemplate failed error={}",resp.getError());
            AbnormalRecord ar = new AbnormalRecord();
            ar.setCode(exist.getExtra().get("materialCode"));
            ar.setReason(resp.getError());
            helper.appendToExcel(ar);
            throw new JsonResponseException(500, resp.getError());
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
        Map<String,String> params = exportMposDiscountEvent.getParams();
        log.info("ayncs export mpos task start");
        Long userId = exportMposDiscountEvent.getCurrentUserId();
        int pageNo = 1;
        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet();
        sheet.protectSheet("terminus");
        List<String> filedNames = setUpTitleRow(SearchSkuTemplateEntity.class,sheet.createRow(0));
        boolean next = batchExportMposDiscount(pageNo,BATCH_SIZE,params,wb,sheet,filedNames);
        while(next && pageNo < 100){
            pageNo++;
            next = batchExportMposDiscount(pageNo,BATCH_SIZE, params,wb,sheet,filedNames);
            log.debug(pageNo + "exporting");
        }
        File file = new File(".", DateTime.now().toString("yyyyMMddHHmmss") + ".xls");
        try{
            if(!file.exists()){
                file.createNewFile();
            }
            FileOutputStream out = new FileOutputStream(file);
            wb.write(out);
        } catch (IOException e) {
            throw new RuntimeException("save export result to file fail", e);
        }
        this.uploadToAzureOSS(file,userId,"export");
        log.info("async export mpos task over");
    }



    /**
     *  分批次处理数据
     * @param pageNo
     * @param size
     * @param params
     * @param wb
     * @param sheet
     * @param filedNames
     * @return
     */
    public Boolean batchExportMposDiscount(int pageNo, int size, Map<String,String> params, Workbook wb,Sheet sheet,List<String> filedNames){
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
        appendToExcel(searchSkuTemplates,wb,sheet,filedNames);
        int current = searchSkuTemplates.size();
        return current == size;
    }

    /**
     * 文件上传至微软云
     * @param file
     */
    private void uploadToAzureOSS(File file,Long currentUserID,String operateType){
        String fileName = file.getName();
        //String url = azureOssBlobClient.upload(file, DEFAULT_CLOUD_PATH);
        String url = "test";
        log.info("the azure blob url:{}", url);
        boolean isStoreToRedisFileInfoSuccess = jedisTemplate.execute(new JedisTemplate.JedisAction<Boolean>() {
            @Override
            public Boolean action(Jedis jedis) {
                try {
                    String realName = fileName;
                    if (fileName.contains(File.separator)) {
                        realName = fileName.substring(fileName.lastIndexOf(File.separator) + 1);
                    }
                    System.out.println(UserUtil.getUserId());
                    if (null == currentUserID) {
                        log.error("cant't get current login user");
                        return false;
                    }
                    //redis记录日志
                    jedis.setex(toUploadKey(currentUserID, realName,"export"), BATCH_RECORD_EXPIRE_TIME, url);
                    log.info("save user:{}'s export file azure url to redis", currentUserID);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });
        if (isStoreToRedisFileInfoSuccess) {
            log.info("delete local file:{}", file.getPath());
            if (!file.delete())
                log.warn("delete local file fail:{}", file.getPath());
        }
    }

    /**
     * 追加数据至excel
     * @param searchSkuTemplates
     * @param wb
     * @param sheet
     * @param filedNames
     */
    public void appendToExcel(List<SearchSkuTemplate> searchSkuTemplates,Workbook wb,Sheet sheet,List<String> filedNames){
        int pos = sheet.getLastRowNum()+1;
        //锁住sheet，设置折扣列单元格可编辑
        XSSFCellStyle unLockStyle = (XSSFCellStyle) wb.createCellStyle();
        unLockStyle.setLocked(false);
        for (SearchSkuTemplate searchSkuTemplate:searchSkuTemplates) {
            //转换成符合格式的实体类
            SearchSkuTemplateEntity entity = new SearchSkuTemplateEntity(searchSkuTemplate);
            Row row = sheet.createRow(pos++);
            if (null != filedNames && !filedNames.isEmpty()) {
                int rowPos = 0;
                for (String fieldName : filedNames) {
                    try {
                        Field f = entity.getClass().getDeclaredField(fieldName);
                        f.setAccessible(true);
                        Cell cell = row.createCell(rowPos++);
                        Object value = f.get(entity);
                        if(value != null)
                            cell.setCellValue(value.toString());
                        if(fieldName.equals("discount"))
                            cell.setCellStyle(unLockStyle);
                    } catch (NoSuchFieldException e) {
                        throw new RuntimeException("can not find field:" + fieldName + "in " + entity.getClass().getName() + " class", e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException("cant not access field:" + fieldName + "in " + entity.getClass().getName() + " class", e);
                    }
                }
            }
        }
    }

    /**
     * 根据class反射出字段集合
     * @param clazz
     * @param row
     * @return
     */
    private List<String> setUpTitleRow(Class clazz, Row row) {
        List<String> fieldNames = new ArrayList<>();
        log.debug("not specify title context use export entity field annotation for export title");
        //未标注@ExportTitle注解的字段忽略，不导出
        //根据@ExportOrder的值排序。标注@ExportOrder都在未标注的字段前，未标注@ExportOrder的字段按照定声明顺序导出
        Stream.of(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(ExportTitle.class))
                .sorted((f1, f2) -> {
                    if (f1.isAnnotationPresent(ExportOrder.class) && !f2.isAnnotationPresent(ExportOrder.class)) {
                        return -5;
                    } else if (!f1.isAnnotationPresent(ExportOrder.class) && f2.isAnnotationPresent(ExportOrder.class)) {
                        return 5;
                    } else if (f1.isAnnotationPresent(ExportOrder.class) && f2.isAnnotationPresent(ExportOrder.class)) {
                        return f2.getAnnotation(ExportOrder.class).value() - f1.getAnnotation(ExportOrder.class).value();
                    } else
                        return 0;
                })
                .forEach(field -> {
                    fieldNames.add(field.getName());
                    ExportTitle titleAnnotation = field.getAnnotation(ExportTitle.class);
                    row.createCell(row.getPhysicalNumberOfCells()).setCellValue(titleAnnotation.value());
                });
        return fieldNames;
    }

    @Subscribe
    public void onImportMposDiscount(BatchAsyncImportMposDiscountEvent event) throws OpenXML4JException, ParserConfigurationException, SAXException, IOException {
        log.info("async import mpos discount task start");
        MultipartFile file = event.getFile();
        Long userId = event.getCurrentUserId();
        String taskId = taskId();
        String key = toImportKey(userId,file.getOriginalFilename(),taskId);
        ExcelExportHelper<AbnormalRecord> helper = ExcelExportHelper.newExportHelper(AbnormalRecord.class);
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                jedis.setex(key,BATCH_RECORD_EXPIRE_TIME,PsItemConstants.EXECUTING);
            }
        });
        List<String[]> list = ExcelUtil.readerExcel(file.getInputStream(),"Sheet0",11);
        for (int i = 1;i<list.size();i++) {
            String[] strs = list.get(i);
            if(!Strings.isNullOrEmpty(strs[8]) && !"\"\"".equals(strs[8])){
                try{
                    Long id = Long.parseLong(strs[0].replace("\"",""));
                    Integer discount = Integer.parseInt(strs[8]);
                    setDiscount(id,discount);
                }catch (Exception e){
                    log.error("set discount fail,spucode={},discount={},cause:{}",strs[1],strs[8], Throwables.getStackTraceAsString(e));
//                    jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
//                        @Override
//                        public void action(Jedis jedis) {
//                            String key = "mpos:"+ userId +":abnormal:import:"+taskId;
//                            if(!jedis.exists(key)){
//                                jedis.expire(key,BATCH_RECORD_EXPIRE_TIME);
//                            }
//                            jedis.lpush(key,strs[1].replace("\"","") +"~"+ "Formatting error");
//                        }
//                    });





                }
            }
        }
        jedisTemplate.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                //1.如果有异常记录，置2，无异常，置1
//                if(jedis.exists("mpos:"+ userId+":abnormal:import:"+taskId)){
//                    jedis.setex(key,BATCH_RECORD_EXPIRE_TIME,PsItemConstants.EXECUTE_ERROR);
//                }else{
//                    jedis.setex(key,BATCH_RECORD_EXPIRE_TIME,PsItemConstants.EXECUTED);
//                }
            }
        });
        log.info("async import mpos discount task over");
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
            throw new JsonResponseException(rExist.getError());
        }
        SkuTemplate exist = rExist.getResult();
        Map<String,String> extra = setMopsDiscount(exist,discount);
        SkuTemplate toUpdate = new SkuTemplate();
        toUpdate.setId(exist.getId());
        toUpdate.setExtra(extra);
        Response<Boolean> resp = psSkuTemplateWriteService.update(toUpdate);
        if (!resp.isSuccess()) {
            log.error("update SkuTemplate failed error={}",resp.getError());
            throw new JsonResponseException(resp.getError());
        }
    }

    //设置折扣
    private Map<String,String> setMopsDiscount(SkuTemplate exist,Integer discount){
        Map<String,String> extra = exist.getExtra();
        if(org.springframework.util.CollectionUtils.isEmpty(extra)){
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
            if(org.springframework.util.CollectionUtils.isEmpty(extra)&&extra.containsKey(PsItemConstants.MPOS_DISCOUNT)){
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
     * @param taskId
     * @return
     */
    private String toKey(Long userId,String operatorType,String taskId){
        return "mpos:" + userId + ":flag:" + operatorType + "~" + taskId;
    }

    private String toUploadKey(Long userId,String name,String operateType){
        return "mpos:" + userId + ":" + operateType + ":" + name + "~" + DateTime.now().toDate().getTime();
    }

    private String toImportKey(Long userId,String name,String taskId){
        return "mpos:" + userId + ":import:"+ name + "~" + taskId;
    }

    /**
     * 生成任务ID
     * @return
     */
    private String taskId(){
        return String.valueOf(DateTime.now().toDate().getTime());
    }

}
