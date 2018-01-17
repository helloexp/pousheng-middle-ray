package com.pousheng.middle.web.order.sync.mpos;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.order.model.AutoCompensation;
import com.pousheng.middle.order.service.AutoCompensationWriteService;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 同步mpos订单状态
 * created by ph on 2017/01/10
 */
@Component
@Slf4j
public class SyncMposOrderLogic {


    @Autowired
    private SyncMposApi syncMposApi;

    @Autowired
    private AutoCompensationWriteService autoCompensationWriteService;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    /**
     * 同步无法派出商品至mpos
     * @param shopOrder                 订单
     * @param skuCodeAndQuantityList    商品编码及数量
     * @return
     */
    public Response<Boolean> syncNotDispatcherSkuToMpos(ShopOrder shopOrder, List<SkuCodeAndQuantity> skuCodeAndQuantityList){
        Map<String,Object> param = this.assembNotDispatcherSkuParam(shopOrder,skuCodeAndQuantityList);
        return this.syncNotDispatcherSkuToMpos(param,null);
    }

    /**
     * 同步无法派出商品至mpos
     * @return
     */
    public Response<Boolean> syncNotDispatcherSkuToMpos(Map<String,Object> param,Long id){
        try{
            Response<Boolean> response = mapper.fromJson(syncMposApi.syncNotDispatcherSkuToMpos(param),Response.class);
            if(!response.isSuccess() || Objects.equals(response.getResult(),false)){
                log.error("sync not dispatched sku to mpos fail,cause:{}",response.getError());
                if(Objects.isNull(id)){
                    this.createNewAutoCompensationTask(param);
                }
                return Response.fail("sync.not.dispatcher.sku.fail");
            }
        }catch (Exception e){
            log.error("sync not dispatched sku to mpos fail,cause:{}", Throwables.getStackTraceAsString(e));
            if(Objects.isNull(id)){
                this.createNewAutoCompensationTask(param);
            }
            return Response.fail("sync.not.dispatcher.sku.fail");
        }
        if(!Objects.isNull(id))
            this.updateAutoCompensationTask(id);
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
        param.put("skuCodeList",mapper.toJson(skuCodes));
        return param;
    }

    /**
     * 同步失败，创建自动补偿任务
     * @param param
     */
    private void createNewAutoCompensationTask(Map<String,Object> param){
            AutoCompensation autoCompensation = new AutoCompensation();
            Map<String,String> extra = Maps.newHashMap();
            extra.put("param",mapper.toJson(param));
            autoCompensation.setType(1);
            autoCompensation.setStatus(0);
            autoCompensation.setExtra(extra);
            autoCompensationWriteService.create(autoCompensation);
    }

    /**
     * 同步成功，修改任务状态
     * @param id
     */
    private void updateAutoCompensationTask(Long id){
        AutoCompensation autoCompensation = new AutoCompensation();
        autoCompensation.setId(id);
        autoCompensation.setStatus(1);
        autoCompensationWriteService.update(autoCompensation);
    }
}
