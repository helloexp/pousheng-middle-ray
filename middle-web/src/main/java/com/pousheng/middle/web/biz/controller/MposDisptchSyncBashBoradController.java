package com.pousheng.middle.web.biz.controller;

import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.dto.DashBoardShipmentDTO;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MposDisptchSyncBashBoradController {

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private CompensateBizLogic compensateBizLogic;
    @Autowired
    private ShopReadService shopReadService;


    @RequestMapping(value = "/api/mpos/dispatch/sync/app/test", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response testSyncApp(Long shipmentId) {


       Shipment shipment = shipmentReadLogic.findShipmentById(shipmentId);
        //邮件提醒接单店铺
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        Response<Shop> shopResponse = shopReadService.findById(shipmentExtra.getWarehouseId());
        if (!shopResponse.isSuccess()) {
        }
        Shop shop = shopResponse.getResult();

        //同步bashborad
        DashBoardShipmentDTO dashBoardShipmentDTO = new DashBoardShipmentDTO();
        //订单号
        dashBoardShipmentDTO.setBillNo("23232");
        //订单类型
        dashBoardShipmentDTO.setBillType("普通");
        //出货店铺代码（提醒信息的店铺）
        dashBoardShipmentDTO.setOutShopCode(shop.getOuterId());
        //出货店铺公司代码
        dashBoardShipmentDTO.setDestinationCompanyCode(String.valueOf(shop.getBusinessId()));

        //PoushengCompensateBiz 中台业务处理表
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        //shipment 发货单
        biz.setBizId(shipment.getId().toString());
        // 门店派单同步
        biz.setBizType(PoushengCompensateBizType.MPOS_SHIP_NOTIFY_DASHBOARD.toString());
        //状态待处理
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        //明细内容
        biz.setContext(JsonMapper.nonDefaultMapper().toJson(dashBoardShipmentDTO));
        //mq消息推送
        compensateBizLogic.createBizAndSendMq(biz, MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC);
        return Response.ok();
    }

}
