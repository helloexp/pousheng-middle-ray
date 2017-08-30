package com.pousheng.middle.open;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.hksyc.dto.trade.ReceiverInfoHandleResult;
import com.pousheng.middle.open.erp.ErpOpenApiClient;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.fsm.MiddleOrderEvent;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.EcpOrderStatus;
import com.pousheng.middle.spu.service.PoushengMiddleSpuService;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import com.pousheng.middle.warehouse.service.WarehouseAddressReadService;
import com.pousheng.middle.web.events.trade.NotifyHkOrderDoneEvent;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import com.taobao.api.domain.Trade;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.job.order.component.DefaultOrderReceiver;
import io.terminus.open.client.common.channel.OpenClientChannel;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.order.dto.OpenClientFullOrder;
import io.terminus.open.client.order.dto.OpenClientOrderConsignee;
import io.terminus.open.client.order.dto.OpenClientOrderInvoice;
import io.terminus.open.client.order.enums.OpenClientOrderStatus;
import io.terminus.parana.item.model.Item;
import io.terminus.parana.item.model.Sku;
import io.terminus.parana.order.dto.RichOrder;
import io.terminus.parana.order.dto.RichSku;
import io.terminus.parana.order.dto.RichSkusByShop;
import io.terminus.parana.order.dto.fsm.OrderOperation;
import io.terminus.parana.order.model.Invoice;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.InvoiceWriteService;
import io.terminus.parana.order.service.OrderWriteService;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SpuReadService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by cp on 7/25/17
 */
@Component
@Slf4j
public class PsOrderReceiver extends DefaultOrderReceiver {

    @RpcConsumer
    private SpuReadService spuReadService;

    @RpcConsumer
    private PoushengMiddleSpuService middleSpuService;

    @RpcConsumer
    private OrderWriteService orderWriteService;

    @RpcConsumer
    private InvoiceWriteService invoiceWriteService;

    @Autowired
    private OrderWriteLogic orderWriteLogic;

    @Autowired
    private ErpOpenApiClient erpOpenApiClient;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private WarehouseAddressReadService warehouseAddressReadService;

    @Override
    protected Item findItemById(Long paranaItemId) {
        //TODO use cache

        Response<Spu> findR = spuReadService.findById(paranaItemId);
        if (!findR.isSuccess()) {
            log.error("fail to find spu by id={},cause:{}", paranaItemId, findR.getError());
            return null;
        }
        Spu spu = findR.getResult();

        Item item = new Item();
        item.setId(spu.getId());
        item.setName(spu.getName());
        item.setMainImage(spu.getMainImage_());
        return item;
    }

    @Override
    protected Sku findSkuByCode(String skuCode) {
        //TODO use cache
        Response<Optional<SkuTemplate>> findR = middleSpuService.findBySkuCode(skuCode);
        if (!findR.isSuccess()) {
            log.error("fail to find sku template by code={},cause:{}",
                    skuCode, findR.getError());
            return null;
        }
        Optional<SkuTemplate> skuTemplateOptional = findR.getResult();
        if (!skuTemplateOptional.isPresent()) {
            return null;
        }
        SkuTemplate skuTemplate = skuTemplateOptional.get();

        Sku sku = new Sku();
        sku.setId(skuTemplate.getId());
        sku.setName(skuTemplate.getName());
        sku.setPrice(skuTemplate.getPrice());
        sku.setSkuCode(skuTemplate.getSkuCode());
        try {
            sku.setExtraPrice(skuTemplate.getExtraPrice());
        } catch (Exception e) {
            //ignore
        }
        sku.setImage(skuTemplate.getImage_());
        sku.setAttrs(skuTemplate.getAttrs());
        return sku;
    }

    @Override
    protected Integer toParanaOrderStatusForShopOrder(OpenClientOrderStatus clientOrderStatus) {
        return OpenClientOrderStatus.PAID.getValue();
    }

    @Override
    protected Integer toParanaOrderStatusForSkuOrder(OpenClientOrderStatus clientOrderStatus) {
        return OpenClientOrderStatus.PAID.getValue();
    }

    protected void updateParanaOrder(ShopOrder shopOrder, OpenClientFullOrder openClientFullOrder) {
        if (openClientFullOrder.getStatus() == OpenClientOrderStatus.CONFIRMED) {
            Response<Boolean> updateR = orderWriteService.shopOrderStatusChanged(shopOrder.getId(),
                    shopOrder.getStatus(), MiddleOrderStatus.CONFIRMED.getValue());
            if (!updateR.isSuccess()) {
                log.error("failed to change shopOrder(id={})'s status from {} to {} when sync order, cause:{}",
                        shopOrder.getId(), shopOrder.getStatus(), MiddleOrderStatus.CONFIRMED.getValue(), updateR.getError());
            }else {
                //更新同步电商状态为已确认收货
                OrderOperation successOperation = MiddleOrderEvent.CONFIRM.toOrderOperation();
                Response<Boolean> response = orderWriteLogic.updateEcpOrderStatus(shopOrder, successOperation);
                if (response.isSuccess()) {
                    //通知恒康发货单收货时间
                    NotifyHkOrderDoneEvent event  = new NotifyHkOrderDoneEvent();
                    event.setShopOrderId(shopOrder.getId());
                    eventBus.post(event);
                }
            }
        }
    }


    protected RichOrder makeParanaOrder(OpenClientShop openClientShop,
                                        OpenClientFullOrder openClientFullOrder) {
        RichOrder richOrder = super.makeParanaOrder(openClientShop, openClientFullOrder);
        //初始化店铺订单的extra
        RichSkusByShop richSkusByShop = richOrder.getRichSkusByShops().get(0);
        Map<String, String> shopOrderExtra = richSkusByShop.getExtra() == null ? Maps.newHashMap() : richSkusByShop.getExtra();
        shopOrderExtra.put(TradeConstants.ECP_ORDER_STATUS, String.valueOf(EcpOrderStatus.WAIT_SHIP.getValue()));
        richSkusByShop.setExtra(shopOrderExtra);

        //初始化店铺子单extra
        List<RichSku> richSkus = richSkusByShop.getRichSkus();
        richSkus.forEach(richSku -> {
            Map<String, String> skuExtra = richSku.getExtra();
            skuExtra.put(TradeConstants.WAIT_HANDLE_NUMBER, String.valueOf(richSku.getQuantity()));
            richSku.setExtra(skuExtra);
        });
        //生成发票信息
        Long invoiceId = this.addInvoice(openClientFullOrder.getInvoice());
        richSkusByShop.setInvoiceId(invoiceId);
        return richOrder;
    }

    private Long addInvoice(OpenClientOrderInvoice openClientOrderInvoice) {
        try {
            //获取发票类型
            Integer invoiceType = Integer.valueOf(openClientOrderInvoice.getType());
            //获取抬头
            String title = openClientOrderInvoice.getTitle();
            //获取detail
            Map<String, String> detail = openClientOrderInvoice.getDetail();
            if (detail != null) {
                if (Objects.equals(invoiceType, 2)) {
                    //公司
                    detail.put("titleType", "2");
                }
            } else {
                detail = Maps.newHashMap();
                detail.put("type", String.valueOf(invoiceType));
            }

            Invoice newInvoice = new Invoice();
            newInvoice.setTitle(title);
            newInvoice.setStatus(1);
            newInvoice.setIsDefault(false);
            newInvoice.setDetail(detail);
            Response<Long> response = invoiceWriteService.createInvoice(newInvoice);
            if (!response.isSuccess()) {
                log.error("create invoice failed,caused by {}", response.getError());
                throw new ServiceException("create.invoice.failed");
            }
            return response.getResult();
        } catch (Exception e) {
            log.error("create invoice failed,caused by {}", e.getMessage());
        }
        return null;
    }

    @Override
    protected void saveParanaOrder(RichOrder richOrder) {
        super.saveParanaOrder(richOrder);

        for (RichSkusByShop richSkusByShop : richOrder.getRichSkusByShops()) {
            //如果是天猫订单，则发请求到端点erp，把收货地址信息同步过来
            if (OpenClientChannel.from(richSkusByShop.getOutFrom()) == OpenClientChannel.TAOBAO) {
                syncReceiverInfo(richSkusByShop);
            }
        }
    }


    @Override
    protected ReceiverInfo toReceiverInfo(OpenClientOrderConsignee consignee) {

        ReceiverInfoHandleResult handleResult = new ReceiverInfoHandleResult();
        handleResult.setSuccess(Boolean.TRUE);
        List<String> errors = Lists.newArrayList();

        ReceiverInfo receiverInfo = new ReceiverInfo();
        receiverInfo.setMobile(consignee.getMobile());
        receiverInfo.setPhone(consignee.getTelephone());
        receiverInfo.setReceiveUserName(consignee.getName());
        receiverInfo.setProvince(consignee.getProvince());
        Long provinceId = queryAddressId(receiverInfo.getProvince());
        if(Arguments.notNull(provinceId)){
            receiverInfo.setProvinceId(Integer.valueOf(provinceId.toString()));
        }else {
            handleResult.setSuccess(Boolean.FALSE);
            errors.add("第三方渠道省："+receiverInfo.getProvince()+"未匹配到中台的省");
        }
        receiverInfo.setCity(consignee.getCity());
        Long cityId = queryAddressId(receiverInfo.getCity());
        if(Arguments.notNull(cityId)){
            receiverInfo.setCityId(Integer.valueOf(cityId.toString()));
        }else {
            handleResult.setSuccess(Boolean.FALSE);
            errors.add("第三方渠道市："+receiverInfo.getProvince()+"未匹配到中台的市");
        }
        receiverInfo.setRegion(consignee.getRegion());
        Long regionId = queryAddressId(receiverInfo.getRegion());
        if(Arguments.notNull(regionId)){
            receiverInfo.setRegionId(Integer.valueOf(regionId.toString()));
        }else {
            handleResult.setSuccess(Boolean.FALSE);
            errors.add("第三方渠道区："+receiverInfo.getProvince()+"未匹配到中台的区");
        }
        handleResult.setErrors(errors);
        receiverInfo.setDetail(consignee.getDetail());
        Map<String,String> extraMap = Maps.newHashMap();
        extraMap.put("handleResult", JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(handleResult));
        receiverInfo.setExtra(extraMap);
        return receiverInfo;
    }


    private Long queryAddressId(String name){
        Optional<WarehouseAddress> wo1 = findWarehouseAddressByName(name);
        if(wo1.isPresent()){
            return wo1.get().getId();
        }

        String splitName = name.substring(0,2);
        Optional<WarehouseAddress> wo2 = findWarehouseAddressByName(splitName);
        if(wo2.isPresent()){
            return wo2.get().getId();
        }

        return null;
    }

    private void syncReceiverInfo(RichSkusByShop richSkusByShop) {
        try {
            erpOpenApiClient.doPost("order.receiver.sync",
                    ImmutableMap.of("shopId", richSkusByShop.getShop().getId(), "orderId", richSkusByShop.getOuterOrderId()));
        } catch (Exception e) {
            log.error("fail to send sync order receiver request to erp for order(outOrderId={},openShopId={}),cause:{}",
                    richSkusByShop.getOuterOrderId(), richSkusByShop.getShop().getId(), Throwables.getStackTraceAsString(e));
        }
    }


    /**
     * 根据名称获取addressId
     * @param addressName 中文名
     * @return 返回地址信息
     */
    private Optional<WarehouseAddress> findWarehouseAddressByName(String addressName){
        Response<Optional<WarehouseAddress>> warehouseResponse = warehouseAddressReadService.findByName(addressName);
        if (!warehouseResponse.isSuccess()){
            log.error("find warehouseAddress failed,addressName is(:{}) error:{}",addressName, warehouseResponse.getError());
            throw new ServiceException("find.warehouse.address.failed");
        }
        return  warehouseResponse.getResult();
    }

}
