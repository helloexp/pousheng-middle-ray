package com.pousheng.middle.open.api.skx;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.erp.cache.ErpSpuCacher;
import com.pousheng.middle.open.api.skx.dto.OnSaleItem;
import com.pousheng.middle.open.api.skx.dto.OnSaleSku;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.dto.ItemMappingCriteria;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

/**
 * Sku开放平台接口
 * Created by songrenfei on 2017/7/10
 */
@OpenBean
@Slf4j
public class SkxItemOpenApi {

    @RpcConsumer
    @Setter
    private MappingReadService mappingReadService;
    @RpcConsumer
    @Setter
    private SkuTemplateReadService skuTemplateReadService;

    private final ErpSpuCacher erpSpuCacher;
    @Value("${skx.open.shop.id}")
    private Long skxOpenShopId;

    private static final DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyyMMddHHmmss");

    @Autowired
    public SkxItemOpenApi(ErpSpuCacher erpSpuCacher) {
        this.erpSpuCacher = erpSpuCacher;
    }

    /**
     * 提供给Skx查询在售的商品库存清单
     *
     * @param pageNo   页码
     * @param pageSize 页大小
     * @param startAt  开始时间
     * @param endAt    截止时间
     * @return 库存分页信息
     */
    @OpenMethod(key = "query.erp.onsale.item.api",
            paramNames = {"pageNo", "pageSize", "startAt", "endAt"},
            httpMethods = RequestMethod.GET)
    public Paging<OnSaleItem> getOnSaleItem(@NotNull(message = "page.no.is.null") Integer pageNo,
                                            @NotNull(message = "page.size.is.null") Integer pageSize,
                                            @NotEmpty(message = "start.at.empty") String startAt,
                                            @NotEmpty(message = "end.at.empty") String endAt) {
        try {
            log.info("QUERY-ERP-ON-SALE-ITEM-START param pageNo is:{},pageSize:{},startAt:{} ,endAt:{} skxOpenShopId:{} ",
                    pageNo, pageSize, startAt, endAt, skxOpenShopId);

            Date startDate = null;
            Date endDate = null;
            if (nonNull(startAt)) {
                try {
                    startDate = new DateTime(Long.parseLong(startAt)).toDate();
                } catch (Exception e) {
                    log.warn("Fail to convert startAt with {}", startAt);
                    throw new OPServerException(200, "SkxItemOpenApi.failToConvertStartDate");
                }
            }
            if (nonNull(endAt)) {
                try {

                    endDate = new DateTime(Long.parseLong(endAt)).toDate();
                } catch (Exception e) {
                    log.warn("Fail to convert endAt with {}", startAt);
                    throw new OPServerException(200, "SkxItemOpenApi.failToConvertEndDate");
                }
            }


            ItemMappingCriteria criteria = new ItemMappingCriteria();
            criteria.setStartAt(startDate);
            criteria.setEndAt(endDate);
            criteria.setOpenShopId(skxOpenShopId);
            criteria.setStatus(1);
            criteria.setPageNo(pageNo);
            criteria.setPageSize(pageSize);

            Response<Paging<ItemMapping>> response = mappingReadService
                    .paging(criteria);

            if (!response.isSuccess()) {
                log.error("find push item fail");
                throw new OPServerException(200, "SkxItemOpenApi.failToQueryMapping");
            }

            log.info("QUERY-ERP-ON-SALE-ITEM-END param pageNo is:{},pageSize:{}, startAt:{}, endAt:{} result:{}",
                    pageNo, pageSize, startDate, endDate, response.getResult().getData());
            return transToOnSaleItem(response.getResult());
        } catch (OPServerException e) {
            throw e;
        } catch (Exception e) {
            log.error("Fail to getOnSaleItem with pageNo is:{}, pageSize:{}, startAt:{}, endAt:{}, cause {}",
                    pageNo, pageSize, startAt, endAt, Throwables.getStackTraceAsString(e));
            throw new OPServerException(200, "SkxItemOpenApi.failToQueryOnSaleItem");
        }
    }

    private Paging<OnSaleItem> transToOnSaleItem(Paging<ItemMapping> itemMappingPaging) {
        Paging<OnSaleItem> onSaleItemPaging = new Paging<>();
        onSaleItemPaging.setTotal(itemMappingPaging.getTotal());

        List<ItemMapping> mappingList = itemMappingPaging.getData();
        List<OnSaleItem> onSaleItems = Lists.newArrayListWithCapacity(mappingList.size());
        for (ItemMapping itemMapping : mappingList) {
            OnSaleItem onSaleItem = new OnSaleItem();
            Spu spu = erpSpuCacher.findById(itemMapping.getItemId());
            onSaleItem.setItemId(itemMapping.getItemId());
            onSaleItem.setItemName(spu.getName());
            onSaleItems.add(onSaleItem);
            // 包装 sku (注意此处虽然是列表，但仅会返回单个sku, 由于历史原因不调整结构)
            onSaleItem.setSkus(getSkuWithCode(itemMapping.getSkuCode()));

        }

        onSaleItemPaging.setData(onSaleItems);

        return onSaleItemPaging;
    }

    private List<OnSaleSku> getSkuWithCode(String skuCode) {
        // 这里只查出正常状态和指定skuCode的记录
        Map<String, Object> params = Maps.newHashMap();
        params.put("status", 1);
        params.put("skuCode", skuCode);
        Response<Paging<SkuTemplate>> skuTempRes = skuTemplateReadService.findBy(0, 1, params);

        if (!skuTempRes.isSuccess() || isNull(skuTempRes.getResult()) || skuTempRes.getResult().isEmpty()) {
            log.error("Fail to find unique sku with {}", params);
            throw new OPServerException(200, "SkxItemOpenApi.notFoundSku");
        }

        List<OnSaleSku> onSaleSkus = Lists.newArrayListWithCapacity(1);
        Optional<SkuTemplate> skuOp = skuTempRes.getResult().getData().stream().findFirst();

        if (!skuOp.isPresent()) {
            log.error("Fail to find unique sku with empty");
            throw new OPServerException(200, "SkxItemOpenApi.notFoundSku");
        }

        SkuTemplate skuTemplate = skuOp.get();
        OnSaleSku onSaleSku = new OnSaleSku();
        Map<String, String> extra = skuTemplate.getExtra();

        if (isNull(extra) || !extra.containsKey("materialId")) {
            log.error("Fail to find unique sku, sku extra is incorrect {}", extra);
            throw new OPServerException(200, "SkxItemOpenApi.withoutMaterialIdOrEmpty");
        }


        if (extra.containsKey("materialId")) {
            onSaleSku.setMaterialId(extra.get("materialId"));
        }
        //销售属性
        List<SkuAttribute> skuAttributes = skuTemplate.getAttrs();
        if (CollectionUtils.isEmpty(skuAttributes)) {
            log.error("Fail to find unique sku, sku attrs is incorrect {}", skuAttributes);
            throw new OPServerException(200, "SkxItemOpenApi.withIncorrectAttrs");
        }

        for (SkuAttribute skuAttribute : skuAttributes) {
            if (Objects.equal(skuAttribute.getAttrKey(), "尺码")) {
                onSaleSku.setSize(skuAttribute.getAttrVal());
                break;
            }
        }

        if (isNull(onSaleSku.getSize())) {
            log.error("Fail to find unique sku, sku attrs has no size attr {}", skuAttributes);
            throw new OPServerException(200, "SkxItemOpenApi.withIncorrectSize");
        }


        onSaleSku.setSkuId(skuTemplate.getId());
        onSaleSku.setBarCode(skuTemplate.getSkuCode());
        onSaleSkus.add(onSaleSku);
        return onSaleSkus;
    }

}
