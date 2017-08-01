package com.pousheng.middle.web.utils.permission;

import com.pousheng.middle.web.user.component.UserManageShopReader;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.RefundReadService;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.order.service.ShopOrderReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 登陆用户可操作店铺权限检查器
 * Created by sunbo@terminus.io on 2017/7/27.
 */
@Component
public class PermissionUtil {


    @Autowired
    private UserManageShopReader userManageShopReader;

    @RpcConsumer
    private ShopOrderReadService shopOrderReadService;
    @RpcConsumer
    private RefundReadService refundReadService;
    @RpcConsumer
    private ShipmentReadService shipmentReadService;

    /**
     * 检查当前登陆用户是否可以操作对应店铺
     *
     * @param shopID 店铺ID
     * @return
     */
    public Response<Boolean> checkByShopID(Long shopID) {

        if (null == shopID)
            return Response.fail("permission.check.shop.id.empty");

        if (null == UserUtil.getCurrentUser())
            return Response.fail("permission.check.current.user.empty");

        if (userManageShopReader.findManageShops(UserUtil.getCurrentUser()).stream().anyMatch(openClientShop -> openClientShop.getOpenShopId().equals(shopID)))
            return Response.ok();
        else
            return Response.fail("permission.check.not.allow");
    }

    /**
     * 检查订单是否属于当前登陆用户可操作店铺下的订单
     *
     * @param ShopOrderID
     * @return
     */
    public Response<Boolean> checkByShopOrderID(Long ShopOrderID) {
        if (null == ShopOrderID)
            return Response.fail("permission.check.shop.order.id.empty");
        Response<ShopOrder> shopOrderResponse = shopOrderReadService.findById(ShopOrderID);

        if (!shopOrderResponse.isSuccess())
            return Response.fail("permission.check.shop.order.found.fail");

        return checkByShopID(shopOrderResponse.getResult().getShopId());
    }


    public Response<Boolean> checkByRefundID(Long refundID) {
        if (null == refundID)
            return Response.fail("permission.check.refund.id.empty");
        Response<Refund> refundResponse = refundReadService.findById(refundID);

        if (!refundResponse.isSuccess())
            return Response.fail("permission.check.refund.found.fail");

        return checkByShopID(refundResponse.getResult().getShopId());
    }


    public Response<Boolean> checkByShipmentID(Long shipmentID) {
        if (null == shipmentID)
            return Response.fail("permission.check.shipment.id.empty");
        Response<Shipment> shipmentResponse = shipmentReadService.findById(shipmentID);
        if (!shipmentResponse.isSuccess())
            return Response.fail("permission.check.shipment.found.fail");
        return checkByShopID(shipmentResponse.getResult().getShopId());
    }

    /**
     * 获取当前登陆用户可操作店铺ID列表
     *
     * @return 可操作店铺ID列表
     */
    public List<Long> getCurrentUserCanOperateShopIDs() {

        if (null == UserUtil.getCurrentUser())
            throw new JsonResponseException("permission.check.current.user.empty");

        return userManageShopReader.findManageShops(UserUtil.getCurrentUser()).stream().map(OpenClientShop::getOpenShopId).collect(Collectors.toList());
    }
}
