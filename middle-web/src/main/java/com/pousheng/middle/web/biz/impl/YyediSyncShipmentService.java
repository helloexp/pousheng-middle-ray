/**
 * Copyright (C), 2012-2018, XXX有限公司
 * FileName: YyediSyncShipmentService
 * Author:   xiehong
 * Date:     2018/5/29 下午8:34
 * Description: yyedi回传发货信息业务处理
 * History:
 * <author>          <time>          <version>          <desc>
 * 作者姓名           修改时间           版本号              描述
 */
package com.pousheng.middle.web.biz.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.middle.open.api.dto.YyEdiShipInfo;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.order.service.OrderShipmentWriteService;
import com.pousheng.middle.web.biz.CompensateBizService;
import com.pousheng.middle.web.biz.Exception.BizException;
import com.pousheng.middle.web.biz.annotation.CompensateAnnotation;
import com.pousheng.middle.web.order.component.*;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentPosLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.msg.common.StringUtil;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShipmentItem;
import io.terminus.parana.order.service.ShipmentWriteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 〈yyedi回传发货信息业务处理〉
 *
 * @author xiehong
 * @create 2018/5/29 下午8:34
 */
@CompensateAnnotation(bizType = PoushengCompensateBizType.YYEDI_SYNC_SHIPMENT_RESULT)
@Service
@Slf4j
public class YyediSyncShipmentService implements CompensateBizService {


    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    @Autowired
    private MiddleOrderFlowPicker flowPicker;

    @Autowired
    private OrderReadLogic orderReadLogic;

    @Autowired
    private SyncShipmentPosLogic syncShipmentPosLogic;

    @Autowired
    private HKShipmentDoneLogic hKShipmentDoneLogic;

    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;

    @RpcConsumer
    private OrderShipmentWriteService orderShipmentWriteService;

    @Autowired
    private AutoCompensateLogic autoCompensateLogic;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    @RpcConsumer
    private OrderShipmentReadService orderShipmentReadService;

    @Override
    public void doProcess(PoushengCompensateBiz poushengCompensateBiz) {
        if (null == poushengCompensateBiz) {
            log.warn("YyediSyncShipmentService.doProcess params is null");
            return;
        }
        String context = poushengCompensateBiz.getContext();
        if (StringUtil.isBlank(context)) {
            log.warn("YyediSyncShipmentService.doProcess context is null");
            return;
        }
        List<YyEdiShipInfo> shipInfos = JsonMapper.nonEmptyMapper().fromJson(context, JsonMapper.nonEmptyMapper().createCollectionType(List.class, YyEdiShipInfo.class));
        if (CollectionUtils.isEmpty(shipInfos)) {
            log.warn("YyediSyncShipmentService.doProcess shipInfos is null");
            return;
        }
        shipInfos.stream().forEach(a -> {
            String shipmentId = a.getShipmentId();
            try {
                this.oneBiz(a);

            } catch (Exception e) {
                log.error("YyediSyncShipmentService. forEach shipInfos ({}) is error: {}", shipmentId, Throwables.getStackTraceAsString(e));
            }

        });


    }

    private void oneBiz(YyEdiShipInfo yyEdiShipInfo) {
        DateTime dt = new DateTime();
        String shipmentCode = yyEdiShipInfo.getShipmentId();
        Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(shipmentCode);
        if (null == shipment) {
            log.warn("YyediSyncShipmentService.oneBiz query shipment is null");
            return;
        }
        Long shipmentId = shipment.getId();
        //判断状态及获取接下来的状态
        Flow flow = flowPicker.pickShipments();
        OrderOperation orderOperation = MiddleOrderEvent.SHIP.toOrderOperation();
        Integer targetStatus = flow.target(shipment.getStatus(), orderOperation);
        //检查是否是部分发货
        List<ShipmentItem> items = shipmentReadLogic.getShipmentItems(shipment);

        Boolean partShip = Boolean.FALSE;
        Map<String,String> boxNoMap=null;
        if (yyEdiShipInfo.getItemInfos() != null) {
            //构造箱号数据
            boxNoMap = yyEdiShipInfo.getItemInfos().stream().
                collect(
                    Collectors.toMap(
                        YyEdiShipInfo.ItemInfo::getSkuCode,
                        YyEdiShipInfo.ItemInfo::getBoxNo)
                );

            Map<String, Integer> itemMap = yyEdiShipInfo.getItemInfos().stream()
                    .collect(Collectors.toMap(YyEdiShipInfo.ItemInfo::getSkuCode, YyEdiShipInfo.ItemInfo::getQuantity));
            //查询订单来源 以区分是否是JIT渠道的大订单
            ShopOrder order = queryShopOrder(shipmentId);
            if (order == null) {
                throw new BizException("failed to query shop order");
            }
            boolean fromJit = MiddleChannel.YUNJUJIT.getValue().equals(order.getOutFrom());
            for (ShipmentItem s : items) {
                assignShipmentQuantity(s, itemMap, fromJit, partShip);
            }
        }else{
            for (ShipmentItem s : items) {
                s.setShipQuantity(s.getQuantity());
            }
        }

        //更新状态
        Response<Boolean> updateStatusRes = shipmentWriteService.updateStatusByShipmentId(shipmentId, targetStatus);
        if (!updateStatusRes.isSuccess()) {
            log.error("update shipment(id:{}) status to :{} fail,error:{}", shipmentId, targetStatus, updateStatusRes.getError());
            return;
        }
        if (partShip) {
            Response<Boolean> updatePartRes = orderShipmentWriteService.updatePartShip(shipmentId, partShip);
            if (!updatePartRes.isSuccess()) {
                log.error("update order shipment(shipmentId:{}) partShip to :{} fail,error:{}", shipmentId, partShip, updateStatusRes.getError());
                return;
            }
        }
        ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
        //封装更新信息
        Shipment update = new Shipment();
        update.setId(shipmentId);
        Map<String, String> extraMap = shipment.getExtra();
        //为了防止yyedi重复请求，清理备注
        update.setShipmentCorpCode(yyEdiShipInfo.getShipmentCorpCode());
        update.setShipmentSerialNo(yyEdiShipInfo.getShipmentSerialNo());
        shipmentExtra.setRemark(null);
        shipmentExtra.setShipmentSerialNo(yyEdiShipInfo.getShipmentSerialNo());
        shipmentExtra.setShipmentCorpCode(yyEdiShipInfo.getShipmentCorpCode());

        //增加jit新增字段
        shipmentExtra.setExpectDate(yyEdiShipInfo.getExpectDate());
        shipmentExtra.setTransportMethodCode(yyEdiShipInfo.getTransportMethodCode());
        shipmentExtra.setTransportMethodName(yyEdiShipInfo.getTransportMethodName());
        shipmentExtra.setCardRemark(yyEdiShipInfo.getCardRemark());
        shipmentExtra.setBoxNoMap(boxNoMap);

        if (Objects.isNull(yyEdiShipInfo.getWeight())) {
            shipmentExtra.setWeight(0L);
        } else {
            shipmentExtra.setWeight(yyEdiShipInfo.getWeight());
        }
        //通过恒康代码查找快递名称
        ExpressCode expressCode = orderReadLogic.makeExpressNameByhkCode(yyEdiShipInfo.getShipmentCorpCode());
        shipmentExtra.setShipmentCorpName(expressCode.getName());
        shipmentExtra.setShipmentDate(dt.toDate());
        // TODO 兼容下yyEdi回传的yyEDIShipmentId 和 yjERP回传的yjShipmentId, 不同的派发中心只会传其对应的发货单号。
        shipmentExtra.setOutShipmentId(MoreObjects.firstNonNull(yyEdiShipInfo.getYyEDIShipmentId(), yyEdiShipInfo.getYjShipmentId()));

        extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO, mapper.toJson(shipmentExtra));
        extraMap.put(TradeConstants.SHIPMENT_ITEM_INFO, mapper.toJson(items));
        update.setExtra(extraMap);
        //更新基本信息
        Response<Boolean> updateRes = shipmentWriteService.update(update);
        if (!updateRes.isSuccess()) {
            log.error("update shipment(id:{}) extraMap to :{} fail,error:{}", shipmentId, extraMap, updateRes.getError());
            return;
        }

        log.info("try to sync shipment(id:{}) to hk", shipmentId);

        //后续更新订单状态,扣减库存，通知电商发货（销售发货）等等
        hKShipmentDoneLogic.doneShipment(shipment);

        //同步pos单到恒康
        Response<Boolean> response = syncShipmentPosLogic.syncShipmentPosToHk(shipment);
        if (!response.isSuccess()) {
            log.error("syncShipmentPosToHk shipment (id:{}) is error ", shipmentId);
            Map<String, Object> param1 = Maps.newHashMap();
            param1.put("shipmentId", shipment.getId());
            autoCompensateLogic.createAutoCompensationTask(param1, TradeConstants.FAIL_SYNC_POS_TO_HK,response.getError());
        }

    }

    /**
     * 根据发货单号反查订单信息
     *
     * @param shipmentId
     * @return
     */
    private ShopOrder queryShopOrder(Long shipmentId) {
        Response<OrderShipment> orderShipmentResponse = orderShipmentReadService.findByShipmentId(shipmentId);
        if (!orderShipmentResponse.isSuccess() || orderShipmentResponse.getResult() == null) {
            throw new BizException("failed to query order shipment.");
        }
        SkuOrder skuOrderInfo = (SkuOrder)orderReadLogic.findOrder(orderShipmentResponse.getResult().getOrderId(),
            OrderLevel.SKU);
        if (skuOrderInfo == null) {
            throw new BizException("failed to query sku order");
        }
        return orderReadLogic.findShopOrderById(skuOrderInfo.getOrderId());
    }

    /**
     * 分配发货数量给sku
     * @param item
     * @param quantityMap
     * @param fromJit
     * @param partShip
     */
    private void assignShipmentQuantity(ShipmentItem item, Map<String, Integer> quantityMap, boolean fromJit,
                                        boolean partShip) {
        Integer shipmentQuantity = quantityMap.get(item.getSkuCode());
        //JIT发货回执会根据SkuCode合并成总数 故需要遍历shipmentItem 分别分配发货数量
        if (fromJit) {
            if (item.getQuantity() > shipmentQuantity) {
                partShip = Boolean.TRUE;
                item.setShipQuantity(shipmentQuantity);
                //设置剩下的总数为0
                quantityMap.put(item.getSkuCode(), 0);
            } else {
                item.setShipQuantity(item.getQuantity());
                //设置总数为减去已分配过的数量
                quantityMap.put(item.getSkuCode(), shipmentQuantity - item.getQuantity());
            }
        } else {
            if (item.getQuantity() > quantityMap.get(item.getSkuCode())) {
                partShip = Boolean.TRUE;
            }
            item.setShipQuantity(quantityMap.get(item.getSkuCode()));
        }
    }

}
