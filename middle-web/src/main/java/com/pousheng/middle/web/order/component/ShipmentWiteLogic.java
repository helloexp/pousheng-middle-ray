package com.pousheng.middle.web.order.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentItem;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddlePayType;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.enums.OrderSource;
import com.pousheng.middle.order.service.MiddleShipmentWriteService;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import com.pousheng.middle.warehouse.model.WarehouseCompanyRule;
import com.pousheng.middle.warehouse.service.WarehouseAddressReadService;
import com.pousheng.middle.warehouse.service.WarehouseCompanyRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.warehouse.service.WarehouseSkuReadService;
import com.pousheng.middle.web.events.trade.OrderShipmentEvent;
import com.pousheng.middle.web.events.trade.UnLockStockEvent;
import com.pousheng.middle.web.order.sync.hk.SyncShipmentLogic;
import com.pousheng.middle.web.warehouses.algorithm.WarehouseChooser;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.fsm.Flow;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.enums.ShipmentType;
import io.terminus.parana.order.model.*;
import io.terminus.parana.order.service.ReceiverInfoReadService;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.order.service.ShipmentWriteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 发货单写服务
 * Created by songrenfei on 2017/7/2
 */
@Component
@Slf4j
public class ShipmentWiteLogic {
    @Autowired
    private MiddleOrderFlowPicker flowPicker;
    @Autowired
    private EventBus eventBus;
    @Autowired
    private SyncShipmentLogic syncShipmentLogic;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @RpcConsumer
    private WarehouseSkuReadService warehouseSkuReadService;
    @RpcConsumer
    private ReceiverInfoReadService receiverInfoReadService;
    @Autowired
    private WarehouseCompanyRuleReadService warehouseCompanyRuleReadService;
    @Autowired
    private WarehouseReadService warehouseReadService;
    @Autowired
    private WarehouseAddressReadService warehouseAddressReadService;
    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;
    @Autowired
    private WarehouseChooser warehouseChooser;
    @RpcConsumer
    private ShipmentReadService shipmentReadService;
    @Autowired
    private MiddleShipmentWriteService middleShipmentWriteService;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @Autowired
    private ObjectMapper objectMapper;

    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();

    public Response<Boolean> updateStatus(Shipment shipment, OrderOperation orderOperation) {

        Flow flow = flowPicker.pickShipments();
        if (!flow.operationAllowed(shipment.getStatus(), orderOperation)) {
            log.error("shipment(id:{}) current status:{} not allow operation:{}", shipment.getId(), shipment.getStatus(), orderOperation.getText());
            return Response.fail("shipment.status.not.allow.current.operation");
        }

        Integer targetStatus = flow.target(shipment.getStatus(), orderOperation);
        Response<Boolean> updateRes = shipmentWriteService.updateStatusByShipmentId(shipment.getId(), targetStatus);
        if (!updateRes.isSuccess()) {
            log.error("update shipment(id:{}) status to:{} fail,error:{}", shipment.getId(), updateRes.getError());
            return Response.fail(updateRes.getError());
        }
        return Response.ok();

    }


    //更新发货单
    public void update(Shipment shipment) {
        Response<Boolean> updateRes = shipmentWriteService.update(shipment);
        if (!updateRes.isSuccess()) {
            log.error("update shipment:{} fail,error:{}", shipment, updateRes.getError());
            throw new JsonResponseException(updateRes.getError());
        }
    }

    //更新发货单Extra
    public void updateExtra(Long shipmentId, Map<String, String> extraMap) {

        Shipment updateShipment = new Shipment();
        updateShipment.setId(shipmentId);
        updateShipment.setExtra(extraMap);

        this.update(updateShipment);
    }

    /**
     * 取消/删除发货单逻辑(撤销订单的时候通知恒康删除发货单,电商取消订单的时候取消发货单)
     *
     * @param shipment 发货单
     * @param type 0 取消 1 删除
     * @return 取消成功 返回返回true,取消失败返回false
     */
    boolean cancelShipment(Shipment shipment,Integer type) {
        try {
            Flow flow = flowPicker.pickShipments();
            //未同步恒康,现在只需要将发货单状态置为已取消即可
            if (flow.operationAllowed(shipment.getStatus(), MiddleOrderEvent.CANCEL_SHIP.toOrderOperation())) {
                Response<Boolean> cancelRes = this.updateStatus(shipment, MiddleOrderEvent.CANCEL_SHIP.toOrderOperation());
                if (!cancelRes.isSuccess()) {
                    log.error("cancel shipment(id:{}) fail,error:{}", shipment.getId(), cancelRes.getError());
                    throw new JsonResponseException(cancelRes.getError());
                }
                //解锁库存
                UnLockStockEvent unLockStockEvent = new UnLockStockEvent();
                unLockStockEvent.setShipment(shipment);
                eventBus.post(unLockStockEvent);
            }
            //已经同步过恒康,现在需要取消同步恒康,根据恒康返回的结果判断是否取消成功
            if (flow.operationAllowed(shipment.getStatus(), MiddleOrderEvent.CANCEL_HK.toOrderOperation())) {
                Response<Boolean> syncRes = syncShipmentLogic.syncShipmentCancelToHk(shipment,type);
                if(!syncRes.isSuccess()){
                    log.error("sync cancel shipment(id:{}) to hk fail,error:{}",shipment.getId(),syncRes.getError());
                    throw new JsonResponseException(syncRes.getError());
                }
                //解锁库存
                UnLockStockEvent unLockStockEvent = new UnLockStockEvent();
                unLockStockEvent.setShipment(shipment);
                eventBus.post(unLockStockEvent);
            }
            return true;
        } catch (Exception e) {
            log.error("cancel shipment failed,shipment id is :{},error{}",shipment.getId(),e.getMessage());
            return false;
        }
    }

    /**
     * 自动创建发货单
     * @param shopOrder 店铺订单
     */
    public void doAutoCreateShipment(ShopOrder shopOrder){
        List<SkuOrder> skuOrders = orderReadLogic.findSkuOrderByShopOrderIdAndStatus(shopOrder.getId(),
                MiddleOrderStatus.WAIT_HANDLE.getValue());
        if (skuOrders.size()==0){
            return;
        }
        //判断是否满足自动生成发货单
        if(!commValidateOfOrder(shopOrder,skuOrders)){
            return;
        }
        //获取skuCode,数量的集合
        List<SkuCodeAndQuantity> skuCodeAndQuantities = Lists.newArrayListWithCapacity(skuOrders.size());
        skuOrders.forEach(skuOrder -> {
            SkuCodeAndQuantity skuCodeAndQuantity = new SkuCodeAndQuantity();
            skuCodeAndQuantity.setSkuCode(skuOrder.getSkuCode());
            skuCodeAndQuantity.setQuantity(Integer.valueOf(orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.WAIT_HANDLE_NUMBER,skuOrder)));
            skuCodeAndQuantities.add(skuCodeAndQuantity);
        });
        //获取addressId
        Response<List<ReceiverInfo>> response =receiverInfoReadService.findByOrderId(shopOrder.getId(), OrderLevel.SHOP);
        if (!response.isSuccess()){
            log.error("find ReceiverInfo failed,shopOrderId is(:{})",shopOrder.getId());
            return;
        }
        ReceiverInfo receiverInfo = response.getResult().get(0);
        if(Arguments.isNull(receiverInfo.getCityId())){
            log.error("receive info:{} city id is null,so skip auto create shipment",receiverInfo);
            return;
        }


        //选择发货仓库
        List<WarehouseShipment> warehouseShipments = warehouseChooser.choose(shopOrder.getShopId(),Long.valueOf(receiverInfo.getCityId()),skuCodeAndQuantities);
        //遍历不同的发货仓生成相应的发货单
        for (WarehouseShipment warehouseShipment:warehouseShipments){

            Long shipmentId = this.createShipment(shopOrder, skuOrders, warehouseShipment);
            //抛出一个事件,修改子单和总单的状态,待处理数量,并同步恒康
            if (shipmentId!=null){
                //修改状态
                Response<Shipment> shipmentRes = shipmentReadService.findById(shipmentId);
                if (!shipmentRes.isSuccess()) {
                    log.error("failed to find shipment by id={}, error code:{}", shipmentId, shipmentRes.getError());
                    return;
                }
                try{
                    orderWriteLogic.updateSkuHandleNumber(shipmentRes.getResult().getSkuInfos());
                }catch (ServiceException e){
                    log.error("shipment id is {} update sku handle number failed.caused by {}",shipmentId,e.getMessage());
                }
                //同步恒康
                Response<Boolean> syncRes = syncShipmentLogic.syncShipmentToHk(shipmentRes.getResult());
                if (!syncRes.isSuccess()) {
                    log.error("sync shipment(id:{}) to hk fail,error:{}", shipmentId, syncRes.getError());
                }
            }
        }
    }

    /**
     * 创建发货单
     * @param shopOrder 店铺订单
     * @param skuOrders  子单
     * @param warehouseShipment 发货仓库信息
     */
    private Long createShipment(ShopOrder shopOrder, List<SkuOrder> skuOrders, WarehouseShipment warehouseShipment) {
        //获取该仓库中可发货的skuCode和数量的集合
        List<SkuCodeAndQuantity> skuCodeAndQuantitiesChooser = warehouseShipment.getSkuCodeAndQuantities();
        //获取仓库的id
        long warehouseId = warehouseShipment.getWarehouseId();
        //获取skuOid,quantity的集合
        Map<Long, Integer> skuOrderIdAndQuantity = Maps.newHashMap();

        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantitiesChooser) {
            if (skuCodeAndQuantity.getQuantity()==0){
                continue;
            }
            skuOrderIdAndQuantity.put(this.getSkuOrder(skuOrders, skuCodeAndQuantity.getSkuCode()).getId(), skuCodeAndQuantity.getQuantity());
        }
        if (skuOrderIdAndQuantity.size()==0){
            return null;
        }
        //获取该发货单中涉及到的sku订单
        List<Long> skuOrderIds = Lists.newArrayListWithCapacity(skuOrderIdAndQuantity.size());
        skuOrderIds.addAll(skuOrderIdAndQuantity.keySet());
        List<SkuOrder> skuOrdersShipment = orderReadLogic.findSkuOrdersByIds(skuOrderIds);
        //封装发货信息
        List<ShipmentItem> shipmentItems = makeShipmentItems(skuOrdersShipment, skuOrderIdAndQuantity);
        //发货单商品金额
        Long shipmentItemFee=0L;
        //发货单总的优惠
        Long shipmentDiscountFee=0L;
        //发货单总的净价
        Long shipmentTotalFee=0L;
        //运费
        Long shipmentShipFee =0L;
        //运费优惠
        Long shipmentShipDiscountFee=0L;
        //判断运费是否已经加过
        if (!isShipmentFeeCalculated(shopOrder.getId())){
            shipmentShipFee = Long.valueOf(shopOrder.getOriginShipFee()==null?0:shopOrder.getOriginShipFee());
            shipmentShipDiscountFee = shipmentShipFee  - Long.valueOf(shopOrder.getShipFee());
        }
        for (ShipmentItem shipmentItem : shipmentItems) {
            shipmentItemFee = shipmentItem.getSkuPrice()*shipmentItem.getQuantity() + shipmentItemFee;
            shipmentDiscountFee = shipmentItem.getSkuDiscount()+shipmentDiscountFee;
            shipmentTotalFee = shipmentItem.getCleanFee()+shipmentTotalFee;
        }
        //订单总金额(运费优惠已经包含在子单折扣中)=商品总净价+运费
        Long shipmentTotalPrice=shipmentTotalFee+shipmentShipFee-shipmentShipDiscountFee;;

        Shipment shipment = this.makeShipment(shopOrder, warehouseId,shipmentItemFee
                ,shipmentDiscountFee,shipmentTotalFee,shipmentShipFee,shipmentShipDiscountFee,shipmentTotalPrice);
        shipment.setSkuInfos(skuOrderIdAndQuantity);
        shipment.setType(ShipmentType.SALES_SHIP.value());
        shipment.setShopId(shopOrder.getShopId());
        shipment.setShopName(shopOrder.getShopName());
        Map<String, String> extraMap = shipment.getExtra();
        extraMap.put(TradeConstants.SHIPMENT_ITEM_INFO, JSON_MAPPER.toJson(shipmentItems));
        shipment.setExtra(extraMap);
        //创建发货单
        Response<Long> createResp = shipmentWriteService.create(shipment, Arrays.asList(shopOrder.getId()), OrderLevel.SHOP);
        if (!createResp.isSuccess()) {
            log.error("fail to create shipment:{} for order(id={}),and level={},cause:{}",
                    shipment, shopOrder.getId(), OrderLevel.SHOP.getValue(), createResp.getError());
            throw new JsonResponseException(createResp.getError());
        }
        return createResp.getResult();
    }

    /**
     * 是否满足自动创建发货单的校验
     * @param shopOrder 店铺订单
     * @return 不可以自动创建发货单(false),可以自动创建发货单(true)
     */
    private boolean commValidateOfOrder(ShopOrder shopOrder,List<SkuOrder> skuOrders){
        //1.判断订单是否是京东支付 && 2.判断订单是否是货到付款
        if (Objects.equals(shopOrder.getType(), OrderSource.JD.value())
                && Objects.equals(shopOrder.getPayType(), MiddlePayType.CASH_ON_DELIVERY.getValue())){
            return false;
        }
        //3.判断订单有无备注
        if (StringUtils.isNotEmpty(shopOrder.getBuyerNote())){
            return false;
        }
        //4.判断skuCode是否为空,如果存在skuCode为空则不能自动生成发货单
        int count = 0;
        for (SkuOrder skuOrder:skuOrders){
            if (StringUtils.isEmpty(skuOrder.getSkuCode())){
                count++;
            }
        }
        return count <= 0;
    }

    /**
     * 根据skuCode获取skuOrder
     * @param skuOrders 子单集合
     * @param skuCode sku代码
     * @return 返回经过过滤的skuOrder记录
     */
    private SkuOrder getSkuOrder(List<SkuOrder> skuOrders, String skuCode){
        return skuOrders.stream().filter(Objects::nonNull).filter(it->Objects.equals(it.getSkuCode(),skuCode)).collect(Collectors.toList()).get(0);
    }

    /**
     * 组装发货单参数
     * @param shopOrder 店铺订单
     * @param warehouseId 发货仓主键
     * @return 返回组装的发货单
     */
    private Shipment makeShipment(ShopOrder shopOrder,Long warehouseId,Long shipmentItemFee,Long shipmentDiscountFee,
                                  Long shipmentTotalFee,Long shipmentShipFee,Long shipmentShipDiscountFee,
                                  Long shipmentTotalPrice){
        Shipment shipment = new Shipment();
        shipment.setStatus(MiddleShipmentsStatus.WAIT_SYNC_HK.getValue());
        shipment.setReceiverInfos(findReceiverInfos(shopOrder.getId(), OrderLevel.SHOP));

        //发货仓库信息
        Warehouse warehouse = findWarehouseById(warehouseId);
        Map<String,String> extraMap = Maps.newHashMap();
        ShipmentExtra shipmentExtra = new ShipmentExtra();
        shipmentExtra.setWarehouseId(warehouse.getId());
        shipmentExtra.setWarehouseName(warehouse.getName());

        Response<WarehouseCompanyRule> ruleRes = shipmentReadLogic.findCompanyRuleByWarehouseCode(warehouse.getCode());
        if (!ruleRes.isSuccess()) {
            log.error("find warehouse company rule by company code:{} fail,error:{}", warehouse.getCode(), ruleRes.getError());
            throw new JsonResponseException(ruleRes.getError());
        }

        WarehouseCompanyRule companyRule = ruleRes.getResult();
        shipmentExtra.setErpOrderShopCode(String.valueOf(companyRule.getShopId()));
        shipmentExtra.setErpOrderShopName(companyRule.getShopName());
        shipmentExtra.setErpPerformanceShopCode(String.valueOf(companyRule.getShopId()));
        shipmentExtra.setErpPerformanceShopName(companyRule.getShopName());

        shipmentExtra.setShipmentItemFee(shipmentItemFee);
        //发货单运费金额
        shipmentExtra.setShipmentShipFee(shipmentShipFee);
        //发货单优惠金额
        shipmentExtra.setShipmentDiscountFee(shipmentDiscountFee);
        //发货单总的净价
        shipmentExtra.setShipmentTotalFee(shipmentTotalFee);
        shipmentExtra.setShipmentShipDiscountFee(shipmentShipDiscountFee);
        shipmentExtra.setShipmentTotalPrice(shipmentTotalPrice);
        //添加物流编码
        if (Objects.equals(shopOrder.getType(), OrderSource.JD.value())
                && Objects.equals(shopOrder.getPayType(), MiddlePayType.CASH_ON_DELIVERY.getValue())){
            shipmentExtra.setVendCustID(TradeConstants.JD_VEND_CUST_ID);
        }else{
            shipmentExtra.setVendCustID(TradeConstants.OPTIONAL_VEND_CUST_ID);
        }
        extraMap.put(TradeConstants.SHIPMENT_EXTRA_INFO,JSON_MAPPER.toJson(shipmentExtra));

        shipment.setExtra(extraMap);

        return shipment;
    }

    /**
     * 查找收货人信息
     * @param orderId 订单主键
     * @param orderLevel 订单级别 店铺订单or子单
     * @return 收货人信息的json串
     */
    private String findReceiverInfos(Long orderId, OrderLevel orderLevel) {

        List<ReceiverInfo> receiverInfos = doFindReceiverInfos(orderId, orderLevel);

        if (CollectionUtils.isEmpty(receiverInfos)) {
            log.error("receiverInfo not found where orderId={}", orderId);
            throw new JsonResponseException("receiver.info.not.found");
        }

        ReceiverInfo receiverInfo = receiverInfos.get(0);

        try {
            return objectMapper.writeValueAsString(receiverInfo);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    /**
     *查找收货人信息
     * @param orderId 订单主键
     * @param orderLevel 订单级别 店铺订单or子单
     * @return 收货人信息的list集合
     */
    private List<ReceiverInfo> doFindReceiverInfos(Long orderId, OrderLevel orderLevel) {
        Response<List<ReceiverInfo>> receiversResp = receiverInfoReadService.findByOrderId(orderId, orderLevel);
        if (!receiversResp.isSuccess()) {
            log.error("fail to find receiver info by order id={},and order level={},cause:{}",
                    orderId, orderLevel.getValue(), receiversResp.getError());
            throw new JsonResponseException(receiversResp.getError());
        }
        return receiversResp.getResult();
    }

    /**
     * 获取发货仓库信息
     * @param warehouseId 仓库主键
     * @return  仓库信息
     */
    private Warehouse findWarehouseById(Long warehouseId){
        Response<Warehouse> warehouseRes = warehouseReadService.findById(warehouseId);
        if(!warehouseRes.isSuccess()){
            log.error("find warehouse by id:{} fail,error:{}",warehouseId,warehouseRes.getError());
            throw new JsonResponseException(warehouseRes.getError());
        }

        return warehouseRes.getResult();
    }

    /**
     * 发货单中填充sku订单信息
     * @param skuOrders 子单集合
     * @param skuOrderIdAndQuantity 子单的主键和数量的集合
     * @return shipmentItem的集合
     */
    private List<ShipmentItem> makeShipmentItems(List<SkuOrder> skuOrders, Map<Long,Integer> skuOrderIdAndQuantity){
        Map<Long,SkuOrder> skuOrderMap = skuOrders.stream().filter(Objects::nonNull).collect(Collectors.toMap(SkuOrder::getId,it -> it));
        List<ShipmentItem> shipmentItems = Lists.newArrayListWithExpectedSize(skuOrderIdAndQuantity.size());
        for (Long skuOrderId : skuOrderIdAndQuantity.keySet()){
            ShipmentItem shipmentItem = new ShipmentItem();
            SkuOrder skuOrder = skuOrderMap.get(skuOrderId);
            shipmentItem.setQuantity(skuOrderIdAndQuantity.get(skuOrderId));
            shipmentItem.setRefundQuantity(0);
            shipmentItem.setSkuOrderId(skuOrderId);
            shipmentItem.setSkuName(skuOrder.getItemName());
            shipmentItem.setSkuPrice(Math.round(skuOrder.getOriginFee() / shipmentItem.getQuantity()));
            //积分
            String originIntegral = orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.SKU_INTEGRAL,skuOrder);
            Integer integral = StringUtils.isEmpty(originIntegral)?0:Integer.valueOf(originIntegral);
            shipmentItem.setIntegral(this.getIntegral(integral,skuOrder.getQuantity(),skuOrderIdAndQuantity.get(skuOrderId)));
            Long disCount = skuOrder.getDiscount()+Long.valueOf(this.getShareDiscount(skuOrder));
            shipmentItem.setSkuDiscount(this.getDiscount(skuOrder.getQuantity(),skuOrderIdAndQuantity.get(skuOrderId), Math.toIntExact(disCount)));
            shipmentItem.setCleanFee(this.getCleanFee(shipmentItem.getSkuPrice(),shipmentItem.getSkuDiscount(),shipmentItem.getQuantity()));
            shipmentItem.setCleanPrice(this.getCleanPrice(shipmentItem.getCleanFee(),shipmentItem.getQuantity()));
            shipmentItem.setOutSkuCode(skuOrder.getOutSkuId());
            shipmentItem.setSkuCode(skuOrder.getSkuCode());
            //商品id
            String outItemId="";
            try{
                outItemId =  orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.MIDDLE_OUT_ITEM_ID,skuOrder);
            }catch (Exception e){
                log.info("outItemmId is not exist");
            }
            shipmentItem.setItemId(outItemId);
            //商品属性
            shipmentItem.setAttrs(skuOrder.getSkuAttrs());

            shipmentItems.add(shipmentItem);

        }


        return shipmentItems;
    }


    /**
     *
     * @param skuQuantity  sku订单中商品的数量
     * @param shipSkuQuantity 发货的sku商品的数量
     * @param skuDiscount sku订单中商品的折扣
     * @return 返回四舍五入的计算结果,得到发货单中的sku商品的折扣
     */
    private  Integer getDiscount(Integer skuQuantity,Integer shipSkuQuantity,Integer skuDiscount){
        return Math.round(skuDiscount*shipSkuQuantity/skuQuantity);
    }

    /**
     * 计算总净价
     * @param skuPrice 商品原价
     * @param discount 发货单中sku商品的折扣
     * @param shipSkuQuantity 发货单中sku商品的数量
     * @return 返回sku商品总的净价
     */
    private Integer getCleanFee(Integer skuPrice,Integer discount,Integer shipSkuQuantity){

        return skuPrice*shipSkuQuantity-discount;
    }

    /**
     * 计算商品净价
     * @param cleanFee 商品总净价
     * @param shipSkuQuantity 发货单中sku商品的数量
     * @return 返回sku商品净价
     */
    private Integer getCleanPrice(Integer cleanFee,Integer shipSkuQuantity){
        return Math.round(cleanFee/shipSkuQuantity);
    }

    /**
     * 计算积分
     * @param integral sku订单获取的积分
     * @param skuQuantity sku订单总的数量
     * @param shipmentSkuQuantity 发货单中该sku订单的数量
     * @return 获取发货单中sku订单的积分
     */
    private Integer getIntegral(Integer integral,Integer skuQuantity,Integer shipmentSkuQuantity){
        return Math.round(integral*shipmentSkuQuantity/skuQuantity);
    }
    /**
     * 判断是否存在有效的发货单
     * @param shopOrderId 店铺订单主键
     * @return true:已经计算过发货单,false:没有计算过发货单
     */
    private boolean isShipmentFeeCalculated(long shopOrderId){
        Response<List<Shipment>> response =shipmentReadService.findByOrderIdAndOrderLevel(shopOrderId,OrderLevel.SHOP);
        if (!response.isSuccess()){
            log.error("find shipment failed,shopOrderId is ({})",shopOrderId);
            throw new JsonResponseException("shipment.find.fail");
        }
        //获取有效的销售发货单
        List<Shipment> shipments = response.getResult().stream().filter(Objects::nonNull).
                filter(it->!Objects.equals(it.getStatus(),MiddleShipmentsStatus.CANCELED.getValue())).
                filter(it->Objects.equals(it.getType(),ShipmentType.SALES_SHIP.value())).collect(Collectors.toList());
        int count =0;
        for (Shipment shipment:shipments){
            ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
            if (shipmentExtra.getShipmentShipFee()>0){
                count++;
            }
        }
        //如果已经有发货单计算过运费,返回true
        return count > 0;
    }
    private String getShareDiscount(SkuOrder skuOrder){
        return orderReadLogic.getSkuExtraMapValueByKey(TradeConstants.SKU_SHARE_DISCOUNT,skuOrder);
    }
}
