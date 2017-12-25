package com.pousheng.middle.order.dispatch.link;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.pousheng.middle.hksyc.component.QueryHkWarhouseOrShopStockApi;
import com.pousheng.middle.hksyc.dto.item.HkSkuStockInfo;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.order.enums.AddressBusinessType;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.order.service.AddressGpsReadService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全国仓发货规则
 * 优先级 4
 * 1、调用roger的接口查询全国仓及仓下对应商品的库存
 * 2、过滤掉电商在售仓和省内仓，过滤后如果没有可用的范围则进入下个规则
 * 3、判断是否有整单发货的仓，如果没有则进入下个规则，如果有则判断个数，如果只有一个则该仓发货，如果有多个则需要根据用户收货地址找出距离用户最近的一个仓
 * Created by songrenfei on 2017/12/22
 */
@Slf4j
public class AllWarehouseDispatchLink implements DispatchOrderLink{

    @Autowired
    private AddressGpsReadService addressGpsReadService;
    @Autowired
    private QueryHkWarhouseOrShopStockApi queryHkWarhouseOrShopStockApi;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private WarehouseReadService warehouseReadService;

    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {

        return true;
    }





}
