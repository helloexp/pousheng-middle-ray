package com.pousheng.middle.web.warehouses.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.pousheng.erp.component.ErpClient;
import com.pousheng.erp.model.StockBill;
import com.pousheng.erp.service.StockBillService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseSkuWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 单据接口
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-28
 */
@Component
@Slf4j
public class StockBillImporter {

    private static final int PAGE_SIZE = 200;
    private static final TypeReference<List<StockBill>> LIST_OF_STOCKBILL = new TypeReference<List<StockBill>>() {
    };

    @RpcConsumer
    private WarehouseCacher warehouseCacher;

    @RpcConsumer
    private WarehouseSkuWriteService warehouseSkuWriteService;

    @RpcConsumer
    private StockBillService stockBillService;

    @RpcConsumer
    private ErpClient erpClient;

    private final ImmutableSet<String> inputs = ImmutableSet.<String>builder()
            .add("e-commerce-api/v1/hk-move-in")
            .add("e-commerce-api/v1/hk-cgrk")
            .add("e-commerce-api/v1/hk-thrk")
            .add("e-commerce-api/v1/hk-qtrk")
            .build();

    private final ImmutableSet<String> outputs = ImmutableSet.<String>builder()
            .add("e-commerce-api/v1/hk-move-out")
            .add("e-commerce-api/v1/hk-move-req")
            .add("e-commerce-api/v1/hk-ssck")
            .add("e-commerce-api/v1/hk-yzck")
            .add("e-commerce-api/v1/hk-bsck")
            .add("e-commerce-api/v1/hk-qtck")
            .add("e-commerce-api/v1/hk-thck")
            .add("e-commerce-api/v1/hk-lock-bsck")
            .add("e-commerce-api/v1/hk-lock-qtck")
            .build();

    public int process(Date start, Date end) {
        if (start == null) {
            log.error("no start date specified when import stockBills");
            throw new IllegalArgumentException("start.date.miss");
        }

        //暂存计算结果
        Table<String, Long, Integer> deltas = HashBasedTable.create(); //skuCode, warehouseId => delta
        int handleCount = 0;
        for (String input : inputs) {
            handleCount +=doProcess(input, start, end, deltas);

        }

        for (String output : outputs) {
            handleCount+=doProcess(output, start, end, deltas);
        }


        //更新库存

        return handleCount;
    }

    public int doProcess(String path, Date start, Date end, Table<String, Long, Integer> deltas) {
        int handleCount = 0;
        try {
            int pageNo = 1;
            boolean hasNext = true;
            while (hasNext) {
                String r = erpClient.get(path,  start, end, pageNo, PAGE_SIZE, Maps.newHashMap());
                List<StockBill> stockBills = JsonMapper.nonEmptyMapper().getMapper().readValue(r, LIST_OF_STOCKBILL);
                pageNo = pageNo + 1;
                hasNext = Objects.equal(stockBills.size(), PAGE_SIZE);
                for (StockBill stockBill : stockBills) {
                    processSingle(stockBill, deltas);
                    handleCount++;
                }
            }

        } catch (Exception e) {
            log.error("failed to handle stock bill from {} to {}, cause:{}",
                    start, end, Throwables.getStackTraceAsString(e));
        }
        return handleCount;
    }

    public void processSingle(StockBill stockBill, Table<String, Long, Integer> deltas) {
        try {
            //Warehouse warehouse =  warehouseCacher.findByCode(stockBill.getCompany_id()+"-"+stockBill.getStock_id());

            System.out.println(stockBill);

        } catch (Exception e) {
            log.error("failed to process {}, cause:{}",
                    stockBill, Throwables.getStackTraceAsString(e));
        }
    }

}
