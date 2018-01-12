package com.pousheng.middle.web.order.sync.mpos;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.open.mpos.dto.MposResponse;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.parana.component.ParanaClient;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 同步mpos订单状态
 * created by ph on 2017/01/10
 */
@Component
@Slf4j
public class SyncMposOrderLogic {


    @Autowired
    private SyncMposApi syncMposApi;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    /**
     * 同步无法派出商品至mpos
     * @param shopOrder                 订单
     * @param skuCodeAndQuantityList    商品编码及数量
     * @return
     */
    public Response<Boolean> syncNotDispatcherSkuToMpos(ShopOrder shopOrder, List<SkuCodeAndQuantity> skuCodeAndQuantityList){
        Map<String,Object> param = this.assembNotDispatcherSkuParam(shopOrder,skuCodeAndQuantityList);
        MposResponse response = mapper.fromJson(syncMposApi.syncNotDispatcherSkuToMpos(param),MposResponse.class);
        if(!response.isSuccess()){
            log.error("sync not dispatched sku to mpos fail,cause:{}",response.getError());
            return Response.fail(response.getError());
        }
        return Response.ok(true);
    }



    /**
     * 组装参数
     * @param shopOrder                 订单
     * @param skuCodeAndQuantityList    商品代码和数量
     * @return
     */
    private Map<String,Object> assembNotDispatcherSkuParam(ShopOrder shopOrder,List<SkuCodeAndQuantity> skuCodeAndQuantityList){
        Map<String,Object> param = Maps.newHashMap();
        param.put("orderId",shopOrder.getOutId());
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
