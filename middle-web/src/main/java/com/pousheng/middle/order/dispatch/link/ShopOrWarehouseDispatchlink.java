package com.pousheng.middle.order.dispatch.link;

import com.google.common.base.Objects;
import com.google.common.collect.*;
import com.pousheng.middle.gd.GDMapSearchService;
import com.pousheng.middle.gd.Location;
import com.pousheng.middle.order.cache.AddressGpsCacher;
import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.order.dispatch.contants.DispatchContants;
import com.pousheng.middle.order.dispatch.dto.DispatchOrderItemInfo;
import com.pousheng.middle.order.dispatch.dto.DispatchWithPriority;
import com.pousheng.middle.order.dispatch.dto.ShopShipment;
import com.pousheng.middle.order.enums.AddressBusinessType;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.SkuCodeAndQuantity;
import com.pousheng.middle.warehouse.dto.WarehouseShipment;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.utils.Splitters;
import io.terminus.parana.order.model.ReceiverInfo;
import io.terminus.parana.order.model.ShopOrder;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 门店或仓 发货规则（最后一条规则，最复杂场景）
 * 优先级 7
 * 1、最少拆单
 * 2、先仓后端
 * 3、相同拆单情况下距离最短优先
 * 4、组合拆单情况下距离和最短优先
 * Created by songrenfei on 2017/12/23
 */
@Slf4j
public class ShopOrWarehouseDispatchlink implements DispatchOrderLink{

    @Autowired
    private GDMapSearchService gdMapSearchService;
    @Autowired
    private AddressGpsCacher addressGpsCacher;
    @Autowired
    private DispatchComponent dispatchComponent;

    @RpcConsumer
    private WarehouseCacher warehouseCacher;
    @Override
    public boolean dispatch(DispatchOrderItemInfo dispatchOrderItemInfo, ShopOrder shopOrder, ReceiverInfo receiverInfo, List<SkuCodeAndQuantity> skuCodeAndQuantities, Map<String, Serializable> context) throws Exception {

        String address = (String) context.get(DispatchContants.BUYER_ADDRESS);

        //走到这里, 已经没有可以整仓发货的仓库了, 此时尽量按照返回仓库最少数量返回结果
        Multiset<String> current = ConcurrentHashMultiset.create();
        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
            current.add(skuCodeAndQuantity.getSkuCode(), skuCodeAndQuantity.getQuantity());
        }

        List<DispatchWithPriority> dispatchWithPriorities = Lists.newArrayList();


        //全部仓及商品信息
        Table<Long, String, Integer> warehouseSkuCodeQuantityTable = (Table<Long, String, Integer>) context.get(DispatchContants.WAREHOUSE_SKUCODE_QUANTITY_TABLE);

        ListMultimap<Long, String> byWarehouseId = ArrayListMultimap.create();
        //最少拆单中发货件数最多的仓
        for (Long warehouseId : warehouseSkuCodeQuantityTable.rowKeySet()) {
            //本仓库当前可以发货的数量
            /*for (String skuCode : current.elementSet()) {
                int required = current.count(skuCode);
                int stock = warehouseSkuCodeQuantityTable.get(warehouseId, skuCode);
                if(stock>=required){
                    byWarehouseId.put(warehouseId,skuCode);
                }
            }*/

            DispatchWithPriority dispatchWithPriority = new DispatchWithPriority();
            dispatchWithPriority.setWarehouseOrShopId("warehouse:"+warehouseId);
            dispatchWithPriority.setPriority(getDistance(warehouseId,AddressBusinessType.WAREHOUSE,address));
            dispatchWithPriorities.add(dispatchWithPriority);

        }

        List<DispatchWithPriority> warehouseDispatchWithPriority = dispatchComponent.sortDispatchWithPriority(dispatchWithPriorities);

        DispatchWithPriority nearestWarehouse = warehouseDispatchWithPriority.get(0);
        //全部门店及商品信息
        Table<Long, String, Integer> shopSkuCodeQuantityTable = (Table<Long, String, Integer>) context.get(DispatchContants.SHOP_SKUCODE_QUANTITY_TABLE);

        ListMultimap<Long, String> byShopId = ArrayListMultimap.create();
        //最少拆单中发货件数最多的仓
        for (Long shopId : shopSkuCodeQuantityTable.rowKeySet()) {
            //本仓库当前可以发货的数量
            for (String skuCode : current.elementSet()) {
                int required = current.count(skuCode);
                int stock = shopSkuCodeQuantityTable.get(shopId, skuCode);
                if(stock>=required){
                    byShopId.put(shopId,skuCode);
                }
            }
        }



        packageShipmentInfo(dispatchOrderItemInfo,warehouseSkuCodeQuantityTable,skuCodeAndQuantities,dispatchWithPriorities);



        return false;
    }


    private void packageShipmentInfo(DispatchOrderItemInfo dispatchOrderItemInfo,Table<String, String, Integer> widskucode2stock,List<SkuCodeAndQuantity> skuCodeAndQuantities,List<DispatchWithPriority> dispatchWithPriorities){

        //skuCode及数量
        Multiset<String> current = ConcurrentHashMultiset.create();
        for (SkuCodeAndQuantity skuCodeAndQuantity : skuCodeAndQuantities) {
            current.add(skuCodeAndQuantity.getSkuCode(), skuCodeAndQuantity.getQuantity());
        }
        //仓库发货单
        List<WarehouseShipment> warehouseShipmentResult = Lists.newArrayList();
        //店铺发货单
        List<ShopShipment> shopShipmentResult = Lists.newArrayList();

        //把商品派到对应的发货单上
        while (current.size() > 0) {
            //发货仓sku最大数量
            int affordCount = 0;
            String candidateWarehouseOrShopId = null;
            for (DispatchWithPriority dispatchWithPriority : dispatchWithPriorities) {
                String warehouseOrShopId = dispatchWithPriority.getWarehouseOrShopId();
                //发货单下sku数量
                int count = 0;
                for (String skuCode : current.elementSet()) {
                    int required = current.count(skuCode);
                    int stock = widskucode2stock.get(warehouseOrShopId, skuCode);
                    int actual = stock >= required ? 1 : 0;
                    count += actual;
                }
                //sku数量多说明拆单最小
                if (count > affordCount) {
                    affordCount = count; //更新当前仓库的可发货sku数量
                    //距离最近拆单最少的
                    candidateWarehouseOrShopId = warehouseOrShopId;
                }
            }
            if (Strings.isNullOrEmpty(candidateWarehouseOrShopId)) {
                List<SkuCodeAndQuantity> skuAndQuantity = Lists.newArrayList();
                //下边这些商品是没有库存
                for (String skuCode : current.elementSet()) {
                    log.warn("insufficient sku(skuCode={}) stock: ", skuCode);
                    SkuCodeAndQuantity adq = new SkuCodeAndQuantity();
                    adq.setSkuCode(skuCode);
                    adq.setQuantity(current.count(skuAndQuantity));
                    skuAndQuantity.add(adq);
                }
                dispatchOrderItemInfo.setSkuCodeAndQuantities(skuAndQuantity);
            } else {//分配发货仓库
                List<String> typeAndId = Splitters.COLON.splitToList(candidateWarehouseOrShopId);
                String type = typeAndId.get(0);
                Long id = Long.valueOf(typeAndId.get(1));

                List<SkuCodeAndQuantity> scaqs = Lists.newArrayList();
                for (String skuCode : current.elementSet()) {
                    int required = current.count(skuCode);
                    int stock = widskucode2stock.get(candidateWarehouseOrShopId, skuCode);
                    int actual = stock >= required ? required : 0;

                    SkuCodeAndQuantity scaq = new SkuCodeAndQuantity();
                    scaq.setSkuCode(skuCode);
                    scaq.setQuantity(actual);
                    if (actual!=0){
                        scaqs.add(scaq);
                    }

                    //减少库存需求
                    current.remove(skuCode, actual);
                }
                if(Objects.equal(type,"warehouse")){
                    WarehouseShipment warehouseShipment = new WarehouseShipment();
                    warehouseShipment.setWarehouseId(id);
                    warehouseShipment.setWarehouseName(warehouseCacher.findById(id).getName());
                    warehouseShipment.setSkuCodeAndQuantities(scaqs);
                    warehouseShipmentResult.add(warehouseShipment);

                }else {
                    ShopShipment shopShipment = new ShopShipment();
                    shopShipment.setShopId(id);
                    shopShipment.setShopName("");//todo
                    shopShipment.setSkuCodeAndQuantities(scaqs);
                    shopShipmentResult.add(shopShipment);

                }
            }

            dispatchOrderItemInfo.setWarehouseShipments(warehouseShipmentResult);
            dispatchOrderItemInfo.setShopShipments(shopShipmentResult);
        }

    }


    /**
     * 获取距离用户收货地址到门店或仓的距离
     * @param businessId 门店或仓id
     * @param addressBusinessType {@link AddressBusinessType}
     * @param address 用户收货地址
     * @return 距离
     */
    public double getDistance(Long businessId,AddressBusinessType addressBusinessType, String address){

        //1、调用高德地图查询地址坐标
        Location location = dispatchComponent.getLocation(address);
        AddressGps addressGps = addressGpsCacher.findByBusinessIdAndType(businessId, addressBusinessType.getValue());
        return dispatchComponent.getDistance(addressGps,location.getLon(),location.getLat()).getDistance();
    }
}
