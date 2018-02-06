package com.pousheng.middle.open.api.skx;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.pousheng.middle.open.api.skx.dto.OnSaleItem;
import com.pousheng.middle.open.api.skx.dto.OnSaleSku;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.open.client.item.model.PushedItem;
import io.terminus.open.client.item.service.PushedItemReadService;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.exceptions.OPServerException;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import io.terminus.parana.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
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
    private PushedItemReadService pushedItemReadService;
    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;


    private final static DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyyMMddHHmmss");



    @OpenMethod(key = "query.erp.on.sale.item.api", paramNames = {"pageNo","pageSize","startAt","endAt"}, httpMethods = RequestMethod.GET)
    public Paging<OnSaleItem> getOnSaleItem(Integer pageNo, Integer pageSize, String startAt, String endAt) {
        log.info("QUERY-ERP-ON-SALE-ITEM-START param pageNo is:{},pageSize:{},startAt:{} ,endAt:{} ", pageNo,pageSize,startAt,endAt);

        Date startDate = DFT.parseDateTime(startAt).toDate();
        Date endDate = DFT.parseDateTime(endAt).toDate();

        Response<Paging<PushedItem>> response =  pushedItemReadService.findPushedItem(null,null,7L,1,null,pageNo,pageSize);
        if(!response.isSuccess()){
            log.error("find push item fail");
            throw new OPServerException(200,response.getError());
        }

        log.info("QUERY-ERP-ON-SALE-ITEM-END param pageNo is:{},pageSize:{},startAt:{} ,endAt:{} ", pageNo,pageSize,startAt,endAt);
        return transToOnSaleItem(response.getResult());
    }

    private Paging<OnSaleItem> transToOnSaleItem(Paging<PushedItem> pushedItemPaging){
        Paging<OnSaleItem> onSaleItemPaging = new Paging<>();
        onSaleItemPaging.setTotal(pushedItemPaging.getTotal());

        List<PushedItem> pushedItemList = pushedItemPaging.getData();
        List<OnSaleItem> onSaleItems = Lists.newArrayListWithCapacity(pushedItemList.size());
        for (PushedItem pushedItem : pushedItemList){
            OnSaleItem onSaleItem = new OnSaleItem();
            onSaleItem.setItemId(pushedItem.getItemId());
            onSaleItem.setItemName(pushedItem.getItemName());
            onSaleItem.setSkus(makeOnSaleSku(pushedItem.getItemId()));
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
                    if(Objects.equal(skuAttribute.getAttrKey(),"尺码")) {
                        onSaleSku.setSize(skuAttribute.getAttrVal());
                    }
                }
            }

            onSaleSku.setSkuId(skuTemplate.getId());
            onSaleSku.setSkuCode(skuTemplate.getSkuCode());
            onSaleSkus.add(onSaleSku);
        }

        return onSaleSkus;
    }


}
