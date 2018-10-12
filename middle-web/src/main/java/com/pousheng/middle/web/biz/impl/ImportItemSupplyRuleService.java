package com.pousheng.middle.web.biz.impl;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.pousheng.erp.service.PoushengMiddleSpuService;
import com.pousheng.middle.common.utils.batchhandle.ExcelExportHelper;
import com.pousheng.middle.common.utils.batchhandle.ItemSupplyRuleAbnormalRecord;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.export.UploadFileComponent;
import com.pousheng.middle.web.item.component.ShopSkuSupplyRuleComponent;
import com.pousheng.middle.web.shop.component.OpenShopLogic;
import com.pousheng.middle.web.utils.HandlerFileUtil;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.msg.common.StringUtil;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.spu.model.SkuTemplate;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * Description: 处理批量商品规则导入
 * User: support 9
 * Date: 2018/9/17
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.IMPORT_ITEM_SUPPLY_RULE)
@Service
@Slf4j
public class ImportItemSupplyRuleService implements CompensateBizService {

    private PoushengCompensateBizWriteService poushengCompensateBizWriteService;
    private UploadFileComponent uploadFileComponent;
    private ShopSkuSupplyRuleComponent shopSkuSupplyRuleComponent;
    private OpenShopCacher openShopCacher;
    private PoushengMiddleSpuService poushengMiddleSpuService;

    @RpcConsumer
    private OpenShopReadService openShopReadService;

    @Autowired
    private WarehouseCacher warehouseCacher;

    @Autowired
    private OpenShopLogic openShopLogic;


    @Autowired
    public ImportItemSupplyRuleService(PoushengCompensateBizWriteService poushengCompensateBizWriteService,
                                       UploadFileComponent uploadFileComponent,
                                       ShopSkuSupplyRuleComponent shopSkuSupplyRuleComponent,
                                       OpenShopCacher openShopCacher,
                                       PoushengMiddleSpuService poushengMiddleSpuService) {
        this.poushengCompensateBizWriteService = poushengCompensateBizWriteService;
        this.uploadFileComponent = uploadFileComponent;
        this.shopSkuSupplyRuleComponent = shopSkuSupplyRuleComponent;
        this.openShopCacher = openShopCacher;
        this.poushengMiddleSpuService = poushengMiddleSpuService;
    }

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {

        log.info("import item supply rule start ....,poushengCompensateBiz is {}", poushengCompensateBiz);
        if (null == poushengCompensateBiz) {
            log.warn("ImportItemSupplyRule.doProcess params is null");
            return;
        }
        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("ImportItemSupplyRule.doProcess context is null");
            return;
        }
        poushengCompensateBiz = handle(poushengCompensateBiz);
        poushengCompensateBizWriteService.update(poushengCompensateBiz);
    }

    protected PoushengCompensateBiz handle(PoushengCompensateBiz poushengCompensateBiz) {
        String url = poushengCompensateBiz.getContext();
        ExcelExportHelper<ItemSupplyRuleAbnormalRecord> helper = ExcelExportHelper.newExportHelper(ItemSupplyRuleAbnormalRecord.class);
        List<String[]> list = HandlerFileUtil.getInstance().handlerExcel(url);
        for (int i = 1; i < list.size(); i++) {
            String[] str = list.get(i);
            String failReason = "";
            Boolean notNull = true;
            try {
                for (int j = 0; j < str.length; j++) {
                    if (StringUtils.isEmpty(str[j])) {
                        notNull = false;
                    }
                }
                if (!notNull) {
                    failReason = "所有栏位不可为空";
                    continue;
                }
                String type = str[2].replace("\"", "");
                if (!Objects.equals(type, "IN") && !Objects.equals(type, "NOT_IN")) {
                    failReason = "限制类型必须为IN/NOT_IN";
                    continue;
                }
                String status = str[4].replace("\"", "");
                if (!Objects.equals(status, "ENABLE") && !Objects.equals(status, "DISABLE")) {
                    failReason = "状态必须为ENABLE/DISABLE";
                    continue;
                }
                String shopCode = str[0].replace("\"", "");

                OpenClientShop openShop  =findOpenShop(shopCode);
                if (Objects.isNull(openShop)) {
                    failReason = "该店铺不存在";
                    continue;
                }
                Response<Optional<SkuTemplate>> skuTemplateResponse = poushengMiddleSpuService.findBySkuCode(str[1].replace("\"", ""));
                if (!skuTemplateResponse.isSuccess() || !skuTemplateResponse.getResult().isPresent()) {
                    log.error("fail to find sku template by skuCode:{}, cause:{}", str[1].replace("\"", ""), skuTemplateResponse.getError());
                    failReason = "该货品条码不存在";
                    continue;
                }
                SkuTemplate skuTemplate = skuTemplateResponse.getResult().get();
                if (!shopSkuSupplyRuleComponent.isSkuInShop(Lists.newArrayList(skuTemplate.getSkuCode()), openShop.getOpenShopId())) {
                    failReason = "商品不在店铺中";
                    continue;
                }
                List<String> warerehouseCodes = Splitter.on(",").trimResults().splitToList(str[3].replace("\"", ""));
                if(CollectionUtils.isEmpty(warerehouseCodes)){
                    failReason = "仓库不能为空";
                    continue;
                }

                Response<Boolean> response = shopSkuSupplyRuleComponent.save(openShop, skuTemplate, type, warerehouseCodes, status);
                if (!response.isSuccess() || !response.getResult()) {
                    log.error("fail to update shop sku supply rule,cause:{}", response.getError());
                    failReason = "保存失败";
                    continue;
                }
            } catch (NumberFormatException nfe) {
                log.error("failed to convert shop id while importing item supply rule.url:{}", url, nfe);
                failReason = "店铺id不是数字";
            } catch (Exception e) {
                log.error("failed to import item supply rule url:{}", url, e);
                failReason = "系统异常" ;
            } finally {
                if (!StringUtils.isEmpty(failReason)) {
                    ItemSupplyRuleAbnormalRecord record = new ItemSupplyRuleAbnormalRecord();
                    if (!StringUtils.isEmpty(str[0])) {
                        record.setShop(str[0].replace("\"", ""));
                    }
                    if (!StringUtils.isEmpty(str[1])) {
                        record.setSkuCode(str[1].replace("\"", ""));
                    }
                    if (!StringUtils.isEmpty(str[2])) {
                        record.setType(str[2].replace("\"", ""));
                    }
                    if (!StringUtils.isEmpty(str[3])) {
                        record.setWarehouseCodes(str[3].replace("\"", ""));
                    }
                    if (!StringUtils.isEmpty(str[4])) {
                        record.setStatus(str[4].replace("\"", ""));
                    }
                    record.setFailReason(failReason);
                    helper.appendToExcel(record);
                }
            }
        }
        if (helper.size() > 0) {
            String abnormalUrl = uploadFileComponent.exportAbnormalRecord(helper.transformToFile());
            poushengCompensateBiz.setLastFailedReason(abnormalUrl);
            poushengCompensateBiz.setUpdatedAt(DateTime.now().toDate());
            poushengCompensateBiz.setStatus(PoushengCompensateBizStatus.SUCCESS.name());
        }
        return poushengCompensateBiz;
    }

    /**
     * 查询店铺信息
     * @param shopCode 店铺appCode
     * @return
     */
    protected OpenClientShop findOpenShop(String shopCode) {
        Response<List<OpenClientShop>> openShopResponse = openShopReadService.search(null, null, shopCode);
        if (!openShopResponse.isSuccess()) {
            log.error("find open shop failed,shopCode is {},caused by {}", shopCode, openShopResponse.getError());
            throw new ServiceException("find.open.shop.failed");
        }
        List<OpenClientShop> openClientShops = openShopResponse.getResult();
        if (!CollectionUtils.isEmpty(openClientShops)) {
            return openClientShops.get(0);
        }
        //若没找到则模糊搜索
        List<String> strList = Splitter.on("-").splitToList(shopCode);
        if (CollectionUtils.isEmpty(strList)
            || strList.size() != 2) {
            return null;
        }
        String outId = strList.get(1);
        String biz = strList.get(0);
        List<OpenShop> shopList = openShopLogic.searchByOuterIdAndBusinessId(outId, biz);
        if (CollectionUtils.isEmpty(shopList)) {
            return null;
        }
        return OpenClientShop.from(shopList.get(0));
    }


}
