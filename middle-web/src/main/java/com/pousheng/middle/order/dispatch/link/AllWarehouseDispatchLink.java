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

        //电商在售可用仓，从上个规则传递过来。 // TODO: 2017/12/23  解析context map
        List<Long> onlineSaleWarehouseIds = Lists.newArrayList();
        //省内的mpos仓,如果没有则进入下个规则
        List<AddressGps> addressGpses = findWarehouseAddressGps(Long.valueOf(receiverInfo.getProvinceId()));
        if(CollectionUtils.isEmpty(addressGpses)){
            return Boolean.TRUE;
        }

        //过滤掉上个规则匹配到的电商在售仓,过滤后如果没有可用的范围则进入下个规则
        List<AddressGps> rangeInnerAddressGps = addressGpses.stream().filter(addressGps -> !onlineSaleWarehouseIds.contains(addressGps.getBusinessId())).collect(Collectors.toList());
        if(CollectionUtils.isEmpty(rangeInnerAddressGps)){
            return Boolean.TRUE;
        }

        if(CollectionUtils.isEmpty(rangeInnerAddressGps)){
            return Boolean.TRUE;
        }

        List<Long> warehouseIds = Lists.transform(rangeInnerAddressGps, new Function<AddressGps, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable AddressGps input) {
                return input.getBusinessId();
            }
        });

        List<Warehouse> warehouses = findWarehouseByIds(warehouseIds);

        //查询仓代码
        List<String> stockCodes = Lists.transform(warehouses, new Function<Warehouse, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Warehouse input) {
                return input.getCode();//todo 需要确认什么code
            }
        });

        List<String> skuCodes = Lists.transform(skuCodeAndQuantities, new Function<SkuCodeAndQuantity, String>() {
            @Nullable
            @Override
            public String apply(@Nullable SkuCodeAndQuantity input) {
                return input.getSkuCode();
            }
        });

        List<HkSkuStockInfo> skuStockInfos = queryHkWarhouseOrShopStockApi.doQueryStockInfo(stockCodes,skuCodes,2);




        return true;
    }


    private List<AddressGps> findWarehouseAddressGps(Long provinceId){

        Response<List<AddressGps>> addressGpsListRes = addressGpsReadService.findByProvinceIdAndBusinessType(provinceId, AddressBusinessType.WAREHOUSE);
        if(!addressGpsListRes.isSuccess()){
            log.error("find addressGps by province id :{} for warehouse failed,  error:{}", provinceId,addressGpsListRes.getError());
            throw new ServiceException(addressGpsListRes.getError());
        }
        return addressGpsListRes.getResult();

    }

    private List<Warehouse> findWarehouseByIds(List<Long> ids){

        Response<List<Warehouse>> warehouseListRes = warehouseReadService.findByIds(ids);
        if(!warehouseListRes.isSuccess()){
            log.error("find warehouse by ids:{} failed,  error:{}", ids,warehouseListRes.getError());
            throw new ServiceException(warehouseListRes.getError());
        }
        return warehouseListRes.getResult();

    }


}
