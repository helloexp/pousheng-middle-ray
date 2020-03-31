package com.pousheng.middle.web.yintai.biz;

import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.Sheet;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.MoreObjects;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.pousheng.middle.common.utils.batchhandle.ExcelExportHelper;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.shop.dto.MemberShop;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.export.UploadFileComponent;
import com.pousheng.middle.web.shop.component.MemberShopOperationLogic;
import com.pousheng.middle.web.yintai.YintaiConstant;
import com.pousheng.middle.web.yintai.component.MiddleYintaiShopService;
import com.pousheng.middle.web.yintai.dto.FailYintaiShop;
import com.pousheng.middle.web.yintai.dto.YintaiShop;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.open.client.center.MappingServiceRegistryCenter;
import io.terminus.open.client.common.mappings.model.ShopCounterMapping;
import io.terminus.open.client.common.mappings.service.OpenClientShopCounterMappingService;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.common.shop.service.OpenShopWriteService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * 银泰店铺映射导入
 * AUTHOR: zhangbin
 * ON: 2019/6/26
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.IMPORT_YINTAI_SHOP_MAPPING)
@Service
@Slf4j
public class ImportYintaiShopMappingTask implements CompensateBizService {
    @Autowired
    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;
    @Autowired
    private UploadFileComponent uploadFileComponent;
    @Autowired
    private OpenShopReadService openShopReadService;
    @Autowired
    private OpenShopWriteService openShopWriteService;
    @Autowired
    private MiddleYintaiShopService middleYintaiShopService;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private MemberShopOperationLogic memberShopOperationLogic;

    @Autowired
    private MappingServiceRegistryCenter mappingCenter;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        String bizID = poushengCompensateBiz.getBizId();
        log.info("ImportYintaiShopMappingTask==> start to process yintai shop import task ID:{}", bizID);
        poushengCompensateBiz = processFile(poushengCompensateBiz);
        if (!StringUtils.isEmpty(poushengCompensateBiz.getLastFailedReason())) {
            PoushengCompensateBiz update = new PoushengCompensateBiz();
            update.setId(poushengCompensateBiz.getId());
            update.setUpdatedAt(DateTime.now().toDate());
            update.setLastFailedReason(poushengCompensateBiz.getLastFailedReason());
            poushengCompensateBizWriteService.update(update);
        }
        log.info("ImportYintaiShopMappingTask==> process yintai shop import task ID:{} finished", bizID);
    }

    private PoushengCompensateBiz processFile(PoushengCompensateBiz poushengCompensateBiz) {
        String bizID = poushengCompensateBiz.getBizId();
        BufferedInputStream stream = null;
        try {
            String context = poushengCompensateBiz.getContext();
            if (StringUtils.isEmpty(context)) {
                log.error("ImportYintaiShopMappingTask==> task ID:{} content is empty !", bizID);
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
            //银泰约定只有一个外部门店，这里直接取第一个了
            Response<List<OpenShop>> openShopResp = openShopReadService.findByChannel(MiddleChannel.YINTAI.getValue());
            if (!openShopResp.isSuccess()) {
                log.error("find open shop fail by yintai channel. poushengCompensateBiz:({}), error:({})", poushengCompensateBiz, openShopResp.getError());
                throw new ServiceException(openShopResp.getError());
            }
            OpenShop openShop = openShopResp.getResult().get(0);
            OpenClientShopCounterMappingService shopCounterService = mappingCenter.getShopCounterService(MiddleChannel.YINTAI.getValue());

            //店铺下的柜台映射
            Response<List<ShopCounterMapping>> mappingResp = shopCounterService.findByOpenShopId(openShop.getId());
            Table</*公司码*/String, /*门店外码*/String, ShopCounterMapping> existMappingTable = HashBasedTable.create();
            mappingResp.getResult().forEach(mapping->
                    existMappingTable.put(
                            mapping.getExtra().get(YintaiConstant.COMPANY_CODE),
                            mapping.getExtra().get(YintaiConstant.SHOP_OUT_CODE),
                            mapping
                    )
            );

            //导入的excel数据
            URL url = new URL(fileUrl);
            stream = new BufferedInputStream(url.openStream());
            ExcelReader reader = new ExcelReader(stream, bizID, new AnalysisEventListener() {
                //导入失败需要记录的数据日志excel
                List<FailYintaiShop> errorData = Lists.newArrayList();
                List<YintaiShop> createList = Lists.newArrayList();
                List<YintaiShop> updateList = Lists.newArrayList();

                @Override
                public void invoke(Object data, AnalysisContext analysisContext) {
                    if (analysisContext.getCurrentRowAnalysisResult() == null) {
                        return;
                    }
                    YintaiShop yintaiShop = (YintaiShop) analysisContext.getCurrentRowAnalysisResult();
                    if (StringUtils.isEmpty(yintaiShop.getCompanyCode())) {
                        FailYintaiShop fail = convertFail(yintaiShop);
                        fail.setFaildReason("公司码缺失");
                        errorData.add(fail);
                        log.error("companyCode is null, data:({}), row:({})", data, analysisContext.getCurrentRowNum());
                        return;
                    }
                    if (StringUtils.isEmpty(yintaiShop.getShopOutCode())) {
                        FailYintaiShop fail = convertFail(yintaiShop);
                        fail.setFaildReason("门店外码缺失");
                        errorData.add(fail);
                        log.error("shopOutCode is null, data:({}), row:({})", data, analysisContext.getCurrentRowNum());
                        return;
                    }

                    //更新
                    if (existMappingTable.contains(yintaiShop.getCompanyCode(), yintaiShop.getShopOutCode())) {
                        ShopCounterMapping updated = existMappingTable.get(yintaiShop.getCompanyCode(), yintaiShop.getShopOutCode());
                        updated.setCounterId(MoreObjects.firstNonNull(yintaiShop.getChannelShopId(), ""));
                        Response<Boolean> response = shopCounterService.update(updated);
                        if (!response.isSuccess()) {
                            FailYintaiShop fail = convertFail(yintaiShop);
                            fail.setFaildReason(response.getError());
                            errorData.add(fail);
                            log.error("fail to update shop counter mapping, updated:({}), row:({})", updated, analysisContext.getCurrentRowNum());
                            return;
                        }
                        updateList.add(yintaiShop);
                    } else {//创建
                        List<MemberShop> memberShops = null;
                        try {
                            memberShops = memberShopOperationLogic.findShops(yintaiShop.getShopOutCode());
                        } catch (Exception e) {
                            FailYintaiShop fail = convertFail(yintaiShop);
                            fail.setFaildReason("会员接口调用失败");
                            errorData.add(fail);
                            log.error("fail to call member shop, row:({})", analysisContext.getCurrentRowNum());
                            return;
                        }
                        if (CollectionUtils.isEmpty(memberShops)) {
                            FailYintaiShop fail = convertFail(yintaiShop);
                            fail.setFaildReason("会员接口无数据");
                            errorData.add(fail);
                            log.error("fail to call member shop, row:({})", analysisContext.getCurrentRowNum());
                            return;
                        }
                        Optional<MemberShop> first = memberShops.stream().filter(memberShop -> memberShop.getCompanyId().equals(yintaiShop.getCompanyCode())).findFirst();
                        if (!first.isPresent()) {
                            FailYintaiShop fail = convertFail(yintaiShop);
                            fail.setFaildReason("店铺数据缺失");
                            errorData.add(fail);
                            log.error("fail to call member shop, row:({})", analysisContext.getCurrentRowNum());
                            return;
                        }
                        MemberShop member = first.get();
                        ShopCounterMapping created = new ShopCounterMapping();
                        created.setOpenShopId(openShop.getId());
                        created.setChannel(MiddleChannel.YINTAI.getValue());
                        created.setBizKey(yintaiShop.getCompanyCode()+"-"+yintaiShop.getShopOutCode());
                        created.setCounterId(MoreObjects.firstNonNull(yintaiShop.getChannelShopId(), ""));
                        created.setStatus(1);
                        Map<String, String> extra = Maps.newHashMap();
                        extra.put(YintaiConstant.SHOP_OUT_CODE, yintaiShop.getShopOutCode());
                        extra.put(YintaiConstant.COMPANY_CODE, yintaiShop.getCompanyCode());
                        extra.put(YintaiConstant.COMPANY_NAME, member.getStoreName());
                        created.setExtra(extra);
                        Response<Long> response = shopCounterService.create(created);
                        if (!response.isSuccess()) {
                            FailYintaiShop fail = convertFail(yintaiShop);
                            fail.setFaildReason(response.getError());
                            errorData.add(fail);
                            log.error("fail to create shop counter mapping, created:({}), row:({})", created, analysisContext.getCurrentRowNum());
                            return;
                        }

                        createList.add(yintaiShop);
                    }

                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                    log.info("shop excel analysed after,bizId({}), all({}), created({}), updated({}), error({})",
                            bizID, analysisContext.getCurrentRowNum(), createList.size(), updateList.size(), errorData.size());
                    //如果不可用数据不为空，写入错误日志excel
                    if (errorData.size() > 0) {
                        try {
                            poushengCompensateBiz.setLastFailedReason("导入出错");
                            ExcelExportHelper<FailYintaiShop> errorExcel = ExcelExportHelper.newExportHelper(FailYintaiShop.class);
                            errorExcel.appendToExcel(errorData);
                            File file = errorExcel.transformToFile();
                            String abnormalUrl = uploadFileComponent.exportAbnormalRecord(file);
                            poushengCompensateBiz.setLastFailedReason(abnormalUrl);
                        } catch (Exception e) {
                            log.error("upload fail excel fail bizId, cause:({})", bizID, e);
                            poushengCompensateBiz.setLastFailedReason(e.getMessage());
                        }
                    }
                }
            }, false);

            reader.read(new Sheet(1, 1, YintaiShop.class));


        } catch (Exception e) {
            log.error("ImportYintaiShopMappingTask process fail, bizId:({}), cause:({})", bizID, e);
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

    private FailYintaiShop convertFail(YintaiShop yintaiShop) {
        FailYintaiShop fail = new FailYintaiShop();
        fail.setCompanyCode(yintaiShop.getCompanyCode());
        fail.setShopOutCode(yintaiShop.getShopOutCode());
        fail.setChannelShopId(yintaiShop.getChannelShopId());
        return fail;
    }
}
