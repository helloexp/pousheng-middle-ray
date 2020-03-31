package com.pousheng.middle.web.warehouses.component;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.pousheng.erp.component.WarehouseFetcher;
import com.pousheng.erp.model.PoushengWarehouse;
import com.pousheng.middle.gd.Location;
import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.order.enums.AddressBusinessType;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.order.service.AddressGpsReadService;
import com.pousheng.middle.order.service.AddressGpsWriteService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.Splitters;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 仓库导入
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-28
 */
@Component
@Slf4j
public class WarehouseImporter {

    @RpcConsumer
    private WarehouseFetcher warehouseFetcher;
    @Autowired
    private DispatchComponent dispatchComponent;
    @Autowired
    private WarehouseAddressTransverter warehouseAddressTransverter;
    @Autowired
    private AddressGpsWriteService addressGpsWriteService;
    @Autowired
    private AddressGpsReadService addressGpsReadService;
    @Autowired
    private WarehouseCacher warehouseCacher;



    /**
     * 增量导入
     *
     * @return 处理的条数
     */
    public int process(Date start, Date end) {
        if (start == null) {
            log.error("no start date specified when import warehouse");
            throw new IllegalArgumentException("start.date.miss");
        }
        int handleCount = 0;
        int pageNo = 1;
        boolean hasNext = true;
        while (hasNext) {
            List<PoushengWarehouse> warehouses = warehouseFetcher.fetch(pageNo, 20, start, end);
            pageNo = pageNo + 1;
            hasNext = Objects.equal(warehouses.size(), 20);
            for (PoushengWarehouse warehouse : warehouses) {
                doProcess(warehouse);
                handleCount++;
            }
        }
        return handleCount;
    }


    private void doProcess(PoushengWarehouse warehouse) {

        //如果仓库的地址信息缺失则直接跳过
        if (Strings.isNullOrEmpty(warehouse.getArea_full_name())){
            log.error("pousheng warehouse:{} address info invalid ,so skip",warehouse.getArea_full_name());
            return;
        }

        String warehosueCode = warehouse.getCompany_id()+'-'+ warehouse.getStock_id();
        try {

            WarehouseDTO warehouseDTO = warehouseCacher.findByCode(warehosueCode);

            //检查是否已同步过这个仓库
            if (Arguments.notNull(warehouseDTO)){ //已同步过, 则更新
                handAddress(warehouse,warehouseDTO.getId(),Boolean.TRUE);
            } else {
                log.warn("not find warehouse code :{} so skip create gps",warehosueCode);
            }

        } catch (Exception e){
            log.error("create address gps fail for warehouse code :{} so skip create gps ,cause:{}",warehosueCode, Throwables.getStackTraceAsString(e));

        }

    }


    private void handAddress(PoushengWarehouse pw,Long warehouseId,Boolean isUpdate){
        List<String> addressList = Splitters.DOT.splitToList(pw.getArea_full_name());
        if (addressList.size() != 3){
            log.error("warehouse(id:{}) address area full name :{} invalid,so skip",warehouseId,pw.getArea_full_name());
            return;
        }
        String province = addressList.get(0);
        String city = addressList.get(1);
        String region = addressList.get(2);
        String address = province + city + region;
        if(!Strings.isNullOrEmpty(pw.getStock_address())){
            address = pw.getStock_address();
        }
        //2、调用高德地图查询地址坐标
        Optional<Location> locationOp = dispatchComponent.getLocation(address);
        if(!locationOp.isPresent()){
            log.error("[ADDRESS-LOCATION]:not find warehouse(id:{}) location by address:{}",warehouseId,address);
            return;
        }
        Location location = locationOp.get();
        //3、创建门店地址定位信息
        AddressGps addressGps = new AddressGps();
        addressGps.setLatitude(location.getLat());
        addressGps.setLongitude(location.getLon());
        addressGps.setBusinessId(warehouseId);
        addressGps.setBusinessType(AddressBusinessType.WAREHOUSE.getValue());
        addressGps.setDetail(address);
        addressGps.setProvince(province);
        addressGps.setCity(city);
        addressGps.setRegion(region);
        warehouseAddressTransverter.complete(addressGps);

        if (isUpdate){
            Response<Optional<AddressGps>> addressGpsRes = addressGpsReadService.findByBusinessIdAndType(warehouseId,AddressBusinessType.WAREHOUSE);
            if(!addressGpsRes.isSuccess()){
                log.error("find address gps by warehouse id:{} fail,error:{}",warehouseId,addressGpsRes.getError());
                return;
            }

            if(!addressGpsRes.getResult().isPresent()){
                Response<Long> response = addressGpsWriteService.create(addressGps);
                if(!response.isSuccess()){
                    log.error("create address gps for old data, warehouse id:{} fail,error:{}",warehouseId,response.getError());
                }
                return;
            }

            AddressGps existAddressGps = addressGpsRes.getResult().get();
            addressGps.setId(existAddressGps.getId());
            Response<Boolean> response = addressGpsWriteService.update(addressGps);
            if(!response.isSuccess()){
                log.error("update address gps for warehouse id:{} fail,error:{}",warehouseId,response.getError());
            }
        }else {
            Response<Long> response = addressGpsWriteService.create(addressGps);
            if(!response.isSuccess()){
                log.error("create address gps for warehouse id:{} fail,error:{}",warehouseId,response.getError());
            }
        }
    }
}
