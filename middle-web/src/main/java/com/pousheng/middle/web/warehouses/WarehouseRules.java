package com.pousheng.middle.web.warehouses;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.WarehouseRulesClient;
import com.pousheng.middle.warehouse.dto.ThinShop;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.warehouse.model.WarehouseRuleItem;
import com.pousheng.middle.warehouse.model.WarehouseShopGroup;
import com.pousheng.middle.web.item.cacher.VipWarehouseMappingProxy;
import com.pousheng.middle.web.order.component.OrderReadLogic;
import com.pousheng.middle.web.shop.cache.ShopChannelGroupCacher;
import com.pousheng.middle.web.shop.dto.ShopChannel;
import com.pousheng.middle.web.shop.dto.ShopChannelGroup;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.warehouses.component.WarehouseRuleComponent;
import io.swagger.annotations.ApiOperation;
import io.terminus.applog.annotation.LogMe;
import io.terminus.applog.annotation.LogMeContext;
import io.terminus.applog.annotation.LogMeId;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.common.exception.InvalidException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 店铺规则
 * <p>
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-20
 */
@RestController
@RequestMapping("/api/warehouse/rule")
@Slf4j
@OperationLogModule(OperationLogModule.Module.WAREHOUSE_RULE)
public class WarehouseRules {

    @RpcConsumer
    private OpenShopReadService openShopReadService;
    @Autowired
    private OrderReadLogic orderReadLogic;
    @Autowired
    private WarehouseRuleComponent warehouseRuleComponent;
    @Autowired
    private ShopChannelGroupCacher shopChannelGroupCacher;
    @Autowired
    private ShopCacher shopCacher;
    @Autowired
    private WarehouseRulesClient warehouseRulesClient;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private VipWarehouseMappingProxy vipWarehouseMappingProxy;


    /**
     * 创建规则适用的地址信息, 同时会创建仓库发货优先级规则, 并返回新创建的rule id
     *
     * @param shops 勾选的店铺
     * @return rule id 新生成的规则id
     */
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @LogMe(description = "创建了发货仓规则", ignore = true)
    public Long create(@RequestBody @LogMeContext ThinShop[] shops) {
        //判断所选店铺是否属于同一账套
        List<ThinShop> thinShops = Lists.newArrayList(shops);

        Boolean isAllChannel= orderReadLogic.isAllChannelOpenShop(thinShops.get(0).getShopId());
        if(thinShops.size()>1){
            for(ThinShop shop:thinShops){
                if(isAllChannel!=orderReadLogic.isAllChannelOpenShop(shop.getShopId())){
                    throw new JsonResponseException("open.shop.type.differ");
                }
            }
        }
        Set<Long> shopIds = warehouseRulesClient.findAllConfigShopIds();
        for (ThinShop thinShop : thinShops) {
            if (shopIds.contains(thinShop.getShopId())) {
                throw new JsonResponseException("shop.may.conflict");
            }
        }
        Response<Long> r = warehouseRulesClient.createRule(thinShops);
        if (!r.isSuccess()) {
            log.error("failed to batchCreate warehouse rule with shops:{}, error code:{}", shops, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }


    /**
     * 创建规则适用的地址信息, 同时会创建仓库发货优先级规则, 并返回新创建的rule id
     *
     * @param shops 勾选的店铺
     * @return rule id 新生成的规则id
     */
    @RequestMapping(value="/{ruleId}",method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @LogMe(description = "修改了发货仓规则",ignore = true)
    public Boolean update(@PathVariable @LogMeContext Long ruleId, @RequestBody @LogMeContext ThinShop[] shops) {
        //判断所选店铺是否属于同一账套
        /*List<ThinShop> thinShops = Lists.newArrayList(shops);
        List<Long> shopIds = thinShops.stream().map(ThinShop::getShopId).collect(Collectors.toList());
        List<OpenShop> openShops = orderReadLogic.findOpenShopByShopIds(shopIds);
        Set<String> companyCodes = new HashSet<>();
        openShops.forEach(openShop -> {
            String companyCode = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.HK_COMPANY_CODE,openShop);
            if(openShop.getShopName().startsWith("mpos") && openShop.getAppKey().contains("-")){
                companyCode = openShop.getAppKey().substring(0,openShop.getAppKey().indexOf("-"));
            }
            companyCodes.add(companyCode);
        });
        if (companyCodes.size()>1){
            log.error("can not add more company code:{}",companyCodes);
            throw new JsonResponseException("shop.must.be.in.one.company");
        }*/
        List<ThinShop> thinShops = Lists.newArrayList(shops);

        Boolean isAllChannel= orderReadLogic.isAllChannelOpenShop(thinShops.get(0).getShopId());
        if(thinShops.size()>1){
            for(ThinShop shop:thinShops){
                if(isAllChannel!=orderReadLogic.isAllChannelOpenShop(shop.getShopId())){
                    throw new JsonResponseException("open.shop.type.differ");
                }
            }
        }
        Response<Boolean> r = warehouseRulesClient.updateRule(ruleId, Lists.newArrayList(shops));
        if (!r.isSuccess()) {
            log.error("failed to batch update warehouse rule(id={}) with shops:{}, error code:{}",
                    ruleId, shops, r.getError());
            throw new JsonResponseException(r.getError());
        }
        //刷新open shop缓存
        shopChannelGroupCacher.refreshShopChannelGroupCache();
        return r.getResult();
    }




    /**
     * 删除单条规则
     *
     * @param ruleId 规则id
     * @return 是否删除成功
     */
    @ApiOperation("删除单条规则")
    @LogMe(description = "删除单条发货仓规则", deleting = "warehouseRulesClient#findRuleById")
    @RequestMapping(value = "/{ruleId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean delete(@PathVariable @LogMeId Long ruleId) {
        Response<Boolean> r = warehouseRulesClient.deleteById(ruleId);
        if (!r.isSuccess()) {
            log.error("failed to delete warehouse rule(id={}), error code:{}", ruleId, r.getError());
            throw new JsonResponseException(r.getError());
        }
        //刷新open shop缓存
        shopChannelGroupCacher.refreshShopChannelGroupCache();

        return r.getResult();
    }

    /**
     * 删除店铺组, 同时删除对应的发货规则记录
     *
     * @param groupId 店铺组id
     * @return 是否删除成功
     */
    @ApiOperation("删除店铺组,同时删除对应的发货规则记录")
    @LogMe(description = "删除店铺组", deleting = "warehouseRulesClient#findShopListByGroup")
    @RequestMapping(value = "/group/{groupId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean deleteShopGroup(@PathVariable @LogMeId Long groupId){
        Response<Boolean> r = warehouseRulesClient.deleteShopGroup(groupId);
        if (!r.isSuccess()) {
            log.error("failed to delete warehouse shop group(id={}), error code:{}", groupId, r.getError());
            throw new JsonResponseException(r.getError());
        }
        //刷新open shop缓存
        shopChannelGroupCacher.refreshShopChannelGroupCache();

        return r.getResult();
    }

    @RequestMapping(value = "/shops", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ThinShop> markShops(@RequestParam(value = "groupId", required = false) Long groupId) {
        //获取店铺列表集合
        List<ThinShop> thinShops = findAllCandidateShops();

        //标记所有已设置发货规则的店铺不可被编辑
        disableRuleShops( thinShops);


        //标记当前规则选的店铺可以编辑
        if(groupId!=null) {
            enableCurrentRuleShops(groupId, thinShops);
        }
        return thinShops;
    }

    @RequestMapping(value = "/shops-new", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ShopChannelGroup> markShopsNew(@RequestParam(value = "groupId", required = false) Long groupId) {
        //获取店铺列表集合
        List<ShopChannelGroup> channelGroups = shopChannelGroupCacher.listAllShopChannelGroupCache();

        //标记所有已设置发货规则的店铺不可被编辑
        disableRuleShopsNew(channelGroups);


        //标记当前规则选的店铺可以编辑
        if(groupId!=null) {
            enableCurrentRuleShopsNew(groupId,channelGroups);
        }
        return channelGroups;
    }

    /**
     * 根据ruleId获取公司码
     * @param ruleId
     * @return
     */
    @RequestMapping(value = "/{ruleId}/company/code",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public String getCompanyCode(@PathVariable Long ruleId){
        List<WarehouseShopGroup> shopGroups = warehouseRuleComponent.findWarehouseShopGropsByRuleId(ruleId);
        WarehouseShopGroup warehouseShopGroup = shopGroups.get(0);
        OpenShop openShop = orderReadLogic.findOpenShopByShopId(warehouseShopGroup.getShopId());
        String companyCode = orderReadLogic.getOpenShopExtraMapValueByKey(TradeConstants.HK_COMPANY_CODE,openShop);
        if(Strings.isNullOrEmpty(companyCode) && openShop.getAppKey().contains("-")){
            companyCode = openShop.getAppKey().substring(0,openShop.getAppKey().indexOf("-"));
        }
        if(Strings.isNullOrEmpty(companyCode)){
            log.error("open shop (id:{}) company code invalid",openShop.getId());
            throw new JsonResponseException("rule.shop.company.code.invalid");
        }
        return companyCode;
    }

    /**
     * 根据ruleId获取第一个店铺的渠道 如果是vip的话 检查映射
     * @param ruleId
     * @return
     */
    @RequestMapping(value = "/{ruleId}/check", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<String> checkChannel(@PathVariable Long ruleId, @RequestBody WarehouseRuleItem[] warehouseRuleItems) {
        List<WarehouseShopGroup> shopGroups = warehouseRuleComponent.findWarehouseShopGropsByRuleId(ruleId);
        WarehouseShopGroup warehouseShopGroup = shopGroups.get(0);
        OpenShop openShop = orderReadLogic.findOpenShopByShopId(warehouseShopGroup.getShopId());
        if (Objects.equal(MiddleChannel.VIP.getValue(), openShop.getChannel())) {
            ArrayList<WarehouseRuleItem> ruleItemArrayList = Lists.newArrayList(warehouseRuleItems);
            List<String> invalidWarehouse = Lists.newArrayList();
            for (WarehouseRuleItem it : ruleItemArrayList) {
                WarehouseDTO warehouseDTO = warehouseCacher.findById(it.getWarehouseId());
                if (vipWarehouseMappingProxy.findByWarehouseId(warehouseDTO.getId()) == null){
                    invalidWarehouse.add(warehouseDTO.getWarehouseName() + "(" + warehouseDTO.getOutCode() + ")");
                }
            }
            if (!ObjectUtils.isEmpty(invalidWarehouse)) {
                log.error("find shop by invalidWarehouse:{} fail", invalidWarehouse);
                throw new InvalidException(500, "not.vip.warehouse(outCode={0})", invalidWarehouse.toString());
            }
        }
        return Response.ok(openShop.getChannel());
    }

    private List<ThinShop> findAllCandidateShops() {

        Response<List<OpenShop>> r = openShopReadService.findAll();
        if (!r.isSuccess()) {
            log.error("failed to find open shops, error code:{}", r.getError());
            throw new JsonResponseException(r.getError());
        }
        List<OpenShop> openShops = r.getResult();
        List<ThinShop> thinShops = Lists.newArrayListWithCapacity(openShops.size());
        for (OpenShop openShop : openShops) {
            ThinShop thinShop = new ThinShop();
            thinShop.setShopId(openShop.getId());
            thinShop.setShopName(openShop.getChannel()+"-"+openShop.getShopName());
            thinShops.add(thinShop);
        }
        return thinShops;
    }

    private void disableRuleShops( List<ThinShop> thinShops) {
        //获取所有已设置规则的店铺
        Set<Long> shopIds = warehouseRulesClient.findAllConfigShopIds();
        if (null == shopIds) {
            log.error("failed to find shopIds which have warehouse rules set");
            throw new JsonResponseException("warehouse.rule.find.fail");
        }

        //标记所有已设置发货规则的店铺不可被编辑
        for (ThinShop thinShop : thinShops) {
            if (shopIds.contains(thinShop.getShopId())) {
                thinShop.setEditable(false);
            }
        }
    }

    private void disableRuleShopsNew(List<ShopChannelGroup> channelGroups) {
        //获取所有已设置规则的店铺
        Set<Long> shopIds = warehouseRulesClient.findAllConfigShopIds();
        if (null == shopIds) {
            log.error("failed to find shopIds which have warehouse rules set");
            throw new JsonResponseException("warehouse.rule.find.fail");
        }


        //标记所有已设置发货规则的店铺不可被编辑
        for (ShopChannelGroup channelGroup : channelGroups) {

            List<ShopChannel> shopChannels = channelGroup.getShopChannels();

            for (ShopChannel shopChannel : shopChannels){
                OpenClientShop openClientShop = shopChannel.getOpenClientShop();
                if(Arguments.notNull(openClientShop)){
                    if (shopIds.contains(openClientShop.getOpenShopId())) {
                        openClientShop.setEditable(false);
                    }
                }

                List<OpenClientShop> zoneOpenClientShops = shopChannel.getZoneOpenClientShops();
                if(!CollectionUtils.isEmpty(zoneOpenClientShops)){
                    for (OpenClientShop openShop : zoneOpenClientShops){
                        if (shopIds.contains(openShop.getOpenShopId())) {
                            openShop.setEditable(false);
                        }
                    }
                }

            }
        }
    }

    //标记当前规则选的店铺可以编辑
    private void enableCurrentRuleShops(Long shopGroupId, List<ThinShop> thinShops) {
        List<WarehouseShopGroup> rwsrs = warehouseRulesClient.findShopListByGroup(shopGroupId);
        if (null == rwsrs) {
            log.error("failed to find warehouseShopGroups by shopGroupId={}", shopGroupId);
            throw new JsonResponseException("warehouse.shopgroup.find.fail");
        }

        for (WarehouseShopGroup warehouseShopGroup : rwsrs) {
            Long shopId = warehouseShopGroup.getShopId();
            for (ThinShop thinShop : thinShops) {
                if (Objects.equal(thinShop.getShopId(), shopId)) {
                    thinShop.setEditable(true);
                    thinShop.setSelected(true);
                }
            }
        }
    }


    //标记当前规则选的店铺可以编辑
    private void enableCurrentRuleShopsNew(Long shopGroupId, List<ShopChannelGroup> channelGroups) {
        List<WarehouseShopGroup> rwsrs = warehouseRulesClient.findShopListByGroup(shopGroupId);
        if (null == rwsrs) {
            log.error("failed to find warehouseShopGroups by shopGroupId={}", shopGroupId);
            throw new JsonResponseException("warehouse.shopgroup.find.fail");
        }
        for (WarehouseShopGroup warehouseShopGroup : rwsrs) {
            Long shopId = warehouseShopGroup.getShopId();
            for (ShopChannelGroup channelGroup : channelGroups) {

                List<ShopChannel> shopChannels = channelGroup.getShopChannels();

                for (ShopChannel shopChannel : shopChannels){
                    OpenClientShop openClientShop = shopChannel.getOpenClientShop();
                    if(Arguments.notNull(openClientShop)){
                        if (Objects.equal(openClientShop.getOpenShopId(), shopId)) {
                            openClientShop.setEditable(true);
                            openClientShop.setSelected(true);
                        }
                    }

                    List<OpenClientShop> zoneOpenClientShops = shopChannel.getZoneOpenClientShops();
                    if(!CollectionUtils.isEmpty(zoneOpenClientShops)){
                        for (OpenClientShop openShop : zoneOpenClientShops){
                            if (Objects.equal(openShop.getOpenShopId(), shopId)) {
                                openShop.setEditable(true);
                                openShop.setSelected(true);
                            }
                        }
                    }

                }
            }
        }
    }
}
