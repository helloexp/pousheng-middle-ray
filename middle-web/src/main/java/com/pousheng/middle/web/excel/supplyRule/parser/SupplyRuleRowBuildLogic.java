package com.pousheng.middle.web.excel.supplyRule.parser;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.pousheng.erp.model.SpuMaterial;
import com.pousheng.erp.service.PoushengMiddleSpuService;
import com.pousheng.erp.service.SpuMaterialReadService;
import com.pousheng.middle.web.excel.supplyRule.ImportProgressStatus;
import com.pousheng.middle.web.excel.supplyRule.dto.SupplyRuleDTO;
import com.pousheng.middle.web.shop.component.OpenShopLogic;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-10 15:47<br/>
 */
@Slf4j
@Component
public class SupplyRuleRowBuildLogic {
    public static final int COLUMN_COUNT = 5;

    private SkuTemplateReadService skuTemplateReadService;
    private SpuMaterialReadService spuMaterialReadService;
    private PoushengMiddleSpuService poushengMiddleSpuService;
    private LoadingCache<String, Optional<OpenClientShop>> shopCacher;

    public SupplyRuleRowBuildLogic(OpenShopLogic openShopLogic,
                                   OpenShopReadService openShopReadService,
                                   SkuTemplateReadService skuTemplateReadService,
                                   SpuMaterialReadService spuMaterialReadService,
                                   PoushengMiddleSpuService poushengMiddleSpuService) {
        this.spuMaterialReadService = spuMaterialReadService;
        this.skuTemplateReadService = skuTemplateReadService;
        this.poushengMiddleSpuService = poushengMiddleSpuService;

        shopCacher = CacheBuilder.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(100)
                .build(new CacheLoader<String, Optional<OpenClientShop>>() {
                    @Override
                    public Optional<OpenClientShop> load(String shopCode) {
                        Response<List<OpenClientShop>> openShopResponse = openShopReadService.search(null, null, shopCode);
                        if (!openShopResponse.isSuccess()) {
                            log.error("find open shop failed,shopCode is {},caused by {}", shopCode, openShopResponse.getError());
                            throw new ServiceException("find.open.shop.failed");
                        }
                        List<OpenClientShop> openClientShops = openShopResponse.getResult();
                        if (!CollectionUtils.isEmpty(openClientShops)) {
                            return Optional.ofNullable(openClientShops.get(0));
                        }
                        //若没找到则模糊搜索
                        List<String> strList = Splitter.on("-").splitToList(shopCode);
                        if (CollectionUtils.isEmpty(strList)
                                || strList.size() != 2) {
                            return Optional.empty();
                        }
                        String outId = strList.get(1);
                        String biz = strList.get(0);
                        List<OpenShop> shopList = openShopLogic.searchByOuterIdAndBusinessId(outId, biz);
                        if (CollectionUtils.isEmpty(shopList)) {
                            return Optional.empty();
                        }
                        return Optional.ofNullable(OpenClientShop.from(shopList.get(0)));
                    }
                });
    }

    public SupplyRuleDTO build(int rowCount, List<String> values, ImportProgressStatus processStatus) {
        String failReason = "";
        try {
            boolean blank = true;
            for (String value : values) {
                blank = StringUtils.isEmpty(value) && blank;
            }

            // ommit blank line
            if (blank) {
                return null;
            }

            // 限制类型
            String type = values.get(3).replace("\"", "");
            if (StringUtils.isEmpty(type)) {
                failReason = "限制类型不能为空";
                return null;
            }
            if (!Objects.equals(type, "IN") && !Objects.equals(type, "NOT_IN")) {
                failReason = "限制类型必须为IN/NOT_IN";
                return null;
            }

            // 状态
            String status = values.get(5).replace("\"", "");
            if (StringUtils.isEmpty(status)) {
                failReason = "状态不能为空";
                return null;
            }
            if (!Objects.equals(status, "ENABLE") && !Objects.equals(status, "DISABLE")) {
                failReason = "状态必须为ENABLE/DISABLE";
                return null;
            }

            // 店铺
            String shopCode = values.get(0).replace("\"", "");
            if (StringUtils.isEmpty(shopCode)) {
                failReason = "店铺编码不能为空";
                return null;
            }
            Optional<OpenClientShop> openShop = shopCacher.getUnchecked(shopCode);
            if (!openShop.isPresent()) {
                failReason = "该店铺不存在";
                return null;
            }

            List<SkuTemplate> skus;
            String spuCode = values.get(1).replace("\"", "");
            String barCode = values.get(2).replace("\"", "");
            if (StringUtils.hasText(barCode)) {
                // 条码查商品
                Response<com.google.common.base.Optional<SkuTemplate>> skuTemplateResponse = poushengMiddleSpuService.findBySkuCode(barCode);
                if (!skuTemplateResponse.isSuccess() || !skuTemplateResponse.getResult().isPresent()) {
                    log.error("fail to find sku template by skuCode:{}, cause:{}", values.get(1).replace("\"", ""), skuTemplateResponse.getError());
                    failReason = "该货品条码不存在";
                    return null;
                }
                SkuTemplate skuTemplate = skuTemplateResponse.getResult().get();
                skus = Collections.singletonList(skuTemplate);
            } else if (StringUtils.hasText(spuCode)) {
                // 货号查询
                Response<com.google.common.base.Optional<SpuMaterial>> r = spuMaterialReadService.findbyMaterialCode(spuCode);
                if (!r.isSuccess() || !r.getResult().isPresent()) {
                    log.error("failed to find spu by code:{}, cause: {}", spuCode, r.getError());
                    failReason = "该货品货号不存在";
                    return null;
                }

                SpuMaterial spu = r.getResult().get();
                skus = Lists.newArrayList();
                Response<List<SkuTemplate>> resp = skuTemplateReadService.findBySpuId(spu.getSpuId());
                if (!resp.isSuccess()) {
                    log.error("failed to find sku by spu id: {}, cause: {}", spu.getId(), resp.getError());
                    failReason = "该货号没有找到对应商品";
                    return null;
                }
                skus.addAll(resp.getResult());

            } else {
                failReason = "条码货号不能同时为空";
                return null;
            }

            // 仓库
            String warehouse = values.get(4).replace("\"", "");
            if (StringUtils.isEmpty(warehouse)) {
                failReason = "仓库不能为空";
                return null;
            }
            List<String> warehouseCodes = Splitter.on(",").trimResults().splitToList(warehouse);

            SupplyRuleDTO supplyRuleDTO = new SupplyRuleDTO();
            supplyRuleDTO.setType(type);
            supplyRuleDTO.setStatus(status);
            supplyRuleDTO.setShop(openShop.get());
            supplyRuleDTO.setWarehouseCodes(warehouseCodes);
            supplyRuleDTO.setSkuTemplate(skus);
            supplyRuleDTO.setSource(Lists.newArrayList(values));
            return supplyRuleDTO;
        } catch (NumberFormatException e) {
            log.error("failed to convert shop id while importing item supply rule {}, cause: {}", values, Throwables.getStackTraceAsString(e));
            failReason = "店铺id不是数字";
        } catch (Exception e) {
            log.error("failed to import item supply rule {}, cause: {}", values, Throwables.getStackTraceAsString(e));
            failReason = "系统异常";
        } finally {
            if (!StringUtils.isEmpty(failReason)) {
                log.error("[EXCEL-BUILD-LOGIC] failed to parse row {}, values: {}, cause: {}", rowCount, values, failReason);
                processStatus.fail(Lists.newArrayList(values), failReason, rowCount);
            }
        }

        // return null when fail
        return null;
    }
}
