package com.pousheng.middle.web.warehouses;

import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.warehouse.model.MiddleOpenShop;
import com.pousheng.middle.warehouse.service.MiddleRefundWarehouseReadService;
import com.pousheng.middle.warehouse.service.MiddleRefundWarehouseWriteService;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/10/24
 * pousheng-middle
 */
@RestController
@RequestMapping("/api/refund/default")
@Slf4j
public class RefundDefaultWarehouse {
    @RpcConsumer
    private OpenShopReadService openShopReadService;
    @Autowired
    private MiddleRefundWarehouseReadService middleRefundWarehouseReadService;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @RpcConsumer
    private MiddleRefundWarehouseWriteService middleRefundWarehouseWriteServie;

    @RequestMapping(value = "{id}/warehouse",method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Boolean> editDefaultRefundWarehouse(@PathVariable("id") Long openShopId,@RequestParam Long warehouseId,String warehouseName){
        Response<OpenShop> openShopResponse = openShopReadService.findById(openShopId);
        if (!openShopResponse.isSuccess()){
            log.error("find open shop failed,open shop id is({}),caused by {}",openShopId,openShopResponse.getError());
            throw new JsonResponseException("find.openShop.failed");
        }
        OpenShop openShop=openShopResponse.getResult();
        Map<String, String> extraMap= openShop.getExtra();
        extraMap.put(TradeConstants.DEFAULT_REFUND_WAREHOUSE_ID,String.valueOf(warehouseId));
        extraMap.put(TradeConstants.DEFAULT_REFUND_WAREHOUSE_NAME,warehouseName);
        openShop.setExtra(extraMap);
        Response<Boolean> updateRlt = middleRefundWarehouseWriteServie.update(openShop);
        if (!updateRlt.isSuccess()){
            log.error("update openShop failed,openShopId is {},caused by {}",openShop.getId(),updateRlt.getError());
        }
        return Response.ok();
    }

    /**
     * 分页查询
     * @param pageNo 每页记录数
     * @param pageSize 页码
     * @return
     */
    @RequestMapping(value = "/paging",method =RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Paging<MiddleOpenShop>> paging(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                           @RequestParam(required = false, value = "pageSize") Integer pageSize){
        Response<Paging<OpenShop>> pageRes= middleRefundWarehouseReadService.pagination(pageNo,pageSize,null);
        if (!pageRes.isSuccess()){
            return Response.fail("find.openShop.failed");
        }
        List<OpenShop> openShops = pageRes.getResult().getData();
        List<MiddleOpenShop> middleOpenShops = Lists.newArrayList();
        openShops.forEach(openShop -> {
            MiddleOpenShop middleOpenShop=new MiddleOpenShop();
            BeanUtils.copyProperties(openShop,middleOpenShop);
            middleOpenShops.add(middleOpenShop);
        });
        Paging<MiddleOpenShop> paging =new Paging<>();
        paging.setTotal(pageRes.getResult().getTotal());
        paging.setData(middleOpenShops);
        return Response.ok(paging);
    }


    @RequestMapping(value = "{id}/openshop",method =RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<MiddleOpenShop>  queryOpenShop(@PathVariable("id") Long shopOrderId){
        ShopOrder shopOrder = orderReadLogic.findShopOrderById(shopOrderId);
        Long shopId = shopOrder.getShopId();
        OpenShop openShop = orderReadLogic.findOpenShopByShopId(shopId);
        MiddleOpenShop middleOpenShop =new MiddleOpenShop();
        BeanUtils.copyProperties(openShop,middleOpenShop);
        return Response.ok(middleOpenShop);
    }


}
