package com.pousheng.middle.web.yintai.biz;

import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.Sheet;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.MoreObjects;
import com.pousheng.middle.common.utils.batchhandle.ExcelExportHelper;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.export.UploadFileComponent;
import com.pousheng.middle.web.yintai.component.MiddleYintaiBrandService;
import com.pousheng.middle.web.yintai.dto.FailYintaiBrand;
import com.pousheng.middle.web.yintai.dto.YintaiBrand;
import com.pousheng.middle.web.yintai.mq.YintaiMessageProducer;
import io.terminus.open.client.center.MappingServiceRegistryCenter;
import io.terminus.open.client.common.mappings.model.BrandMapping;
import io.terminus.open.client.common.mappings.service.OpenClientBrandMappingService;
import io.terminus.parana.brand.model.Brand;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.math.NumberUtils;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Strings;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 银泰品牌映射导入
 * AUTHOR: zhangbin
 * ON: 2019/6/25
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.IMPORT_YINTAI_BRAND_MAPPING)
@Service
@Slf4j
public class ImportYintaiBrandMappingTask implements CompensateBizService {

    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;
    @Autowired
    private UploadFileComponent uploadFileComponent;
    @Autowired
    private MiddleYintaiBrandService middleYintaiBrandService;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private YintaiMessageProducer yintaiMessageProducer;
    @Autowired
    private MappingServiceRegistryCenter mappingCenter;
    @Value("${yintai.brand.mapping.full.push.enable:false}")
    private String fullPush;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        String bizID = poushengCompensateBiz.getBizId();
        log.info("ImportYintaiBrandMappingTask==> start to process yintai brand import task ID:{}", bizID);
        poushengCompensateBiz = processFile(poushengCompensateBiz);
        if (!StringUtils.isEmpty(poushengCompensateBiz.getLastFailedReason())) {
            PoushengCompensateBiz update = new PoushengCompensateBiz();
            update.setId(poushengCompensateBiz.getId());
            update.setUpdatedAt(DateTime.now().toDate());
            update.setLastFailedReason(poushengCompensateBiz.getLastFailedReason());
            poushengCompensateBizWriteService.update(update);
        }
        log.info("ImportYintaiBrandMappingTask==> process yintai brand import task ID:{} finished", bizID);
    }

    private PoushengCompensateBiz processFile(PoushengCompensateBiz poushengCompensateBiz) {
        String bizID = poushengCompensateBiz.getBizId();
        BufferedInputStream stream = null;
        try {

            String context = poushengCompensateBiz.getContext();
            if (StringUtils.isEmpty(context)) {
                log.error("ImportYintaiBrandMappingTask==> task ID:{} content is empty !", bizID);
                throw new BizException("任务参数为空");
            }
            YintaiImportFileInfo fileInfo = JSONObject.parseObject(context, YintaiImportFileInfo.class);
            //1.解析excel数据
            String fileUrl = fileInfo.getFilePath();
            if (StringUtils.isEmpty(fileUrl)) {
                String errorMsg = "task ID:" + bizID + ",文件地址为空";
                log.error(errorMsg);
                throw new BizException(errorMsg);
            }

            OpenClientBrandMappingService brandService = mappingCenter.getBrandService(MiddleChannel.YINTAI.getValue());
            List<Brand> brandList = middleYintaiBrandService.getMiddleBrandList();
            Map<Long, String> brandMapping = brandList.stream().collect(Collectors.toMap(Brand::getId, Brand::getName));
            List<BrandMapping> channelBrandList = brandService.findBrandListByChannel(MiddleChannel.YINTAI.getValue()).getResult();
            Map<Long, BrandMapping> channelBrandMapper = channelBrandList.stream().collect(Collectors.toMap(BrandMapping::getBrandId, Function.identity(), (a, b) -> a));

            //导入的excel数据
            URL url = new URL(fileUrl);
            stream = new BufferedInputStream(url.openStream());
            ExcelReader reader = new ExcelReader(stream, bizID, new AnalysisEventListener() {
                //导入失败需要记录的数据日志excel
                List<FailYintaiBrand> errorData = Lists.newArrayList();
                List<BrandMapping> createList = Lists.newArrayList();
                List<BrandMapping> updateList = Lists.newArrayList();

                @Override
                public void invoke(Object data, AnalysisContext analysisContext) {
                    if (analysisContext.getCurrentRowAnalysisResult() == null) {
                        return;
                    }
                    YintaiBrand yintaiBrand = (YintaiBrand) analysisContext.getCurrentRowAnalysisResult();
                    if (Strings.isNullOrEmpty(yintaiBrand.getBrandId())) {
                        FailYintaiBrand fail = convertFail(yintaiBrand);
                        fail.setFaildReason("中台品牌ID缺失");
                        errorData.add(fail);
                        log.error("brandId is null, data:({}), row:({})", data, analysisContext.getCurrentRowNum());
                        return;
                    }
                    if (!NumberUtils.isNumber(yintaiBrand.getBrandId())) {
                        FailYintaiBrand fail = convertFail(yintaiBrand);
                        fail.setFaildReason("中台品牌ID非法字符");
                        errorData.add(fail);
                        log.error("brandId is illegal, data:({}), row:({})", data, analysisContext.getCurrentRowNum());
                        return;
                    }
                    if (!Strings.isNullOrEmpty(yintaiBrand.getChannelBrandId()) && !NumberUtils.isNumber(yintaiBrand.getChannelBrandId())) {
                        FailYintaiBrand fail = convertFail(yintaiBrand);
                        fail.setFaildReason("银泰品牌ID非法字符");
                        errorData.add(fail);
                        log.error("channel brandId is illegal, data:({}), row:({})", data, analysisContext.getCurrentRowNum());
                        return;
                    }
                    Long yintaiBrandId = Long.valueOf(yintaiBrand.getBrandId());
                    if (!brandMapping.containsKey(yintaiBrandId)) {
                        FailYintaiBrand fail = convertFail(yintaiBrand);
                        fail.setFaildReason("中台品牌ID不存在");
                        errorData.add(fail);
                        log.error("brandId not exist, data:({}), row:({})", data, analysisContext.getCurrentRowNum());
                        return;
                    }
//                    if (!brandMapping.get(yintaiBrandId).equals(yintaiBrand.getBrandName())) {
//                        FailYintaiBrand fail = convertFail(yintaiBrand);
//                        fail.setFaildReason("中台品牌ID与品牌名称不一致");
//                        errorData.add(fail);
//                        log.error("brandId and name no match, data:({}), row:({})", data, analysisContext.getCurrentRowNum());
//                        return;
//                    }
                    if (channelBrandMapper.containsKey(yintaiBrandId)) {//更新映射
                        BrandMapping updated = new BrandMapping();
                        updated.setId(channelBrandMapper.get(yintaiBrandId).getId());
                        updated.setChannelBrandId(MoreObjects.firstNonNull(yintaiBrand.getChannelBrandId(),""));
                        brandService.updateBrandMapping(updated);
                        updated.setBrandId(yintaiBrandId);
                        updateList.add(updated);
                    } else if (!Strings.isNullOrEmpty(yintaiBrand.getChannelBrandId())){
                        //保存
                        BrandMapping created = new BrandMapping();
                        created.setBrandId(yintaiBrandId);
                        created.setChannelBrandId(yintaiBrand.getChannelBrandId());
                        created.setChannel(MiddleChannel.YINTAI.getValue());
                        brandService.createBrandMapping(created);
                        createList.add(created);
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                    log.info("brand excel analysed after,bizId({}), all({}), created({}), updated({}), error({})",
                            bizID, analysisContext.getCurrentRowNum(), createList.size(), updateList.size(), errorData.size());
                    //如果不可用数据不为空，写入错误日志excel
                    if (errorData.size() > 0) {
                        try {
                            poushengCompensateBiz.setLastFailedReason("导入出错");
                            ExcelExportHelper<FailYintaiBrand> errorExcel = ExcelExportHelper.newExportHelper(FailYintaiBrand.class);
                            errorExcel.appendToExcel(errorData);
                            File file = errorExcel.transformToFile();
                            String abnormalUrl = uploadFileComponent.exportAbnormalRecord(file);
                            poushengCompensateBiz.setLastFailedReason(abnormalUrl);
                        } catch (Exception e) {
                            log.error("upload fail excel fail bizId, cause:({})", bizID, e);
                            poushengCompensateBiz.setLastFailedReason(e.getMessage());
                        }
                    }
                    if (createList.size() > 0 && "true".equals(fullPush)) {
                        for (BrandMapping brandMapping : createList) {
                            if (!Strings.isNullOrEmpty(brandMapping.getChannelBrandId())) {
                                yintaiMessageProducer.sendItemPush(Lists.newArrayList(brandMapping), null);
                            }
                        }
                    }
                    if (updateList.size() > 0 && "true".equals(fullPush)) {
                        for (BrandMapping brandMapping : updateList) {
                            if (!Strings.isNullOrEmpty(brandMapping.getChannelBrandId())) {
                                yintaiMessageProducer.sendItemPush(Lists.newArrayList(brandMapping), null);
                            }
                        }
                    }
                }
            }, false);

            reader.read(new Sheet(1, 1, YintaiBrand.class));


        } catch (Exception e) {
            log.error("ImportYintaiBrandMappingTask process fail, bizId:({}), cause:({})", bizID, e);
            String message = getMessage(e.getMessage());
            poushengCompensateBiz.setLastFailedReason(message);
        }
        finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
        return poushengCompensateBiz;
    }

    /**
     * 获取国际化信息
     *
     * @param msgCode
     * @return
     */
    private String getMessage(String msgCode) {
        String message = null;
        try {
            message = messageSource.getMessage(msgCode, null, Locale.CHINA);
            if (StringUtils.isEmpty(message)) {
                return msgCode;
            }
        } catch (Exception e) {
            message = msgCode;
        }
        return message;
    }

    private FailYintaiBrand convertFail(YintaiBrand yintaiBrand) {
        FailYintaiBrand fail = new FailYintaiBrand();
        fail.setBrandId(yintaiBrand.getBrandId());
        fail.setBrandName(yintaiBrand.getBrandName());
        fail.setChannelBrandId(yintaiBrand.getChannelBrandId());
        return fail;
    }
}
