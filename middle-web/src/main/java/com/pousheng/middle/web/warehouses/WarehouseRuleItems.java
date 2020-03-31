package com.pousheng.middle.web.warehouses;


import com.pousheng.middle.warehouse.companent.WarehouseRulesClient;
import com.pousheng.middle.warehouse.companent.WarehouseRulesItemClient;
import com.pousheng.middle.warehouse.dto.WarehouseRuleDto;
import com.pousheng.middle.warehouse.model.WarehouseShopGroup;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import io.swagger.annotations.ApiOperation;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

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

    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private WarehouseRulesItemClient warehouseRulesItemClient;
    @Autowired
    private WarehouseRulesClient warehouseRulesClient;

    @ApiOperation("根据规则ID查找仓库规则")
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public WarehouseRuleDto findByRuleId(@PathVariable Long ruleId){
        Response<WarehouseRuleDto> ruleDtoRes = warehouseRulesItemClient.findByRuleId(ruleId);
        if(!ruleDtoRes.isSuccess()){
            log.error("find warehouse rule by id:{} fail,error:{}",ruleId,ruleDtoRes.getError());
            throw new JsonResponseException(ruleDtoRes.getError());
        }

        WarehouseRuleDto ruleDto = ruleDtoRes.getResult();


        List<WarehouseShopGroup> rwsrs = warehouseRulesClient.findShopListByGroup(ruleDto.getWarehouseRule().getShopGroupId());
        if (ObjectUtils.isEmpty(rwsrs)) {
            ruleDto.setIsALlChannel(false);
        } else {
            ruleDto.setIsALlChannel(orderReadLogic.isAllChannelOpenShop(rwsrs.get(0).getShopId()));
        }

        return ruleDto;
    }

}
