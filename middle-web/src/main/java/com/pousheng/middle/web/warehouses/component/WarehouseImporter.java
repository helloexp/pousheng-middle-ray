package com.pousheng.middle.web.warehouses.component;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.pousheng.erp.component.WarehouseFetcher;
import com.pousheng.erp.model.PoushengWarehouse;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.service.WarehouseReadService;
import com.pousheng.middle.warehouse.service.WarehouseWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Map;

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

    @RpcConsumer
    private WarehouseWriteService warehouseWriteService;

    @RpcConsumer
    private WarehouseReadService warehouseReadService;

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
        String companyId = warehouse.getCompany_id();
        String stockId = warehouse.getStock_id();
        Warehouse w = fromPoushengWarehouse(warehouse);
        //检查是否已同步过这个仓库
        String code = companyId + "-" + stockId;
        Response<Optional<Warehouse>> r = warehouseReadService.findByCode(code);
        if(!r.isSuccess()){
            log.error("failed to find warehouse(code={}),error code:{}, so skip {}", code, r.getError(), warehouse);
            return;
        }
        if(r.getResult().isPresent()){ //已同步过, 则更新
            Warehouse exist = r.getResult().get();
            w.setId(exist.getId());
            Response<Boolean> ru = warehouseWriteService.update(w);
            if(!ru.isSuccess()){
                log.error("failed to update {}, error code:{}, so skip {}", w, r.getError(), warehouse);
            }
        }else{ //未同步过, 则新建
            w.setStatus(1);
            Map<String,String> extra=w.getExtra()==null?Maps.newHashMap():w.getExtra();
            extra.put("isNew","true");
            w.setExtra(extra);
            Response<Long> rc = warehouseWriteService.create(w);
            if(!rc.isSuccess()){
                log.error("failed to create {}, error code:{}, so skip {}", w, r.getError(), warehouse);
            }
        }
    }

    private Warehouse fromPoushengWarehouse(PoushengWarehouse pw) {
        Warehouse w = new Warehouse();
        w.setName(pw.getStock_name());
        w.setCode(pw.getCompany_id()+'-'+pw.getStock_id());
        w.setAddress(pw.getStock_address());
        w.setType(pw.getStock_type());
        w.setCompanyId(pw.getCompany_id());
        w.setCompanyName(pw.getCompany_name());
        Map<String, String>  extra = Maps.newHashMap();
        extra.put("outCode", pw.getStock_code());
        extra.put("telephone", pw.getTelphone());
        w.setExtra(extra);
        return w;
    }

    public static void main(String[] args) {
        Map<String,String> map=Maps.newHashMap();
        System.out.print(map.remove("aaa"));
    }
}
