package com.pousheng.middle.web.warehouses;

import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.mq.component.CompensateBizLogic;
import com.pousheng.middle.mq.constant.MqConstants;
import com.pousheng.middle.open.api.dto.SkuStockRuleImportInfo;
import com.pousheng.middle.order.dto.PoushengCompensateBizCriteria;
import com.pousheng.middle.order.enums.PoushengCompensateBizStatus;
import com.pousheng.middle.order.enums.PoushengCompensateBizType;
import com.pousheng.middle.order.model.PoushengCompensateBiz;
import com.pousheng.middle.order.service.PoushengCompensateBizReadService;
import com.pousheng.middle.order.service.PoushengCompensateBizWriteService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.ShopWarehouseRuleClient;
import com.pousheng.middle.warehouse.companent.WarehouseRulesClient;
import com.pousheng.middle.warehouse.companent.WarehouseShopRuleClient;
import com.pousheng.middle.warehouse.dto.ShopWarehouseStockRuleDTO;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.warehouse.dto.ShopStockRule;
import com.pousheng.middle.warehouse.dto.ShopWarehouseStockRule;
import com.pousheng.middle.web.user.component.UserManageShopReader;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.applog.annotation.LogMe;
import io.terminus.applog.annotation.LogMeContext;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @author feisheng.ch
 * Date: 2018-05-10
 */
@RestController
@RequestMapping("/api/warehouse/shop-warehouse-rule")
@Slf4j
@OperationLogModule(OperationLogModule.Module.WAREHOUSE_SHOP_RULE)
@Api(description = "店铺下仓库的库存分配规则相关api")
public class ShopWarehouseStockRules {

    @Autowired
    private ShopWarehouseRuleClient shopWarehouseRuleClient;
    @Autowired
    private ShopSkuStockPushHandler shopSkuStockPushHandler;

    @Autowired
    private SkuTemplateSearchReadService skuTemplateSearchReadService;
    @Autowired
    private PoushengCompensateBizReadService poushengCompensateBizReadService;
    @Autowired
    private UserManageShopReader userManageShopReader;
    @Autowired
    private WarehouseRulesClient warehouseRulesClient;
    @Autowired
    private WarehouseShopRuleClient warehousePushRuleClient;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private CompensateBizLogic compensateBizLogic;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    /**
     * 创建仓库库存推送规则
     *
     * @param shopWarehouseStockRule 仓库库存推送规则
     * @return 新创建的规则id
     */
    @ApiOperation("创建仓库库存推送规则")
    @LogMe(description = "创建仓库级库存推送规则")
    @PostMapping
    @OperationLogType("创建")
    public Long create(@LogMeContext @RequestBody ShopWarehouseStockRule shopWarehouseStockRule) {
        userManageShopReader.authCheck(shopWarehouseStockRule.getShopId());
        Response<Long> createRet = shopWarehouseRuleClient.createShopWarehouseRule(shopWarehouseStockRule);
        if (!createRet.isSuccess()) {
            log.error("failed to create {}, cause:{}", shopWarehouseStockRule, createRet.getError());
            throw new JsonResponseException(createRet.getError());
        }
        return createRet.getResult();
    }


    /**
     * 创建仓库库存推送规则
     *
     * @param info 导入信息
     * @return 新创建的规则id
     */
    @ApiOperation("批量导入创建仓库级库存推送规则")
    @RequestMapping(value = "/import", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("批量导入创建仓库级库存推送规则")
    public Response<Long> create(@RequestBody SkuStockRuleImportInfo info) {
        userManageShopReader.authCheck(info.getOpenShopId());
        PoushengCompensateBiz biz = new PoushengCompensateBiz();
        biz.setBizType(PoushengCompensateBizType.IMPORT_WAREHOUSE_SKU_RULE.toString());
        biz.setContext(mapper.toJson(info));
        biz.setBizId(info.getOpenShopId().toString());
        biz.setStatus(PoushengCompensateBizStatus.WAIT_HANDLE.toString());
        return Response.ok(compensateBizLogic.createBizAndSendMq(biz,MqConstants.POSHENG_MIDDLE_EXPORT_COMPENSATE_BIZ_TOPIC));
    }


    /**
     * 查询导入文件的处理记录
     *
     * @param pageNo   第几页
     * @param pageSize 分页大小
     * @return 查询结果
     */
    @ApiOperation("查询导入文件的处理记录")
    @RequestMapping(value = "/import/result/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @OperationLogType("查询导入文件的处理记录")
    public Paging<PoushengCompensateBiz> importPaging(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                                      @RequestParam(required = false, value = "pageSize") Integer pageSize,
                                                      Long openShopId) {
        PoushengCompensateBizCriteria criteria = new PoushengCompensateBizCriteria();
        criteria.setPageNo(pageNo);
        criteria.setPageSize(pageSize);
        criteria.setBizId(openShopId.toString());
        criteria.setBizType(PoushengCompensateBizType.IMPORT_WAREHOUSE_SKU_RULE.name());
        Response<Paging<PoushengCompensateBiz>> response = poushengCompensateBizReadService.pagingForShow(criteria);
        if (!response.isSuccess()) {
            throw new JsonResponseException(response.getError());
        }
        return response.getResult();
    }

    /**
     * 列出当前用户能查看的仓库推送规则
     *
     * @param shopRuleId  规则id
     * @param warehouseId 仓库id
     * @return
     */
    @ApiOperation("列出当前用户能查看的仓库推送规则")
    @RequestMapping(value = "/{shopRuleId}/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ShopWarehouseStockRuleDTO> pagination(@PathVariable Long shopRuleId,
                                                      @RequestParam(required = false, value = "warehouseId") Long warehouseId) {
        ShopStockRule exist = warehousePushRuleClient.findById(shopRuleId);
        if (null == exist) {
            log.error("failed to find ShopStockRule(id={})", shopRuleId);
            throw new JsonResponseException("warehouse.shop.rule.find.fail");
        }
        List<ShopWarehouseStockRuleDTO> list = Lists.newArrayList();
        Response<List<Long>> resp = warehouseRulesClient.findWarehouseIdsByShopId(exist.getShopId());
        Map<Long, ShopWarehouseStockRule> map = shopWarehouseRuleClient.findByShopId(exist.getShopId());
        //查询默认发货仓列表失败
        if (!resp.isSuccess()) {
            throw new JsonResponseException(resp.getError());
        }
        if (warehouseId != null) {
            WarehouseDTO warehouseDTO = warehouseCacher.findById(warehouseId);
            if (!resp.getResult().contains(warehouseId)) {
                return list;
            }
            if (map.containsKey(warehouseId)) {
                list.add(new ShopWarehouseStockRuleDTO().warehouseDTO(warehouseDTO).shopWarehouseStockRule(map.get(warehouseId)));
            } else {
                list.add(new ShopWarehouseStockRuleDTO().warehouseDTO(warehouseDTO));
            }
            return list;
        }
        for (Long id : resp.getResult()) {
            WarehouseDTO warehouseDTO = warehouseCacher.findById(id);
            if (map.containsKey(id)) {
                list.add(new ShopWarehouseStockRuleDTO().warehouseDTO(warehouseDTO).shopWarehouseStockRule(map.get(id)));
            } else {
                list.add(new ShopWarehouseStockRuleDTO().warehouseDTO(warehouseDTO));
            }
        }
        return list;
    }

    /**
     * 更新仓库推送规则
     *
     * @param shopWarehouseStockRule 仓库推送规则
     * @return 是否成功
     */
    @ApiOperation("更新仓库推送规则")
    @LogMe(description = "更新仓库级库存推送规则", ignore = true)
    @PutMapping
    @OperationLogType("更新")
    public Boolean update(@LogMeContext @RequestBody ShopWarehouseStockRule shopWarehouseStockRule) {
        ShopWarehouseStockRule exist = shopWarehouseRuleClient.findById(shopWarehouseStockRule.getId());
        if (null == exist) {
            log.error("failed to find ShopWarehouseStockRule(id={}), error code:{}", shopWarehouseStockRule.getId());
            throw new JsonResponseException("warehouse.shop.sku.rule.find.fail");
        }
        userManageShopReader.authCheck(exist.getShopId());
        shopWarehouseStockRule.setId(exist.getId());
        shopWarehouseStockRule.setShopId(exist.getShopId());
        shopWarehouseStockRule.setWarehouseId(exist.getWarehouseId());
        Response<Boolean> updRet = shopWarehouseRuleClient.updateShopWarehouseRule(shopWarehouseStockRule);
        if (!updRet.isSuccess()) {
            log.error("failed to update {}, cause:{}", shopWarehouseStockRule, updRet.getError());
            throw new JsonResponseException(updRet.getError());
        }
        return updRet.getResult();
    }

    /**
     * 读取仓库推送规则
     *
     * @param id 规则id
     * @return 是否成功
     */
    @ApiOperation("读取仓库推送规则")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ShopWarehouseStockRule findById(@PathVariable Long id) {
        ShopWarehouseStockRule exist = shopWarehouseRuleClient.findById(id);
        if (null == exist) {
            log.error("failed to find ShopWarehouseStockRule(id={}), error code:{}", id);
            throw new JsonResponseException("warehouse.shop.sku.rule.find.fail");
        }
        userManageShopReader.authCheck(exist.getShopId());
        return exist;
    }

}
