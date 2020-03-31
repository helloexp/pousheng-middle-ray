package com.pousheng.middle.web.order;

import com.google.common.collect.Lists;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.dto.InventoryTradeDTO;
import com.pousheng.middle.warehouse.model.SkuInventory;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * 销售发货  和 换货发货 合并api
 * Created by songrenfei on 2017/7/6
 */
@RestController
@Slf4j
public class InventoryApiTests {

    @Autowired
    private InventoryClient inventoryClient;

    static final Integer BATCH_SIZE = 200;     // 批处理数量

    /**
     * 测试占库存
     */
    @RequestMapping(value = "/api/inventory/lock/stock", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String lock(@RequestParam String shipmentId,@RequestParam(defaultValue = "50") Integer batchSize, @RequestParam(defaultValue = "宝胜北京直营仓") String warehouseName) {
        if (log.isDebugEnabled()){
            log.debug("TEST-LOCK-STOCK-START param: shipmentId [{}] warehouseName [{}]",shipmentId,warehouseName);
        }


        List<SkuInventory> skuInventories = Lists.newArrayList();

        int pageNo = 1;
        boolean next = batchHandle(pageNo, BATCH_SIZE,batchSize,warehouseName,skuInventories);
        while (next) {
            pageNo ++;
            next = batchHandle(pageNo, BATCH_SIZE,batchSize,warehouseName,skuInventories);
        }


        List<InventoryTradeDTO> tradeList = Lists.newArrayListWithCapacity(skuInventories.size());

        for (SkuInventory skuInventory : skuInventories){

            String skuCode = skuInventory.getSkuCode();
            Integer quantity = 1;

            InventoryTradeDTO currTrade = new InventoryTradeDTO();
            currTrade.setWarehouseId(skuInventory.getWarehouseId());
            currTrade.setQuantity(quantity);
            currTrade.setSkuCode(skuCode);
            currTrade.setBizSrcId(shipmentId);
            currTrade.setSubBizSrcId(Lists.newArrayList(currTrade.getBizSrcId()));
            currTrade.setShopId(1L);
            //currTrade.setUniqueCode(inventoryTradeDTO.getUniqueCode());

            tradeList.add(currTrade);

        }

        log.debug("TEST-LOCK-STOCK-CALL-INVENTORY-API-START param: shipmentId [{}] warehouseName [{}]",shipmentId,warehouseName);
        Response<Boolean> tradeRet = inventoryClient.lock(tradeList);
        if (!tradeRet.isSuccess() || !tradeRet.getResult()) {
            log.error("TEST-LOCK-STOCK-FAIL error:{}", tradeRet.getError());
            return tradeRet.getError();
        }



        if (log.isDebugEnabled()){
            log.debug("TEST-LOCK-STOCK-END param: shipmentId [{}] warehouseName [{}]",shipmentId,warehouseName);
        }
        return "SUCCESS";

    }


    @SuppressWarnings("unchecked")
    private boolean batchHandle(int pageNo, int size,Integer batchSize,String warehouseName,List<SkuInventory> skuInventories) {


        Paging<SkuInventory> retPage = inventoryClient.inventoryPaging(pageNo, size, null, warehouseName, null);
        List<SkuInventory> list = retPage.getData();

        if (retPage.getTotal().equals(0L)  || CollectionUtils.isEmpty(list)) {
            return Boolean.FALSE;
        }
        skuInventories.addAll(list);

        if( skuInventories.size()> batchSize){
            return Boolean.FALSE;
        }

        int current = list.size();
        return current == size;  // 判断是否存在下一个要处理的批次
    }



}
