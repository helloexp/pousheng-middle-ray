package com.pousheng.middle.web.order;

import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.jd.open.api.sdk.domain.supplier.AdvertiseJosService.JosAdvertiseApplyDto;
import com.pousheng.middle.order.dto.ShipmentPreview;
import com.pousheng.middle.order.dto.WaitShipItemInfo;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.model.WarehouseCompanyRule;
import com.pousheng.middle.warehouse.service.WarehouseCompanyRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.Splitters;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * 销售发货  和 换货发货 合并api
 * Created by songrenfei on 2017/7/6
 */
@RestController
@Slf4j
public class CreateShipments {

    @Autowired
    private AdminOrderReader adminOrderReader;
    @Autowired
    private Refunds refunds;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private WarehouseReadService warehouseReadService;
    @Autowired
    private WarehouseCompanyRuleReadService warehouseCompanyRuleReadService;


    /**
     * 待处理商品列表
     * @param id 单据id
     * @param type 1 销售发货  2 换货发货
     * @return 商品信息
     */
    @RequestMapping(value = "/api/wait/handle/sku", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<WaitShipItemInfo> waitHandleSku(@RequestParam Long id,@RequestParam(defaultValue = "1") Integer type) {

        if(Objects.equals(1,type)){
            return adminOrderReader.orderWaitHandleSku(id);
        }

        return refunds.refundWaitHandleSku(id);

    }


    /**
     * 发货预览
     *
     * @param id 单据id
     * @param data json格式
     * @param warehouseId          仓库id
     * @param type    1 销售发货  2 换货发货
     * @return 订单信息
     */
    @RequestMapping(value = "/api/ship/preview", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<ShipmentPreview> shipPreview(@RequestParam  Long id,
                                                       @RequestParam("data") String data,
                                                       @RequestParam(value = "warehouseId") Long warehouseId,
                                                       @RequestParam(defaultValue = "1") Integer type){
        Response<ShipmentPreview> response;
        if(Objects.equals(1,type)){
            response =  shipmentReadLogic.orderShipPreview(id,data);
        }else if(Objects.equals(2,type)) {
            response =  shipmentReadLogic.changeShipPreview(id,data);
        }else {
            throw new JsonResponseException("invalid.type");
        }

        if(!response.isSuccess()){
            return Response.fail(response.getError());
        }

        //封装发货仓及下单店铺信息
        ShipmentPreview shipmentPreview = response.getResult();

        //发货仓库信息
        Response<Warehouse> warehouseRes = warehouseReadService.findById(warehouseId);
        if(!warehouseRes.isSuccess()){
            log.error("find warehouse by id:{} fail,error:{}",warehouseId,warehouseRes.getError());
            return Response.fail(warehouseRes.getError());
        }

        Warehouse warehouse = warehouseRes.getResult();
        shipmentPreview.setWarehouseId(warehouse.getId());
        shipmentPreview.setWarehouseName(warehouse.getName());
        String warehouseCode = warehouse.getCode();

        String companyCode;
        try {
            //获取公司编码
            companyCode = Splitter.on("-").splitToList(warehouseCode).get(0);
        }catch (Exception e){
            log.error("analysis warehouse code:{} fail,cause:{}",warehouseCode, Throwables.getStackTraceAsString(e));
            return Response.fail("analysis.warehouse.code.fail");
        }

        Response<WarehouseCompanyRule> ruleRes = warehouseCompanyRuleReadService.findByCompanyCode(companyCode);
        if(!ruleRes.isSuccess()){
            log.error("find warehouse company rule by company code:{} fail,error:{}",companyCode,ruleRes.getError());
            return Response.fail(ruleRes.getError());
        }

        WarehouseCompanyRule companyRule = ruleRes.getResult();
        if(Arguments.isNull(companyRule)){
            log.error("not find warehouse company rule by company code:{}",companyCode);
           return Response.fail("warehouse.company.rule.not.exist");
        }
        shipmentPreview.setErpOrderShopCode(String.valueOf(companyRule.getShopId()));
        shipmentPreview.setErpOrderShopName(companyRule.getShopName());
        shipmentPreview.setErpPerformanceShopCode(String.valueOf(companyRule.getShopId()));
        shipmentPreview.setErpPerformanceShopName(companyRule.getShopName());

        return Response.ok(shipmentPreview);
    }



}
