package com.pousheng.middle.open.api.skx;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.pousheng.erp.cache.ErpSpuCacher;
import com.pousheng.middle.open.api.skx.dto.OnSaleItem;
import com.pousheng.middle.open.api.skx.dto.OnSaleSku;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
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

/**
 * Created by songrenfei on 2017/7/10
 */
@OpenBean
@Slf4j
public class SkxItemOpenApi {

    @RpcConsumer
    private MappingReadService mappingReadService;
    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;
    @Autowired
    private ErpSpuCacher erpSpuCacher;

    @Value("${skx.open.shop.id}")
    private Long skxOpenShopId;

    private final static DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyyMMddHHmmss");




    @OpenMethod(key = "query.erp.onsale.item.api", paramNames = {"pageNo","pageSize","startAt","endAt"}, httpMethods = RequestMethod.GET)
    public Paging<OnSaleItem> getOnSaleItem(@NotNull(message = "page.no.is.null") Integer pageNo,@NotNull(message = "page.size.is.null")  Integer pageSize,
                                            @NotEmpty(message = "start.at.empty")  String startAt,@NotEmpty(message = "end.at.empty") String endAt) {
        log.info("QUERY-ERP-ON-SALE-ITEM-START param pageNo is:{},pageSize:{},startAt:{} ,endAt:{} skxOpenShopId:{} ", pageNo,pageSize,startAt,endAt,skxOpenShopId);

        Date startDate = DFT.parseDateTime(startAt).toDate();
        Date endDate = DFT.parseDateTime(endAt).toDate();

        Response<Paging<ItemMapping>> response =  mappingReadService.findByOpenShopId(skxOpenShopId,1,pageNo,pageSize);
        if(!response.isSuccess()){
            log.error("find push item fail");
            throw new OPServerException(200,response.getError());
        }

        log.info("QUERY-ERP-ON-SALE-ITEM-END param pageNo is:{},pageSize:{},startAt:{} ,endAt:{} result:{}", pageNo,pageSize,startAt,endAt,response.getResult().getData());
        return transToOnSaleItem(response.getResult());
    }

    private Paging<OnSaleItem> transToOnSaleItem(Paging<ItemMapping> itemMappingPaging){
        Paging<OnSaleItem> onSaleItemPaging = new Paging<>();
        onSaleItemPaging.setTotal(itemMappingPaging.getTotal());

        List<ItemMapping> mappingList = itemMappingPaging.getData();
        List<OnSaleItem> onSaleItems = Lists.newArrayListWithCapacity(mappingList.size());
        for (ItemMapping  itemMapping : mappingList){
            OnSaleItem onSaleItem = new OnSaleItem();
            Spu spu = erpSpuCacher.findById(itemMapping.getItemId());
            onSaleItem.setItemId(itemMapping.getItemId());
            onSaleItem.setItemName(spu.getName());
            onSaleItem.setSkus(makeOnSaleSku(onSaleItem.getItemId()));
            onSaleItems.add(onSaleItem);
        }

        onSaleItemPaging.setData(onSaleItems);

        return onSaleItemPaging;
    }


    private List<OnSaleSku> makeOnSaleSku(Long spuId){

        Response<List<SkuTemplate>> spuSkuTemplatesRes = skuTemplateReadService.findBySpuId(spuId);
        if(!spuSkuTemplatesRes.isSuccess()){
            log.error("find sku template by spu id:{} fail,error:{}",spuId);
            throw new OPServerException(200,spuSkuTemplatesRes.getError());
        }

        List<SkuTemplate> skuTemplates = spuSkuTemplatesRes.getResult();

        List<OnSaleSku> onSaleSkus = Lists.newArrayListWithCapacity(skuTemplates.size());

        for (SkuTemplate skuTemplate : skuTemplates){
            OnSaleSku onSaleSku = new OnSaleSku();
            Map<String, String> extra = skuTemplate.getExtra();
            if (extra.containsKey("materialId")) {
                onSaleSku.setMaterialId(extra.get("materialId"));
            }

            //销售属性
            List<SkuAttribute> skuAttributes = skuTemplate.getAttrs();
            if (!CollectionUtils.isEmpty(skuAttributes)) {
                for (SkuAttribute skuAttribute : skuAttributes) {
                    if( Objects.equal(skuAttribute.getAttrKey(),"尺码")) {
                        onSaleSku.setSize(skuAttribute.getAttrVal());
                    }
                }
            }

            onSaleSku.setSkuId(skuTemplate.getId());
            onSaleSku.setBarCode(skuTemplate.getSkuCode());
            onSaleSkus.add(onSaleSku);
        }

        return onSaleSkus;
    }


}
