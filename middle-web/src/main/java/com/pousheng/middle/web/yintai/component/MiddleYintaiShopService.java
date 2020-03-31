package com.pousheng.middle.web.yintai.component;

import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.yintai.YintaiConstant;
import com.pousheng.middle.web.yintai.dto.YintaiShop;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.open.client.center.MappingServiceRegistryCenter;
import io.terminus.open.client.common.mappings.model.ShopCounterMapping;
import io.terminus.open.client.common.mappings.service.OpenClientShopCounterMappingService;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 银泰店铺
 * AUTHOR: zhangbin
 * ON: 2019/6/24
 */
@Service
@Slf4j
public class MiddleYintaiShopService {

    @Autowired
    private OpenShopReadService openShopReadService;
    @Autowired
    private MappingServiceRegistryCenter mappingCenter;
    @Autowired
    private WarehouseCacher warehouseCacher;

    private OpenClientShopCounterMappingService shopCounterService;

    private OpenClientShopCounterMappingService getShopCounterService() {
        if (shopCounterService == null) {
            shopCounterService = mappingCenter.getShopCounterService(MiddleChannel.YINTAI.getValue());
        }
        return shopCounterService;
    }
    /**
     * 中台与银泰映射数据
     * @param shopType
     * @return
     */
    public List<List<String>> getShopMapping(String shopType) {
        if (YintaiConstant.SHOP_MAPPING.equals(shopType)) {
            return getShopList().stream().map(shop -> {
                List<String> row = Lists.newArrayList();
                row.add(shop.getCompanyCode());
                row.add(shop.getShopOutCode());
                row.add(shop.getShopName());
                row.add(shop.getChannelShopId());
                return row;
            }).collect(Collectors.toList());
        }
        return getShopList().stream().map(yintaiShop->{
            List<String> row = Lists.newArrayList();
            row.add(yintaiShop.getCompanyCode());//公司码
            row.add(yintaiShop.getShopOutCode());//店铺外码
            row.add(yintaiShop.getChannelShopId());//银泰专柜
            return row;
        }).collect(Collectors.toList());
    }

    public List<YintaiShop> getShopList() {
        Response<List<ShopCounterMapping>> mappingResp = getShopCounterService().findByChannel(MiddleChannel.YINTAI.getValue());
        if (!mappingResp.isSuccess()) {
            log.error("find yintai shop counter mapping fail, error:({})", mappingResp.getError());
            return Collections.emptyList();
        }

        return mappingResp.getResult().stream()
                .map(mapping->{
                    Map<String, String> extra = mapping.getExtra();
                    YintaiShop yintaiShop = new YintaiShop();
                    yintaiShop.setShopName(extra.get(YintaiConstant.COMPANY_NAME));
                    yintaiShop.setCompanyCode(extra.get(YintaiConstant.COMPANY_CODE));
                    yintaiShop.setShopOutCode(extra.get(YintaiConstant.SHOP_OUT_CODE));
                    yintaiShop.setChannelShopId(mapping.getCounterId());
                    return yintaiShop;
                }).collect(Collectors.toList());
    }

    /**
     * 获取有店铺和店柜映射关系的店仓列表
     * @param shopId 店铺id
     * @return 仓库ids
     */
    public List<WarehouseDTO> getShopWarehouses(Long shopId) {
        Response<List<ShopCounterMapping>> mappingResp = getShopCounterService().findByOpenShopId(shopId);
        if (!mappingResp.isSuccess()) {
            log.error("fail to find yintai shop counter mapping, shopId:({}), error:({})", shopId, mappingResp.getError());
            throw new ServiceException("shop.warehouse.fail");
        }

        List<WarehouseDTO> warehouseIds = Lists.newArrayList();
        for (ShopCounterMapping shopCounterMapping : mappingResp.getResult()) {
            try {
                if (Strings.isNullOrEmpty(shopCounterMapping.getCounterId())) {
                    continue;
                }
                Map<String, String> extra = shopCounterMapping.getExtra();
                String shopOutCode = extra.get(YintaiConstant.SHOP_OUT_CODE);
                String companyCode = extra.get(YintaiConstant.COMPANY_CODE);
                WarehouseDTO warehouseDTO = warehouseCacher.findByOutCodeAndBizId(shopOutCode, companyCode);
                if (warehouseDTO == null) {
                    log.error("fail to find shop warehouse， shopOutCode({}), companyCode({})", shopOutCode, companyCode);
                    continue;
                }
                warehouseDTO.setCounterId(shopCounterMapping.getCounterId());
                warehouseIds.add(warehouseDTO);
            } catch (Exception e) {
                log.error("shopCounterMapping({}) cause:({})",shopCounterMapping, e);
            }
        }
        return warehouseIds;
    }
}
