package com.pousheng.middle.web.biz.impl;

import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.Lists;
import com.pousheng.middle.hksyc.pos.api.SycHkShipmentPosApi;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentDetail;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.biz.dto.DashBoardShipmentDTO;
import com.pousheng.middle.web.biz.dto.DashBoardShipmentItemDTO;
import com.pousheng.middle.web.biz.dto.HeadResponse;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.parana.attribute.dto.SkuAttribute;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShipmentItem;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.pousheng.middle.order.constant.TradeConstants.HK_COMPANY_CODE;
import static com.pousheng.middle.order.constant.TradeConstants.HK_PERFORMANCE_SHOP_OUT_CODE;

/**
 * 门店派单同步bashborad
 **/
@CompensateAnnotation(bizType = PoushengCompensateBizType.MPOS_SHIP_NOTIFY_DASHBOARD)
@Service
@Slf4j
public class MposDispatchSyncBashBoradServiceImpl implements CompensateBizService {

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private OpenShopCacher openShopCacher;
    @Autowired
    private SkuTemplateReadService skuTemplateReadService;
    @Autowired
    private SycHkShipmentPosApi sycHkShipmentPosApi;


    private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");


    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {


        DashBoardShipmentDTO dashBoardShipmentDTO = JsonMapper.nonDefaultMapper().fromJson(poushengCompensateBiz.getContext(), DashBoardShipmentDTO.class);
        Long shipmentId = Long.valueOf(poushengCompensateBiz.getBizId());
        // shipmentReadLogic 获取发货单详情
        ShipmentDetail shipmentDetail = shipmentReadLogic.orderDetail(shipmentId);
        Shipment shipment = shipmentDetail.getShipment();
        //getShipmentExtra()  发货单扩展信息
        ShipmentExtra shipmentExtra = shipmentDetail.getShipmentExtra();

        //set金额  shipmentExtra.getShipmentTotalPrice()发货单扩展信息里的金额
        dashBoardShipmentDTO.setAmount(shipmentExtra.getShipmentTotalPrice());
        //set生成时间 发货单的时间
        dashBoardShipmentDTO.setBillDate(formatter.print(shipment.getCreatedAt().getTime()));

        OpenShop openShop = openShopCacher.findById(shipment.getShopId());
        Map<String, String> extraMap = openShop.getExtra();
        //set来源店铺公司代码
        dashBoardShipmentDTO.setSourceShopCompanyCode(extraMap.get(HK_COMPANY_CODE));

        //set来源店铺代码
        dashBoardShipmentDTO.setSourceShopCode(extraMap.get(HK_PERFORMANCE_SHOP_OUT_CODE));

        //set商品信息
        dashBoardShipmentDTO.setGoodsList(makeDashBoardItems(shipmentDetail.getShipmentItems()));

        String url = "/commonerp/erp/sal/pushpublicorder";
        String result = sycHkShipmentPosApi.doSyncDashBoardShipment(dashBoardShipmentDTO, url);
        log.info("sync dash board result:{}", result);
        Map<String, Object> map = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(result, TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class));
        HeadResponse headResponse = JsonMapper.nonEmptyMapper().fromJson(JsonMapper.nonEmptyMapper().toJson(map.get("head")), HeadResponse.class);

        if (!Objects.equals(headResponse.getCode(), "0")) {
            log.error("sync shipment confirm at to hk fail,error:{}", headResponse.getMessage());
            throw new ServiceException(headResponse.getMessage());
        }


    }

    private List<DashBoardShipmentItemDTO> makeDashBoardItems(List<ShipmentItem> shipmentItems) {
        List<DashBoardShipmentItemDTO> dtos = Lists.newArrayListWithCapacity(shipmentItems.size());
        for (ShipmentItem shipmentItem : shipmentItems) {
            dtos.add(makeItem(shipmentItem));
        }
        return dtos;
    }

    private DashBoardShipmentItemDTO makeItem(ShipmentItem shipmentItem) {

        DashBoardShipmentItemDTO shipmentItemDTO = new DashBoardShipmentItemDTO();
        shipmentItemDTO.setQty(shipmentItem.getQuantity());


        Response<List<SkuTemplate>> rS = skuTemplateReadService.findBySkuCodes(Lists.newArrayList(shipmentItem.getSkuCode()));
        if (!rS.isSuccess()) {
            log.error("find sku template by sku codes:{} failed,error:{}", shipmentItem.getSkuCode(), rS.getError());
            throw new ServiceException(rS.getError());
        }
        List<SkuTemplate> skuTemplates = rS.getResult();
        Optional<SkuTemplate> skuTemplateOptional = skuTemplates.stream().findAny();
        if (!skuTemplateOptional.isPresent()) {
            log.error("not find sku template by sku code:{}", shipmentItem.getSkuCode());
            throw new ServiceException("find.sku.template.failed");
        }
        SkuTemplate skuTemplate = skuTemplateOptional.get();
        Map<String, String> extraMaps = skuTemplate.getExtra();

        if (extraMaps == null) {
            log.error("sku template(id:{}) extra is null", skuTemplate.getId());
            throw new ServiceException("sku.template.extra.map.is.null");
        }

        String materialCode = extraMaps.get(TradeConstants.HK_MATRIAL_CODE);
        // 货号
        shipmentItemDTO.setMaterialCode(materialCode);
        // 尺码
        String sizeName = "";
        List<SkuAttribute> skuAttributes = skuTemplate.getAttrs();
        for (SkuAttribute skuAttribute : skuAttributes) {
            if (Objects.equals(skuAttribute.getAttrKey(), "尺码")) {
                sizeName = skuAttribute.getAttrVal();
            }
        }
        shipmentItemDTO.setSizeCode(sizeName);

        return shipmentItemDTO;
    }


}
