package com.pousheng.middle.web.biz.impl;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.middle.open.api.dto.SkxShipInfo;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.common.utils.Splitters;
import io.terminus.msg.common.StringUtil;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShipmentItem;
import io.terminus.parana.order.service.ShipmentWriteService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @Description: skx同步发货信息到中台
 * @author: yjc
 * @date: 2018/8/1下午6:41
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.SKX_SYNC_SHIPMENT_RESULT)
@Service
@Slf4j
public class SkxSycnShipmentService implements CompensateBizService {

    private final static DateTimeFormatter DFT = DateTimeFormat.forPattern("yyyyMMddHHmmss");
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @Autowired
    private SyncShipmentPosLogic syncShipmentPosLogic;
    @Autowired
    private HKShipmentDoneLogic hkShipmentDoneLogic;
    @Autowired
    private AutoCompensateLogic autoCompensateLogic;

    @RpcConsumer
    private OrderReadLogic orderReadLogic;
    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        if (null == poushengCompensateBiz) {
            log.warn("SkxSycnShipmentService.doProcess params is null");
            return;
        }
        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("SkxSycnShipmentService.doProcess context is null");
            return;
        }
        SkxShipInfo skxShipInfo = JsonMapper.nonEmptyMapper().fromJson(context, SkxShipInfo.class);
        if (skxShipInfo == null) {
            log.warn("SkxSycnShipmentService.doProcess shipInfos is null");
            return;
        }
        try {
            doBiz(skxShipInfo);
        } catch (Exception e) {
            log.error("SkxSycnShipmentService forEach skxShipInfo ({}) is error: {}", context, Throwables.getStackTraceAsString(e));
        }


    }

    private void doBiz(SkxShipInfo skxShipInfo) {
        DateTime dt = DateTime.parse(skxShipInfo.getShipmentDate(), DFT);
        Shipment shipment = null;
        String shipmentId = skxShipInfo.getShipmentId();
        try {
            shipment = shipmentReadLogic.findShipmentByShipmentCode(shipmentId);
        } catch (Exception e) {
            log.error("find shipment failed,shipment id is {} ,caused by {}", shipmentId, Throwables.getStackTraceAsString(e));
            return;
        }
        //判断状态及获取接下来的状态
        Flow flow = flowPicker.pickShipments();
        OrderOperation orderOperation = MiddleOrderEvent.SHIP.toOrderOperation();
        if (!flow.operationAllowed(shipment.getStatus(), orderOperation)) {
            log.error("shipment(id={})'s status({}) not fit for ship", shipment.getId(), shipment.getStatus());
            return;
        }
        Integer targetStatus = flow.target(shipment.getStatus(), orderOperation);
        List<ShipmentItem> items = shipmentReadLogic.getShipmentItems(shipment);
        //斯凯奇默认全部发货
        for (ShipmentItem s : items) {
            s.setShipQuantity(s.getQuantity());
        }
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);

        if (!Objects.equals(skxShipInfo.getErpShipmentId(), shipmentExtra.getOutShipmentId())) {
            log.error("hk shipment id:{} not equal middle shipment(id:{} ) out shipment id:{}", skxShipInfo.getErpShipmentId(), shipment.getId(), shipmentExtra.getOutShipmentId());
            return;
        }

        //封装更新信息
        Shipment update = new Shipment();
        update.setId(shipment.getId());
        Map<String, String> extraMap = shipment.getExtra();
        shipmentExtra.setShipmentSerialNo(skxShipInfo.getShipmentSerialNo());
        shipmentExtra.setShipmentCorpCode(skxShipInfo.getShipmentCorpCode());
        //通过恒康代码查找快递名称
        ExpressCode expressCode = orderReadLogic.makeExpressNameByhkCode(skxShipInfo.getShipmentCorpCode());
        shipmentExtra.setShipmentCorpName(expressCode.getName());
        shipmentExtra.setShipmentDate(dt.toDate());
        extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, mapper.toJson(shipmentExtra));
        extraMap.put(TradeConstants.SHIPMENT_ITEM_INFO, mapper.toJson(items));
        update.setExtra(extraMap);
        update.setShipmentSerialNo(skxShipInfo.getShipmentSerialNo());
        update.setShipmentCorpCode(skxShipInfo.getShipmentCorpCode());

        //更新状态
        Response<Boolean> updateStatusRes = shipmentWriteService.updateStatusByShipmentIdAndCurrentStatus(shipment.getId(), shipment.getStatus(), targetStatus);
        if (!updateStatusRes.isSuccess()) {
            log.error("update shipment(id:{}) status to :{} fail,error:{}", shipment.getId(), targetStatus, updateStatusRes.getError());
            return;
        }

        //更新基本信息
        Response<Boolean> updateRes = shipmentWriteService.update(update);
        if (!updateRes.isSuccess()) {
            log.error("update shipment(id:{}) extraMap to :{} fail,error:{}", shipment.getId(), extraMap, updateRes.getError());
            return;
        }
        //后续更新订单状态,扣减库存，通知电商发货（销售发货）等等
        hkShipmentDoneLogic.doneShipment(shipment);

        //等待整单到齐后再呼叫
        //同步pos单到恒康
//        Response<Boolean> response = syncShipmentPosLogic.syncShipmentPosToHk(shipment);
//        if (!response.isSuccess()) {
//            log.error("syncShipmentPosToHk shipment:{} fail,error:{}",shipment,response.getError());
//            Map<String, Object> param1 = Maps.newHashMap();
//            param1.put("shipmentId", shipment.getId());
//            autoCompensateLogic.createAutoCompensationTask(param1, TradeConstants.FAIL_SYNC_POS_TO_HK, response.getError());
//
//        }
    }
}
