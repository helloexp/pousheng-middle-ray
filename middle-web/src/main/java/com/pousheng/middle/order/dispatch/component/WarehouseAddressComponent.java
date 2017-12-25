package com.pousheng.middle.order.dispatch.component;

import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.pousheng.middle.order.enums.AddressBusinessType;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.order.service.AddressGpsReadService;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.model.WarehouseSkuStock;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Created by songrenfei on 2017/12/25
 */
@Component
@Slf4j
public class WarehouseAddressComponent {


    @Autowired
    private AddressGpsReadService addressGpsReadService;
    @Autowired
    private WarehouseReadService warehouseReadService;



    public List<AddressGps> findWarehouseAddressGps(Long provinceId){

        Response<List<AddressGps>> addressGpsListRes = addressGpsReadService.findByProvinceIdAndBusinessType(provinceId, AddressBusinessType.WAREHOUSE);
        if(!addressGpsListRes.isSuccess()){
            log.error("find addressGps by province id :{} for warehouse failed,  error:{}", provinceId,addressGpsListRes.getError());
            throw new ServiceException(addressGpsListRes.getError());
        }
        return addressGpsListRes.getResult();

    }

    public List<Warehouse> findWarehouseByIds(List<Long> ids){

        Response<List<Warehouse>> warehouseListRes = warehouseReadService.findByIds(ids);
        if(!warehouseListRes.isSuccess()){
            log.error("find warehouse by ids:{} failed,  error:{}", ids,warehouseListRes.getError());
            throw new ServiceException(warehouseListRes.getError());
        }
        return warehouseListRes.getResult();

    }



    /*public List<WarehouseShipment> trySingleWarehouse(List<SkuCodeAndQuantity> skuCodeAndQuantities,
                                                                 Long warehouseId) {
        boolean enough = true;
        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
            String skuCode = skuCodeAndQuantity.getSkuCode();
            Response<WarehouseSkuStock> rStock = warehouseSkuReadService.findByWarehouseIdAndSkuCode(warehouseId, skuCode);
            if (!rStock.isSuccess()) {
                log.error("failed to find sku(skuCode={}) in warehouse(id={}), error code:{}",
                        skuCode, warehouseId, rStock.getError());
                throw new ServiceException(rStock.getError());
            }
            int stock = rStock.getResult().getAvailStock().intValue();
            widskucode2stock.put(warehouseId, skuCode, stock);
            if (stock < skuCodeAndQuantity.getQuantity()) {
                enough = false;
            }
        }
        if (enough) {
            WarehouseShipment warehouseShipment = new WarehouseShipment();
            warehouseShipment.setWarehouseId(warehouseId);
            Warehouse warehouse = warehouseCacher.findById(warehouseId);
            warehouseShipment.setWarehouseName(warehouse.getName());
            warehouseShipment.setSkuCodeAndQuantities(skuCodeAndQuantities);
            return Lists.newArrayList(warehouseShipment);
        }
        return Collections.emptyList();
    }*/
}
