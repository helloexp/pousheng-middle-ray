package com.pousheng.middle.web.middleLog;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.core.type.TypeReference;
import com.pousheng.auth.model.MiddleUser;
import com.pousheng.auth.service.PsUserReadService;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.InventoryClient;
import com.pousheng.middle.warehouse.model.PoushengChannelDTO;
import com.pousheng.middle.warehouse.model.SkuInventory;
import com.pousheng.middle.web.middleLog.dto.ApplogDto;
import com.pousheng.middle.web.middleLog.dto.ApplogTypeEnum;
import io.swagger.annotations.ApiOperation;
import io.terminus.applog.core.criteria.MemberApplicationLogCriteria;
import io.terminus.applog.core.model.MemberAppLogKey;
import io.terminus.applog.core.model.MemberApplicationLog;
import io.terminus.applog.core.service.MemberApplicationLogReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author zhaoxw
 * @date 2018/8/20
 */
@RestController
@RequestMapping("/api/applog")
@Slf4j
public class ApplicationLogs {

    @RpcConsumer
    private MemberApplicationLogReadService memberApplicationLogReadService;

    @Autowired
    private ApplLogKeyCacher applLogKeyCacher;

    @Autowired
    private PsUserReadService psUserReadService;

    @Autowired
    private MiddleShopCacher middleShopCacher;

    @Autowired
    private WarehouseCacher warehouseCacher;

    @Autowired
    private InventoryClient inventoryClient;

    @ApiOperation("操作日志key")
    @RequestMapping(value = "/keys", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<MemberAppLogKey> keys() {
        List<MemberAppLogKey> keys = Lists.newArrayList();
        for (String d : ApplogTypeEnum.getKeys()) {
            keys.add(applLogKeyCacher.findByDescription(d));
        }
        return keys;
    }

    @ApiOperation("操作日志列表")
    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<ApplogDto> pagination(MemberApplicationLogCriteria criteria, @RequestParam(value = "operator", required = false) String operator) {
        if (StringUtils.isEmpty(criteria.getRootKeyId())) {
            List<String> keys = Lists.newArrayList();
            keys().stream().map(MemberAppLogKey::getId).collect(Collectors.toList()).forEach(e -> keys.add(e.toString()));
            criteria.setRootKeyIdList(keys);
        } else {
            criteria.setRootKeyIdList(Lists.newArrayList(criteria.getRootKeyId()));
        }
        if (!StringUtils.isEmpty(operator)) {
            Response<MiddleUser> userResp = psUserReadService.findByName(operator);
            if (!userResp.isSuccess() || userResp.getResult() == null) {
                return new Paging<>();
            }
            criteria.setOperatorId(userResp.getResult().getId().toString());
        }
        if (criteria.getCreatedEndAt() != null) {
            if (criteria.getCreatedEndAt() != null) {
                criteria.setCreatedEndAt(new DateTime(criteria.getCreatedEndAt().getTime()).plusDays(1).minusSeconds(1).toDate());
            }
        }
        Response<Paging<MemberApplicationLog>> response = memberApplicationLogReadService.paging(criteria);
        if (!response.isSuccess()) {
            throw new JsonResponseException(response.getError());
        }
        List<ApplogDto> list = Lists.newArrayList();
        for (MemberApplicationLog log : response.getResult().getData()) {
            list.add(assembleLog(log));
        }
        Paging<ApplogDto> paging = new Paging<>();
        paging.setData(list);
        paging.setTotal(response.getResult().getTotal());
        return paging;

    }

    /**
     * 组装成dto对象，
     *
     * @param memberApplicationLog
     * @return
     */
    private ApplogDto assembleLog(MemberApplicationLog memberApplicationLog) {
        ApplogDto dto = new ApplogDto();
        Map infoMap = JSON.parseObject(memberApplicationLog.getMetadata());
        dto.setCreatedAt(memberApplicationLog.getCreatedAt());
        dto.setOperator(infoMap.get("operator").toString());
        dto.setType(infoMap.get("description").toString());
        List<Object> list = JSON.parseArray(infoMap.get("contexts").toString(), Object.class);
        Map<String, String> result = new HashMap<>(10);
        ApplogTypeEnum type = ApplogTypeEnum.from(applLogKeyCacher.findById(Long.parseLong(memberApplicationLog.getRootKeyId())).getDescription());
        try {
            switch (type) {
                case SET_SAFE_STOCK:
                    Response<SkuInventory> response = inventoryClient.findInventoryById(Long.parseLong(list.get(0).toString()));
                    if (!response.isSuccess()) {
                        throw new JsonResponseException("fail.find.stock.log");
                    }
                    SkuInventory skuInventory = response.getResult();
                    result.put("warehouseName", warehouseCacher.findByCode(skuInventory.getWarehouseCode()).getWarehouseName());
                    result.put("warehouseCode", warehouseCacher.findByCode(skuInventory.getWarehouseCode()).getOutCode());
                    result.put("num", list.size() == 1 ? "0" : list.get(1).toString());
                    break;
                case SET_WAREHOUSR_SAFE_STOCK:
                    result.put("warehouseName", warehouseCacher.findById(Long.parseLong(list.get(0).toString())).getWarehouseName());
                    result.put("warehouseCode", warehouseCacher.findById(Long.parseLong(list.get(0).toString())).getOutCode());
                    result.put("num", list.size() == 1 ? "0" : list.get(1).toString());
                    break;
                case SET_CHANNEL_STOCK:
                case BATCH_SET_CHANNEL_STOCK:
                    List<PoushengChannelDTO> dtos = JsonMapper.JSON_NON_EMPTY_MAPPER.getMapper().readValue(list.get(list.size() - 1).toString(), new TypeReference<List<PoushengChannelDTO>>() {
                    });
                    for (PoushengChannelDTO d : dtos) {
                        d.setWarehouseCode(warehouseCacher.findById(d.getWarehouseId()).getOutCode());
                        d.setWarehouseName(warehouseCacher.findById(d.getWarehouseId()).getWarehouseName());
                        d.setOpenShopCode(middleShopCacher.findById(d.getOpenShopId()).getExtra().get("hkPerformanceShopCode"));
                    }
                    result.put("channel_stock", JSON.toJSONString(dtos));
                    break;
                case CREATE_DISPATCH_RULE:
                    result.put("rule", list.get(list.size() - 1).toString());
                    break;
                case UPDATE_DISPATCH_RULE:
                    result.put("shopName", middleShopCacher.findById(Long.parseLong(list.get(0).toString())).getShopName());
                    result.put("rule", list.get(list.size() - 1).toString());
                    break;
                default:
                    log.error("incorrect stock log type");

            }
        } catch (Exception e) {
            log.error("fail to analysis stock log context {}, cause by {}", memberApplicationLog.getMetadata(), e.getMessage());
            result = new HashMap<>();
        }
        dto.setDetail(result);
        return dto;
    }


}
