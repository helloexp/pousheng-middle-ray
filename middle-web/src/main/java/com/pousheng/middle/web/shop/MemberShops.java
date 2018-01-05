/*
 * Copyright (c) 2017. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.shop;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.pousheng.middle.shop.dto.MemberShop;
import com.pousheng.middle.shop.dto.MemberSportCity;
import com.pousheng.middle.shop.dto.PsShop;
import com.pousheng.middle.shop.enums.MemberFromType;
import com.pousheng.middle.web.shop.component.MemberShopOperationLogic;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.common.exception.JsonResponseException;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * @author : songrenfei
 */
@Api(description = "会员中心门店信息API")
@Slf4j
@RestController
public class MemberShops {

    @Autowired
    private MemberShopOperationLogic memberShopOperationLogic;


    /**
     * 根据店铺类型和店铺外码查询店铺信息
     * @param code 店铺外码
     * @param type {@link MemberFromType}
     * @return 店铺信息
     */
    @ApiOperation("根据店铺类型和店铺外码查询店铺信息")
    @GetMapping("/api/ec/member/shop-query")
    public List<PsShop> checkShopExists(@RequestParam String code,
                                        @RequestParam Integer type) {
        if (Strings.isNullOrEmpty(code)) {
            throw new JsonResponseException("code.is.null");
        }
        if (type == null) {
            throw new JsonResponseException("type.is.null");
        }
        List<PsShop> psShops = Lists.newArrayList();
        PsShop psShop = null;
        if (Objects.equal(type, MemberFromType.SHOP.value())||Objects.equal(type, MemberFromType.SHOP_STORE.value())) {
            List<MemberShop> shops = memberShopOperationLogic.findShopByCodeAndType(code,type);
            for (MemberShop shop : shops) {
                psShop = new PsShop(shop.getId(), shop.getStoreFullName(), shop.getStoreCode(), shop.getZoneId(),shop.getZoneName());
                psShops.add(psShop);
            }
        } else {
            List<MemberSportCity> sportCities = memberShopOperationLogic.findSportCityByCode(code);
            for (MemberSportCity sportCity : sportCities) {
                psShop = new PsShop(sportCity.getId(), sportCity.getSportCityFullName(),
                        sportCity.getSportCityCode(), sportCity.getZoneId(),sportCity.getZoneName());
                psShops.add(psShop);
            }
        }
        return psShops;
    }

}
