package com.pousheng.middle.web.order.sync.mpos;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.open.mpos.dto.MposResponse;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 同步mpos订单状态
 * 1. 待接单 待发货 待收货 已收货
 */
@Component
@Slf4j
public class SyncMposOrderLogic {

    @Autowired
    private SyncMposApi syncMposApi;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    /**
     * 同步售后单至mpos
     * @param orderId                   订单号
     * @param afterSaleId               售后单号
     * @param skuCodeAndQuantityList    商品编码及数量
     * @return
     */
    public Response<Boolean> syncAfterSaleToMpos(Long orderId,Long afterSaleId,List<SkuCodeAndQuantity> skuCodeAndQuantityList){
        Map<String,Serializable> param = this.assembAfterSaleParam(orderId,afterSaleId,skuCodeAndQuantityList);
        MposResponse response = mapper.fromJson(syncMposApi.syncAfterSaleToMpos(param),MposResponse.class);
        if(!response.isSuccess()){
            log.error("sync aftersale(id:{}) fail,cause:{}",afterSaleId,response.getError());
            return Response.fail(response.getError());
        }
        return Response.ok(true);
    }

    private Map<String,Serializable> assembAfterSaleParam(Long orderId,Long afterSaleId,List<SkuCodeAndQuantity> skuCodeAndQuantityList){
        Map<String,Serializable> param = Maps.newHashMap();
        param.put("orderId",orderId);
        param.put("afterSaleId",afterSaleId);
        List<String> skuCodes = Lists.transform(skuCodeAndQuantityList, new Function<SkuCodeAndQuantity, String>() {
            @Nullable
            @Override
            public String apply(@Nullable SkuCodeAndQuantity skuCodeAndQuantity) {
                return skuCodeAndQuantity.getSkuCode();
            }
        });
        param.put("outerSkuCodes",mapper.toJson(skuCodes));
        return param;
    }

}
