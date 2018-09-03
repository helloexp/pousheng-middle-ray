package com.pousheng.middle.web.biz.impl;

import com.google.common.base.Optional;
import com.pousheng.middle.common.utils.batchhandle.ExcelExportHelper;
import com.pousheng.middle.common.utils.batchhandle.ItemPushRatioAbnormalRecord;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.export.UploadFileComponent;
import com.pousheng.middle.web.utils.HandlerFileUtil;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.msg.common.StringUtil;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import io.terminus.open.client.common.mappings.service.MappingWriteService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Description: 异步处理 批量导入商品推送比例
 * User: support 9
 * Date: 2018/9/3
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.IMPORT_ITEM_PUSH_RATIO)
@Service
@Slf4j
public class ImportItemPushRatioService implements CompensateBizService {

    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;
    private MappingReadService mappingReadService;
    private MappingWriteService mappingWriteService;
    private UploadFileComponent uploadFileComponent;

    @Autowired
    public ImportItemPushRatioService(PoushengCompensateBizWriteService poushengCompensateBizWriteService,
                                      MappingReadService mappingReadService,
                                      MappingWriteService mappingWriteService,
                                      UploadFileComponent uploadFileComponent) {
        this.poushengCompensateBizWriteService = poushengCompensateBizWriteService;
        this.mappingReadService = mappingReadService;
        this.mappingWriteService = mappingWriteService;
        this.uploadFileComponent = uploadFileComponent;

    }

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {

        log.info("import item push ratio start ....,poushengCompensateBiz is {}", poushengCompensateBiz);
        if (null == poushengCompensateBiz) {
            log.warn("ImportSkuStockRule.doProcess params is null");
            return;
        }
        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("ImportItemPushRatio.doProcess context is null");
            return;
        }
        poushengCompensateBiz = handle(poushengCompensateBiz);
        poushengCompensateBizWriteService.update(poushengCompensateBiz);
    }

    private PoushengCompensateBiz handle(PoushengCompensateBiz poushengCompensateBiz) {
        String url = poushengCompensateBiz.getContext();
        ExcelExportHelper<ItemPushRatioAbnormalRecord> helper = ExcelExportHelper.newExportHelper(ItemPushRatioAbnormalRecord.class);
        List<String[]> list = HandlerFileUtil.getInstance().handlerExcel(url);
        for (int i = 1; i < list.size(); i++) {
            String[] str = list.get(i);
            String failReason = "";
            try {
                if (StringUtils.isEmpty(str[0]) || StringUtils.isEmpty(str[1])) {
                    failReason = "外部电商平台id/电商商品编号不可为空";
                    continue;
                }
                Integer ratio = null;
                if (Objects.nonNull(str[2])) {
                    ratio = Integer.valueOf(str[2].replace("\"", ""));
                    if (ratio <= 0 || ratio > 100) {
                        failReason = "商品推送比例必须为大于0，小于等于100的整数";
                        continue;
                    }
                }
                Response<Optional<ItemMapping>> itemMappingRes = mappingReadService.findByChannelSkuIdAndOpenShopId(str[1].replace("\"", ""), Long.valueOf(str[0].replace("\"", "")));
                if (!itemMappingRes.isSuccess() || !itemMappingRes.getResult().isPresent()) {
                    log.error("fail to find item-mapping by channelSkuId:{}, openShopId:{}, cause:{}", str[1], str[0], itemMappingRes.getError());
                    failReason = "该商品映射不存在";
                    continue;
                }
                ItemMapping itemMapping = itemMappingRes.getResult().get();
                Response<Boolean> response = mappingWriteService.updatePushRatio(itemMapping.getId(), ratio);
                if (!response.isSuccess() || !response.getResult()) {
                    log.error("fail to update item-mapping(id:{}) ratio:{}", itemMapping.getId(), str[2]);
                    failReason = "设置商品推送比例失败";
                    continue;
                }
            } catch (NumberFormatException nfe) {
                failReason = "外部电商平台id/商品推送比例为空或不是整数";
            } catch (Exception e) {
                failReason = "系统异常";
            } finally {
                if (!StringUtils.isEmpty(failReason)) {
                    ItemPushRatioAbnormalRecord record = new ItemPushRatioAbnormalRecord();
                    if (!StringUtils.isEmpty(str[0])) {
                        record.setOpenShopId(str[0].replace("\"", ""));
                    }
                    if (!StringUtils.isEmpty(str[1])) {
                        record.setChannelSkuId(str[1].replace("\"", ""));
                    }
                    if (!StringUtils.isEmpty(str[2])) {
                        record.setRatio(str[2].replace("\"", ""));
                    }
                    record.setFailReason(failReason);
                    helper.appendToExcel(record);
                }
            }
        }
        if (helper.size() > 0) {
            String abnormalUrl = uploadFileComponent.exportAbnormalRecord(helper.transformToFile());
            poushengCompensateBiz.setLastFailedReason(abnormalUrl);
            poushengCompensateBiz.setStatus(PoushengCompensateBizStatus.SUCCESS.name());
            poushengCompensateBiz.setUpdatedAt(DateTime.now().toDate());
        }
        return poushengCompensateBiz;
    }

}
