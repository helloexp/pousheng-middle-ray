package com.pousheng.middle.web.warehouses;

import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.open.api.dto.WarehouseRulePriorityImportInfo;
import com.pousheng.middle.order.dto.PoushengCompensateBizCriteria;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.WarehouseRulesItemClient;
import com.pousheng.middle.warehouse.dto.*;
import com.pousheng.middle.warehouse.model.WarehouseRulePriority;
import com.pousheng.middle.warehouse.model.WarehouseRulePriorityItem;
import com.pousheng.middle.warehouse.service.WarehouseRulePriorityItemReadService;
import com.pousheng.middle.warehouse.service.WarehouseRulePriorityItemWriteService;
import com.pousheng.middle.warehouse.service.WarehouseRulePriorityReadService;
import com.pousheng.middle.warehouse.service.WarehouseRulePriorityWriteService;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author zhaoxw
 * @date 2018/9/4
 */
@Slf4j
@RestController
@RequestMapping("api/warehouse/rule/priority")
public class WarehouseRulePriorities {

    @RpcConsumer
    private WarehouseRulePriorityItemReadService warehouseRulePriorityItemReadService;

    @RpcConsumer
    private WarehouseRulePriorityReadService warehouseRulePriorityReadService;

    @RpcConsumer
    private WarehouseRulePriorityWriteService warehouseRulePriorityWriteService;

    @RpcConsumer
    private WarehouseRulePriorityItemWriteService warehouseRulePriorityItemWriteService;

    @RpcConsumer
    private PoushengCompensateBizReadService poushengCompensateBizReadService;

    @RpcConsumer
    private WarehouseRulesItemClient warehouseRulesItemClient;

    @Autowired
    private WarehouseCacher warehouseCacher;

    @Autowired
    private CompensateBizLogic compensateBizLogic;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    @ApiOperation("获取派单优先级规则列表")
    @GetMapping("/paging")
    public Paging<WarehouseRulePriority> findBy(RulePriorityCriteria criteria) {
        if (criteria.getRuleId() == null) {
            throw new JsonResponseException("warehouse.rule.item.id.is.null");
        }
        if (criteria.getEndAt() != null) {
            criteria.setEndAt(new DateTime(criteria.getEndAt().getTime()).plusDays(1).minusSeconds(1).toDate());
        }
        Response<Paging<WarehouseRulePriority>> findResp = warehouseRulePriorityReadService.findByCriteria(criteria);
        if (!findResp.isSuccess()) {
            log.error("fail to find warehouse rule priority, pageNo:{},pageSize:{},cause:{}",
                    criteria.getPageNo(), criteria.getPageSize(), findResp.getError());
            throw new JsonResponseException(findResp.getError());
        }
        return findResp.getResult();
    }


    @ApiOperation("根据id获取派单优先级规则")
    @GetMapping("/{id}")
    public WarehouseRulePriority findById(@PathVariable Long id) {
        Response<WarehouseRulePriority> findResp = warehouseRulePriorityReadService.findById(id);
        if (!findResp.isSuccess()) {
            log.error("fail to find warehouse rule priority, id:{},cause:{}",
                    id, findResp.getError());
            throw new JsonResponseException(findResp.getError());
        }
        return findResp.getResult();
    }

    @ApiOperation("创建派单优先级规则")
    @PostMapping
    public Long create(@RequestBody WarehouseRulePriority warehouseRulePriority) {
        Response<Boolean> checkResp = warehouseRulePriorityReadService.checkByName(warehouseRulePriority);
        if (!checkResp.isSuccess()) {
            throw new JsonResponseException(checkResp.getError());
        }
        if (!checkResp.getResult()) {
            throw new JsonResponseException("warehouse.rule.priority.name.duplicate");
        }
        checkResp = warehouseRulePriorityReadService.checkTimeRange(warehouseRulePriority);
        if (!checkResp.isSuccess()) {
            throw new JsonResponseException(checkResp.getError());
        }
        if (!checkResp.getResult()) {
            throw new JsonResponseException("warehouse.rule.priority.range.coincidence");
        }
        Response<Long> createResp = warehouseRulePriorityWriteService.create(warehouseRulePriority);
        if (!createResp.isSuccess()) {
            log.error("fail to create rule priority:{},cause:{}", warehouseRulePriority, createResp.getError());
            throw new JsonResponseException(createResp.getError());
        }
        return createResp.getResult();
    }


    @ApiOperation("更新派单优先级规则")
    @PutMapping
    public Boolean update(@RequestBody WarehouseRulePriority warehouseRulePriority) {
        if (warehouseRulePriority.getId() == null) {
            throw new JsonResponseException("rule.priority.id.not.exist");
        }
        Response<Boolean> checkResp = warehouseRulePriorityReadService.checkByName(warehouseRulePriority);
        if (!checkResp.isSuccess()) {
            throw new JsonResponseException(checkResp.getError());
        }
        if (!checkResp.getResult()) {
            throw new JsonResponseException("warehouse.rule.priority.name.duplicate");
        }
        checkResp = warehouseRulePriorityReadService.checkTimeRange(warehouseRulePriority);
        if (!checkResp.isSuccess()) {
            throw new JsonResponseException(checkResp.getError());
        }
        if (!checkResp.getResult()) {
            throw new JsonResponseException("warehouse.rule.priority.range.coincidence");
        }
        Response<Boolean> updateResp = warehouseRulePriorityWriteService.update(warehouseRulePriority);
        if (!updateResp.isSuccess()) {
            log.error("fail to update rule priority:{},cause:{}", warehouseRulePriority, updateResp.getError());
            throw new JsonResponseException(updateResp.getError());
        }
        return updateResp.getResult();
    }


    @ApiOperation("删除派单优先级规则")
    @DeleteMapping
    public Boolean delete(Long id) {
        Response<Boolean> delResp = warehouseRulePriorityWriteService.deleteById(id);
        if (!delResp.isSuccess()) {
            log.error("fail to delete rule priority:{},cause:{}",
                    id, delResp.getError());
            throw new JsonResponseException(delResp.getError());
        }
        return delResp.getResult();
    }


    /**
     * 创建派单规则仓库优先级
     *
     * @param info 导入信息
     * @return 新创建的规则id
     */
    @ApiOperation("批量导入创建仓库优先级")
    @PostMapping(value = "/import")
    public Response<Long> create(@RequestBody WarehouseRulePriorityImportInfo info) {
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.IMPORT_WAREHOUSE_RULE_PRIORITY_ITEM.toString());
        biz.setContext(mapper.toJson(info));
        biz.setBizId(info.getPriorityId().toString());
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        return Response.ok(compensateBizLogic.createBizAndSendMq(biz,MqConstants.POSHENG_MIDDLE_COMMON_COMPENSATE_BIZ_TOPIC));
    }


    /**
     * 查询导入文件的处理记录
     *
     * @param pageNo   第几页
     * @param pageSize 分页大小
     * @return 查询结果
     */
    @ApiOperation("查询导入文件的处理记录")
    @GetMapping(value = "/import/result/paging")
    @OperationLogType("查询导入文件的处理记录")
    public Paging<PoushengCompensateBiz> importPaging(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                                      @RequestParam(required = false, value = "pageSize") Integer pageSize,
                                                      Integer priorityId) {
        PoushengCompensateBizCriteria criteria = new PoushengCompensateBizCriteria();
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);
        criteria.setBizId(priorityId.toString());
        criteria.setBizType(PoushengCompensateBizType.IMPORT_WAREHOUSE_RULE_PRIORITY_ITEM.name());
        Response<Paging<PoushengCompensateBiz>> response = poushengCompensateBizReadService.pagingForShow(criteria);
        if (!response.isSuccess()) {
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }


    @ApiOperation("查询规则里的优先级明细")
    @GetMapping(value = "/detail")
    public List<WarehouseRulePriorityItemDTO> detail(RulePriorityItemCriteria criteria) {
        if (criteria.getPriorityId() == null) {
            throw new JsonResponseException("warehouse.rule.priority.id.is.null");
        }
        Response<WarehouseRulePriority> findResp = warehouseRulePriorityReadService.findById(criteria.getPriorityId());
        if (!findResp.isSuccess()) {
            log.error("fail to find warehouse rule priority, id:{},cause:{}",
                    criteria.getPriorityId(), findResp.getError());
            throw new JsonResponseException(findResp.getError());
        }

        List<WarehouseRulePriorityItemDTO> list = Lists.newArrayList();
        Response<WarehouseRuleDto> resp = warehouseRulesItemClient.findByRuleId(findResp.getResult().getRuleId());
        if (!resp.isSuccess()) {
            throw new JsonResponseException(resp.getError());
        }
        List<WarehouseRuleItemDto> dtos = resp.getResult().getWarehouseRuleItemDtos();
        Response<List<WarehouseRulePriorityItem>> itemResp = warehouseRulePriorityItemReadService.findByPriorityId(criteria.getPriorityId());
        if (!itemResp.isSuccess()) {
            throw new JsonResponseException(itemResp.getError());
        }
        List<Long> warehouseIds = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(itemResp.getResult())) {
            warehouseIds = itemResp.getResult().stream().map(e -> e.getWarehouseId()).collect(Collectors.toList());
        }
        if (dtos.size() > 0 && criteria.getWarehouseType() != null) {
            dtos = dtos.stream().filter(e -> e.getType().equals(criteria.getWarehouseType())).collect(Collectors.toList());
        }
        if (dtos.size() > 0 && criteria.getWarehouseName() != null) {
            dtos = dtos.stream().filter(e -> e.getName().equals(criteria.getWarehouseName())).collect(Collectors.toList());
        }
        if (dtos.size() > 0 && criteria.getOutCode() != null) {
            dtos = dtos.stream().filter(e -> e.getOutCode().equals(criteria.getOutCode())).collect(Collectors.toList());
        }
        if (dtos.size() == 0) {
            return list;
        }
        Iterator<WarehouseRulePriorityItem> it = itemResp.getResult().iterator();
        while (it.hasNext() && !CollectionUtils.isEmpty(warehouseIds)) {
            WarehouseRulePriorityItem item = it.next();
            for (WarehouseRuleItemDto dto : dtos) {
                if (item.getWarehouseId().equals(dto.getWarehouseId())) {
                    list.add(new WarehouseRulePriorityItemDTO().item(item).warehouse(dto));
                    dtos.remove(dto);
                    break;
                }
            }
        }
        for (WarehouseRuleItemDto dto : dtos) {
            list.add(new WarehouseRulePriorityItemDTO().warehouse(dto));
        }
        return list;
    }


    @ApiOperation("创建派单优先级规则明细")
    @PostMapping("/item")
    public Long createItem(@RequestBody WarehouseRulePriorityItem warehouseRulePriorityItem) {
        Response<Long> createResp = warehouseRulePriorityItemWriteService.create(warehouseRulePriorityItem);
        if (!createResp.isSuccess()) {
            log.error("fail to create rule priority item:{},cause:{}", warehouseRulePriorityItem, createResp.getError());
            throw new JsonResponseException(createResp.getError());
        }
        return createResp.getResult();
    }


    @ApiOperation("更新派单优先级规则明细")
    @PutMapping("/item")
    public Boolean updateItem(@RequestBody WarehouseRulePriorityItem warehouseRulePriorityItem) {
        if (warehouseRulePriorityItem.getId() == null) {
            throw new JsonResponseException("rule.priority.item.id.not.exist");
        }
        if (warehouseRulePriorityItem.getPriority() == null) {
            Response<Boolean> delResp = warehouseRulePriorityItemWriteService.deleteById(warehouseRulePriorityItem.getId());
            if (!delResp.isSuccess()) {
                log.error("fail to delete rule priority item:{},cause:{}",
                        warehouseRulePriorityItem.getId(), delResp.getError());
                throw new JsonResponseException(delResp.getError());
            }
            return delResp.getResult();
        }
        Response<Boolean> updateResp = warehouseRulePriorityItemWriteService.update(warehouseRulePriorityItem);
        if (!updateResp.isSuccess()) {
            log.error("fail to update rule priority item:{},cause:{}", warehouseRulePriorityItem, updateResp.getError());
            throw new JsonResponseException(updateResp.getError());
        }
        return updateResp.getResult();
    }
}
