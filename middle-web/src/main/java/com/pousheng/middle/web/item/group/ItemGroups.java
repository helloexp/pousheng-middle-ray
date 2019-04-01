package com.pousheng.middle.web.item.group;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.erp.service.PoushengMiddleSpuService;
import com.pousheng.middle.group.dto.ItemGroupCriteria;
import com.pousheng.middle.group.model.ItemGroup;
import com.pousheng.middle.group.model.ItemRuleGroup;
import com.pousheng.middle.group.service.ItemGroupReadService;
import com.pousheng.middle.group.service.ItemGroupSkuReadService;
import com.pousheng.middle.group.service.ItemGroupWriteService;
import com.pousheng.middle.group.service.ItemRuleGroupReadService;
import com.pousheng.middle.item.dto.ItemGroupAutoRule;
import com.pousheng.middle.item.dto.SearchSkuTemplate;
import com.pousheng.middle.item.enums.PsItemGroupSkuType;
import com.pousheng.middle.item.service.SkuTemplateSearchReadService;
import com.pousheng.middle.task.dto.ItemGroupTask;
import com.pousheng.middle.task.model.ScheduleTask;
import com.pousheng.middle.task.service.ScheduleTaskWriteService;
import com.pousheng.middle.web.item.cacher.ItemGroupCacherProxy;
import com.pousheng.middle.web.utils.task.ScheduleTaskUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Splitters;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.spu.model.SkuTemplate;
import io.terminus.parana.spu.model.Spu;
import io.terminus.parana.spu.service.SkuTemplateReadService;
import io.terminus.search.api.model.WithAggregations;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author zhaoxw
 * @date 2018/4/26
 */
@Api(description = "商品分组管理")
@RestController
@Slf4j
@RequestMapping("api/item/group")
public class ItemGroups {

    @RpcConsumer
    @Setter
    private ItemGroupReadService itemGroupReadService;

    @RpcConsumer
    @Setter
    private ItemGroupWriteService itemGroupWriteService;

    @RpcConsumer
    @Setter
    private ItemRuleGroupReadService itemRuleGroupReadService;

    @RpcConsumer
    @Setter
    private ScheduleTaskWriteService scheduleTaskWriteService;

    @RpcConsumer
    private SkuTemplateSearchReadService skuTemplateSearchReadService;

    @Autowired
    private ItemGroupCacherProxy itemGroupCacherProxy;

    @Autowired
    private PoushengMiddleSpuService poushengMiddleSpuService;

    @RpcConsumer
    private ItemGroupSkuReadService itemGroupSkuReadService;

    @RpcConsumer
    private SkuTemplateReadService skuTemplateReadService;



    @ApiOperation("查看商品分组信息")
    @GetMapping
    public ItemGroup getById(Long groupId) {
        Response<ItemGroup> findResp = itemGroupReadService.findById(groupId);
        if (!findResp.isSuccess()) {
            log.error("fail to find item group by id:{},cause:{}",
                    groupId, findResp.getError());
            throw new JsonResponseException(findResp.getError());
        }
        ItemGroup itemGroup = findResp.getResult();
        itemGroup.initRule(itemGroup);
        return itemGroup;
    }


    @ApiOperation("获取商品分组分页列表")
    @GetMapping("/paging")
    public Paging<ItemGroup> findBy(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                    @RequestParam(required = false, value = "pageSize") Integer pageSize,
                                    @RequestParam(required = false, value = "name") String name,
                                    @RequestParam(required = false, value = "skuCode") String skuCode,
                                    @RequestParam(required = false, value = "spuCode") String spuCode,
                                    @RequestParam(required = false, value = "exSkuCode") String exSkuCode,
                                    @RequestParam(required = false, value = "exSpuCode") String exSpuCode,
                                    @RequestParam(required = false, value = "skuType") Integer skuType) {
        ItemGroupCriteria criteria = new ItemGroupCriteria();
        if (StringUtils.isNoneEmpty(name)) {
            criteria.setName(name);
        }
        if (pageNo != null) {
            criteria.setPageNo(pageNo);
        }
        if (pageSize != null) {
            criteria.setPageSize(pageSize);
        }
        if (skuType != null) {
            if(Objects.equals(skuType,PsItemGroupSkuType.EXCLUDE.value())){
                skuCode = exSkuCode;
                spuCode = exSpuCode;
            }
            // 如果skucode不为空，则以skucode为准，忽略spucode
            if (StringUtils.isNotBlank(skuCode)) {
                List<Long> groupIds = getGroupIdsBySkucode(skuCode,skuType);
                if (!CollectionUtils.isEmpty(groupIds)){
                    criteria.setIds(groupIds);
                } else {
                    return new Paging<ItemGroup>(); 
                }                
            } else {
                if (StringUtils.isNotBlank(spuCode)) {
                    List<Long> groupIds = getGroupIdsBySpucode(spuCode,skuType);
                    if (!CollectionUtils.isEmpty(groupIds)){
                        criteria.setIds(groupIds); 
                    } else {
                        return new Paging<ItemGroup>();
                    }
                }
            }            
        }
        Response<Paging<ItemGroup>> findResp = itemGroupReadService.findByCriteria(criteria);
        if (!findResp.isSuccess()) {
            log.error("fail to find item group, pageNo:{},pageSize:{},cause:{}",
                    criteria.getPageNo(), criteria.getPageSize(), findResp.getError());
            throw new JsonResponseException(findResp.getError());
        }
        Paging<ItemGroup> paging = findResp.getResult();
        for (ItemGroup group : paging.getData()) {
            group.setRelatedNum(getGroupNum(group.getId()));
        }
        return findResp.getResult();

    }

    @ApiOperation("创建商品分组")
    @PostMapping
    public Long create(@RequestBody ItemGroup itemGroup) {
        Response<Boolean> checkResp = itemGroupReadService.checkName(itemGroup.getName());
        if (!checkResp.isSuccess()) {
            throw new JsonResponseException(checkResp.getError());
        }
        if (!checkResp.getResult()) {
            throw new JsonResponseException("item.group.name.duplicate");
        }
        itemGroup.auto(false).relatedNum(0L);
        Response<Long> createResp = itemGroupWriteService.create(itemGroup);
        if (!createResp.isSuccess()) {
            log.error("fail to create item group:{},cause:{}",
                    itemGroup, createResp.getError());
            throw new JsonResponseException(createResp.getError());
        }
        return createResp.getResult();
    }

    @ApiOperation("检查商品分组名称是否可用")
    @GetMapping("/check")
    public Boolean check(String name) {
        Response<Boolean> checkResp = itemGroupReadService.checkName(name);
        if (!checkResp.isSuccess()) {
            throw new JsonResponseException(checkResp.getError());
        }
        return checkResp.getResult();
    }

    @ApiOperation("修改商品分组基本信息")
    @PutMapping
    public boolean update(@RequestBody ItemGroup itemGroup) {
        if (itemGroup.getId() == null) {
            throw new JsonResponseException("item.group.id.not.exist");
        }
        Response<Boolean> updateResp = itemGroupCacherProxy.update(itemGroup);
        if (!updateResp.isSuccess()) {
            log.error("fail to update item group:{},cause:{}",
                    itemGroup, updateResp.getError());
            throw new JsonResponseException(updateResp.getError());
        }
        return updateResp.getResult();
    }


    @ApiOperation("修改分组自动分组规则")
    @PutMapping("/autoRule/{id}")
    public boolean updateAutoRule(@PathVariable("id") Long groupId, @RequestBody ItemGroupAutoRule rule) {
        if (groupId == null) {
            throw new JsonResponseException("item.group.id.not.exist");
        }
        Response<Boolean> updateResp = itemGroupWriteService.updateAutoRule(groupId, rule);
        if (!updateResp.isSuccess()) {
            log.error("fail to update item group auto rule :{},cause:{}",
                    rule, updateResp.getError());
            throw new JsonResponseException(updateResp.getError());
        }
        return updateResp.getResult();
    }

    @ApiOperation("删除商品分组")
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable("id") Long id) {
        Response<List<ItemRuleGroup>> resp = itemRuleGroupReadService.findByGroupId(id);
        if (!resp.isSuccess()) {
            log.error("fail to delete item group by id:{},cause:{}",
                    id, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        if (!CollectionUtils.isEmpty(resp.getResult())) {
            throw new JsonResponseException("item.group.is.used");
        }
        log.info("ready to delete item group(id:{})", id);
        List<ScheduleTask> list = Lists.newArrayList();
        //异步删除相应的组内商品
        Map<String, String> params = Maps.newHashMap();
        list.add(ScheduleTaskUtil.transItemGroupTask(new ItemGroupTask().params(params).groupId(id)
                .type(PsItemGroupSkuType.GROUP.value()).mark(false).userId(UserUtil.getUserId())));
        list.add(ScheduleTaskUtil.transItemGroupTask(new ItemGroupTask().params(params).groupId(id)
                .type(PsItemGroupSkuType.EXCLUDE.value()).mark(false).userId(UserUtil.getUserId())));
        Response<Integer> cResp = scheduleTaskWriteService.creates(list);
        if (!cResp.isSuccess()) {
            throw new JsonResponseException(cResp.getError());
        }
        Response<Boolean> deleteResp = itemGroupWriteService.delete(id);
        if (!deleteResp.isSuccess()) {
            log.error("fail to delete item group by id:{},cause:{}",
                    id, deleteResp.getError());
            throw new JsonResponseException(deleteResp.getError());
        }
        return deleteResp.getResult();
    }


    private Long getGroupNum(Long groupId) {
        String templateName = "ps_search.mustache";
        Map<String, String> params = Maps.newHashMap();
        params.put("groupId", groupId.toString());
        Response<WithAggregations<SearchSkuTemplate>> response = skuTemplateSearchReadService.doSearchWithAggs(1, 20, templateName, params, SearchSkuTemplate.class);
        if (!response.isSuccess()) {
            log.error("query sku template by groupId:{} fail,error:{}", groupId, response.getError());
            throw new JsonResponseException(response.getError());
        }
        return response.getResult().getTotal();
    }
    
    private List<Long> getGroupIdsBySkucode(String skuCode,Integer skuType){
        List<Long> groupIds = Lists.newArrayList();
        Response<List<Long>> groupIdsRes = itemGroupSkuReadService.findGroupIdsBySkuCodeAndType(Splitters.COMMA.splitToList(skuCode),skuType);
        if (groupIdsRes.isSuccess() && !CollectionUtils.isEmpty(groupIdsRes.getResult())){
            groupIds = groupIdsRes.getResult();
        }
        return groupIds;
    }

    private List<Long> getGroupIdsBySpucode(String spuCode,Integer skuType){
        List<Long> groupIds = Lists.newArrayList();
        Response<List<Spu>> spuResp = poushengMiddleSpuService.findBySpuCode(spuCode);
        if (spuResp.isSuccess() && !CollectionUtils.isEmpty(spuResp.getResult())){
            List<Long> spuIds = spuResp.getResult().stream().map(e ->e.getId()).collect(Collectors.toList());
            spuIds.forEach(spId ->{
            Response<List<SkuTemplate>> skuTemResp = skuTemplateReadService.findBySpuId(spId);
            if (skuTemResp.isSuccess() && !CollectionUtils.isEmpty(skuTemResp.getResult())){
                List<String> filterSkuCodes = skuTemResp.getResult().stream().map(e -> e.getSkuCode()).collect(Collectors.toList());
                Response<List<Long>> groupIdsRes = itemGroupSkuReadService.findGroupIdsBySkuCodeAndType(filterSkuCodes,skuType);
                if (groupIdsRes.isSuccess() && !CollectionUtils.isEmpty(groupIdsRes.getResult())){
                    groupIds.addAll(groupIdsRes.getResult());
                }
            }
            });            
        }
        return groupIds;
    }
}
