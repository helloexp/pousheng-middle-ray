package com.pousheng.middle.web.warehouses;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.enums.WarehouseType;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.model.WarehouseRule;
import com.pousheng.middle.warehouse.model.WarehouseRuleItem;
import com.pousheng.middle.warehouse.model.WarehouseShopGroup;
import com.pousheng.middle.warehouse.service.WarehouseRuleItemReadService;
import com.pousheng.middle.warehouse.service.WarehouseRuleItemWriteService;
import com.pousheng.middle.warehouse.service.WarehouseRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseShopGroupReadService;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.utils.operationlog.OperationLogParam;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import com.pousheng.middle.web.warehouses.component.WarehouseRuleComponent;
import com.pousheng.middle.web.warehouses.dto.WarehouseRuleItemDto;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.BeanMapper;
import io.terminus.open.client.common.shop.model.OpenShop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-20
 */
@RestController
@RequestMapping("/api/warehouse/rule/{ruleId}/rule-item")
@Slf4j
@OperationLogModule(OperationLogModule.Module.WAREHOUSE_RULE_ITEM)
public class WarehouseRuleItems {

    @RpcConsumer
    private WarehouseRuleItemReadService warehouseRuleItemReadService;

    @RpcConsumer
    private WarehouseShopGroupReadService warehouseShopGroupReadService;

    @RpcConsumer
    private WarehouseRuleReadService warehouseRuleReadService;

    @RpcConsumer
    private WarehouseRuleItemWriteService warehouseRuleItemWriteService;

    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private WarehouseRuleComponent warehouseRuleComponent;
    @Autowired
    private OrderReadLogic orderReadLogic;

    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<WarehouseRuleItemDto> findByRuleId(@PathVariable Long ruleId){
        Response<List<WarehouseRuleItem>> r = warehouseRuleItemReadService.findByRuleId(ruleId);
        if(!r.isSuccess()){
            log.error("failed to find warehouse rule items for rule(id={}), error code:{}", ruleId, r.getError());
            throw new JsonResponseException(r.getError());
        }
        List<WarehouseRuleItem> ruleItems =  r.getResult();
        List<WarehouseRuleItemDto> result = Lists.newArrayListWithCapacity(ruleItems.size());
        for (WarehouseRuleItem ruleItem : ruleItems) {
            Long warehouseId = ruleItem.getWarehouseId();
            Warehouse warehouse = warehouseCacher.findById(warehouseId);
            WarehouseRuleItemDto ruleItemDto = new WarehouseRuleItemDto();
            BeanMapper.copy(ruleItem, ruleItemDto);
            ruleItemDto.setCompanyCode(warehouse.getCompanyCode());
            if (warehouse.getExtra()==null)
            {
                ruleItemDto.setOutCode("");
            }else{
                ruleItemDto.setOutCode(warehouse.getExtra().get("outCode")==null?"":warehouse.getExtra().get("outCode"));
            }
            result.add(ruleItemDto);
        }
        return result;
    }

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("批量创建")
    public Boolean save(@PathVariable @OperationLogParam Long ruleId, @RequestBody WarehouseRuleItem[] warehouseRuleItems){
        ArrayList<WarehouseRuleItem> ruleItemArrayList = Lists.newArrayList(warehouseRuleItems);
        //判断店仓
        checkAllChannel(ruleId,ruleItemArrayList);
        Response<Boolean> r = warehouseRuleItemWriteService.batchCreate(ruleId, ruleItemArrayList);
        if(!r.isSuccess()){
            log.error("failed to save rule(id={})'s warehouseRuleItems:{}, error code:{}",
                    ruleId, warehouseRuleItems,r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }


    private void checkAllChannel(Long ruleId,ArrayList<WarehouseRuleItem> ruleItemArrayList){
        List<WarehouseShopGroup> shopGroups = warehouseRuleComponent.findWarehouseShopGropsByRuleId(ruleId);
        WarehouseShopGroup warehouseShopGroup = shopGroups.get(0);
        Boolean isAllChannelShop = orderReadLogic.isAllChannelOpenShop(warehouseShopGroup.getShopId());
        //是全渠道店铺直接返回，不用判断选择的仓范围是否包含店仓
        if(isAllChannelShop){
            return;
        }
        //非全渠道则要判断仓范围中不能含有店仓
        for (WarehouseRuleItem warehouseRuleItem : ruleItemArrayList){
            Warehouse warehouse = warehouseCacher.findById(warehouseRuleItem.getWarehouseId());
            if(Objects.equal(WarehouseType.from(warehouse.getType()),WarehouseType.SHOP_WAREHOUSE)){
                log.error("warehouse(id:{}) is shop warehouse so can not add rule",warehouseRuleItem.getWarehouseId());
                throw new JsonResponseException("can.not.contain.shop.warehouse");
            }

        }




    }
}
