package com.pousheng.middle.warehouses;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import com.pousheng.middle.warehouse.service.WarehouseSkuReadService;
import com.pousheng.middle.warehouse.service.WarehouseSkuWriteService;
import com.pousheng.middle.warehouses.dto.SkuStock;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * 库存管理部分
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-16
 */
@RestController
@RequestMapping("/api/stock")
@Slf4j
public class WarehouseStocks {

    @RpcConsumer
    private WarehouseSkuReadService warehouseSkuReadService;

    @RpcConsumer
    private WarehouseSkuWriteService skuWriteService;

    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;


    /**
     * sku库存概览, 不分仓
     *
     * @param pageNo 起始页码
     * @param pageSize  每页返回条数
     * @param skuCode sku码查询
     * @return 查询结果
     */
    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<SkuStock> findBy(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                   @RequestParam(required = false, value = "pageSize") Integer pageSize,
                                   @RequestParam(required = false, value = "skuCode") String skuCode){

        Map<String, Object> params = Maps.newHashMap();
        if (StringUtils.hasText(skuCode)) {
            params.put("skuCode", skuCode);
        }
        Response<Paging<WarehouseSkuStock>> r = warehouseSkuReadService.findBy(pageNo, pageSize,params);
        if(!r.isSuccess()){
            log.error("failed to find warehouse sku stock by params:{}, error code:{}", params, r.getError());
            throw new JsonResponseException(r.getError());
        }
        Paging<WarehouseSkuStock> p = r.getResult();
        List<WarehouseSkuStock> warehouseSkuStocks = p.getData();
        if(!CollectionUtils.isEmpty(warehouseSkuStocks)){
            List<String> skuCodes = Lists.newArrayListWithCapacity(warehouseSkuStocks.size());
            for (WarehouseSkuStock warehouseSkuStock : warehouseSkuStocks) {
                skuCodes.add(warehouseSkuStock.getSkuCode());
            }
            Response<List<SkuTemplate>> rST =  skuTemplateReadService.findBySkuCodes(skuCodes);
            if(!rST.isSuccess()){
                log.error("failed to find skuTemplates by skuCodes:{}, error code:{}", skuCodes, rST.getError());
                throw new JsonResponseException(rST.getError());
            }
            Map<String, SkuTemplate> bySkuCode = Maps.uniqueIndex(rST.getResult(), new Function<SkuTemplate, String>() {
                @Nullable
                @Override
                public String apply(@Nullable SkuTemplate input) {
                    return input.getSkuCode();
                }
            });
            List<SkuStock> result = Lists.newArrayListWithCapacity(warehouseSkuStocks.size());
            for (WarehouseSkuStock warehouseSkuStock : warehouseSkuStocks) {
                SkuStock skuStock = new SkuStock();
                String currentSkuCode = warehouseSkuStock.getSkuCode();
                SkuTemplate skuTemplate = bySkuCode.get(currentSkuCode);
                if(skuTemplate == null){
                    log.error("no skuTemplate found for skuCode:{}, ignore", currentSkuCode);
                    continue;
                }
                skuStock.setName(skuTemplate.getName());
                skuStock.setSkuCode(currentSkuCode);
                skuStock.setSkuId(skuTemplate.getId());
                skuStock.setStock(warehouseSkuStock.getAvailStock());
                skuStock.setSkuAttrs(skuTemplate.getAttrs());
                result.add(skuStock);
            }
            return new Paging<>(p.getTotal(),result);
        }

        return Paging.empty();
    }


}
