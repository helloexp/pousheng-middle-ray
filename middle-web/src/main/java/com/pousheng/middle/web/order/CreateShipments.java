package com.pousheng.middle.web.order;

import com.pousheng.middle.order.dto.ShipmentPreview;
import com.pousheng.middle.order.dto.WaitShipItemInfo;
import io.terminus.common.model.Response;
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
    private Shipments shipments;


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

        if(Objects.equals(1,type)){
            return shipments.shipPreview(id,data,warehouseId);
        }

        return refunds.changeShipPreview(id,data,warehouseId);

    }

}
