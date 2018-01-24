package com.pousheng.middle.order.dispatch.link;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.order.dispatch.component.ShopAddressComponent;
import com.pousheng.middle.order.dispatch.contants.DispatchContants;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.warehouse.dto.ShopShipment;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import io.terminus.common.exception.ServiceException;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.shop.model.Shop;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 省内门店发货规则
 * 优先级 5
 * 1、查找用户收货地址所在的省
 * 2、查找省内的打了mpos标签的门店（过滤掉已经拒过单的门店），如果没有找到则进入下个规则
 * 3、调用roger的接口查询各个门店及门店下对应商品的库存
 * 4、判断是否有整单发货的门店，如果没有则进入下个规则（记录下省内的门店），如果有则判断个数，如果只有一个则该门店发货，如果有多个则需要根据用户收货地址找出距离用户最近的一个门店
 * Created by songrenfei on 2017/12/23
 */
@Component
@Slf4j
public class ProvinceInnerShopDispatchlink implements DispatchOrderLink{
    @Autowired
    private ShopAddressComponent shopAddressComponent;
    @Autowired
    private DispatchComponent dispatchComponent;
    @Autowired
    private QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;

    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {
        log.info("DISPATCH-ProvinceInnerShopDispatchlink-5 start...");

        //拒绝过的门店
        List<Long> rejectShopIds = dispatchComponent.findRejectedShop(shopOrder.getId());
        context.put(DispatchContants.REJECT_SHOP_IDS, (Serializable) rejectShopIds);

        //省内的mpos门店,如果没有则进入下个规则
        //todo 冻结或删除的门店需要过滤掉
        List<AddressGps> addressGpses = shopAddressComponent.findShopAddressGps(Long.valueOf(receiverInfo.getProvinceId()));
        if(CollectionUtils.isEmpty(addressGpses)){
            return Boolean.TRUE;
        }


        //过滤掉该订单下已拒绝过的门店,过滤后如果没有可用的范围则进入下个规则
        List<AddressGps> rangeInnerAddressGps = addressGpses.stream().filter(addressGps -> !rejectShopIds.contains(addressGps.getBusinessId())).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(rangeInnerAddressGps)){
            return Boolean.TRUE;
        }

        List<Long> shopIds = Lists.transform(rangeInnerAddressGps, new Function<AddressGps, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable AddressGps input) {
                return input.getBusinessId();
            }
        });

        List<Shop> shops = shopAddressComponent.findShopByIds(shopIds);

        //过滤掉省内已删除或已冻结的门店
        List<Shop> validShops = shops.stream().filter(shop -> Objects.equal(shop.getStatus(),1)).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(validShops)){
            return Boolean.TRUE;
        }
        //查询仓代码
        List<String> stockCodes = Lists.transform(validShops, new Function<Shop, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Shop input) {
                if(Strings.isNullOrEmpty(input.getOuterId())){
                    log.error("shop(id:{}) outer id invalid",input.getId());
                    throw new ServiceException("shop.outer.id.invalid");
                }
                return input.getOuterId();
            }
        });

        List<String> skuCodes = dispatchComponent.getSkuCodes(skuCodeAndQuantities);

        List<HkSkuStockInfo> skuStockInfos = queryHkWarhouseOrShopStockApi.doQueryStockInfo(stockCodes,skuCodes,1);
        if(CollectionUtils.isEmpty(skuStockInfos)){
            return Boolean.TRUE;
        }
        Table<Long, String, Integer> shopSkuCodeQuantityTable = HashBasedTable.create();
        dispatchComponent.completeShopTab(skuStockInfos,shopSkuCodeQuantityTable);
        context.put(DispatchContants.SHOP_SKUCODE_QUANTITY_TABLE, (Serializable) shopSkuCodeQuantityTable);
        //判断是否有整单
        List<ShopShipment> shopShipments = dispatchComponent.chooseSingleShop(skuStockInfos,shopSkuCodeQuantityTable,skuCodeAndQuantities);

        //没有整单发的
        if(CollectionUtils.isEmpty(shopShipments)){
            return Boolean.TRUE;
        }

        //如果只有一个
        if(Objects.equal(shopShipments.size(),1)){
            dispatchOrderItemInfo.setShopShipments(shopShipments);
            return Boolean.FALSE;
        }

        String address = (String) context.get(DispatchContants.BUYER_ADDRESS);
        String addressRegion = (String) context.get(DispatchContants.BUYER_ADDRESS_REGION);
        //如果有多个要选择最近的
        ShopShipment shopShipment = shopAddressComponent.nearestShop(shopShipments,address,addressRegion);
        dispatchOrderItemInfo.setShopShipments(Lists.newArrayList(shopShipment));

        return Boolean.FALSE;
    }
}
