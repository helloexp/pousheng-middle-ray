package com.pousheng.middle.order.dispatch.link;

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
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 全国门店发货规则
 * 优先级 6
 * 1、调用roger的接口查询全国门店及门店下对应商品的库存
 * 2、过滤掉省内门店和已经拒过单的门店，过滤后如果没有可用的范围则进入下个规则
 * 3、判断是否有整单发货的门店，如果没有则进入下个规则，如果有则判断个数，如果只有一个则该门店发货，如果有多个则需要根据用户收货地址找出距离用户最近的一个门店
 * Created by songrenfei on 2017/12/23
 */
@Component
@Slf4j
public class AllShopDispatchlink implements DispatchOrderLink{

    @Autowired
    private ShopAddressComponent shopAddressComponent;
    @Autowired
    private DispatchComponent dispatchComponent;
    @Autowired
    private QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;

    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {
        log.info("DISPATCH-AllShopDispatchlink-6  order(id:{}) start...",shopOrder.getId());

        //拒绝过发货单的mpos门店
        List<Long> rejectShopIds = (List<Long>) context.get(DispatchContants.REJECT_SHOP_IDS);
        if(CollectionUtils.isEmpty(rejectShopIds)){
            rejectShopIds = Lists.newArrayList();
        }

        List<String> skuCodes = dispatchComponent.getSkuCodes(skuCodeAndQuantities);

        List<HkSkuStockInfo> skuStockInfos = queryHkWarhouseOrShopStockApi.doQueryStockInfo(null,skuCodes,1);
        if(CollectionUtils.isEmpty(skuStockInfos)){
            return Boolean.TRUE;
        }

        Table<Long, String, Integer> shopSkuCodeQuantityTable = (Table<Long, String, Integer>) context.get(DispatchContants.SHOP_SKUCODE_QUANTITY_TABLE);
        if(Arguments.isNull(shopSkuCodeQuantityTable)){
            shopSkuCodeQuantityTable = HashBasedTable.create();
            context.put(DispatchContants.SHOP_SKUCODE_QUANTITY_TABLE, (Serializable) shopSkuCodeQuantityTable);
        }

        List<HkSkuStockInfo> filterSkuStockInfos = filterAlreadyQuery(skuStockInfos,shopSkuCodeQuantityTable,rejectShopIds);
        if(CollectionUtils.isEmpty(filterSkuStockInfos)){
            return Boolean.TRUE;
        }

        //放入 shopSkuCodeQuantityTable
        dispatchComponent.completeShopTab(filterSkuStockInfos,shopSkuCodeQuantityTable);

        //判断是否有整单
        List<ShopShipment> shopShipments = dispatchComponent.chooseSingleShop(filterSkuStockInfos,shopSkuCodeQuantityTable,skuCodeAndQuantities);

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
        if(Arguments.isNull(shopShipment)){
            log.error("not find nearest shop by:{} fail,",shopShipments);
            throw new ServiceException("find.nearest.shop.fail");
        }
        dispatchOrderItemInfo.setShopShipments(Lists.newArrayList(shopShipment));

        return Boolean.FALSE;
    }


    //过滤掉省内和已拒绝的门店
    private List<HkSkuStockInfo> filterAlreadyQuery(List<HkSkuStockInfo> hkSkuStockInfos,Table<Long, String, Integer> shopSkuCodeQuantityTable,List<Long> rejectShopIds){
        Set<Long> alreadyQueryShopIds =  shopSkuCodeQuantityTable.rowKeySet();
        //过滤掉省内
        List<HkSkuStockInfo> hkSkuStockInfoList = hkSkuStockInfos.stream().filter(hkSkuStockInfo -> !alreadyQueryShopIds.contains(hkSkuStockInfo.getBusinessId())).collect(Collectors.toList());
        //过滤掉已拒绝的门店
        return hkSkuStockInfoList.stream().filter(hkSkuStockInfo -> !rejectShopIds.contains(hkSkuStockInfo.getBusinessId())).collect(Collectors.toList());

    }
}
