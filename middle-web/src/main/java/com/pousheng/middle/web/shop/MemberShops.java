/*
 * Copyright (c) 2017. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.shop;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.middle.shop.dto.MemberShop;
import com.pousheng.middle.shop.dto.MemberSportCity;
import com.pousheng.middle.shop.dto.PsShop;
import com.pousheng.middle.shop.enums.MemberFromType;
import com.pousheng.middle.web.shop.component.MemberShopOperationLogic;
import com.pousheng.middle.web.shop.dto.Zone;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.user.ext.UserTypeBean;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static com.pousheng.middle.constants.Constants.MANAGE_ZONE_IDS;


/**
 * @author : songrenfei
 */
@Api(description = "会员中心门店信息API")
@Slf4j
@RestController
public class MemberShops {

    @Autowired
    private MemberShopOperationLogic memberShopOperationLogic;
    @Autowired
    private UserTypeBean userTypeBean;


    /**
     * ps:中台只会用到店铺类型
     * 根据店铺类型和店铺外码查询店铺信息
     * @param code 店铺外码
     * @param type {@link MemberFromType}
     * @return 店铺信息
     */
    @ApiOperation("根据店铺类型和店铺外码查询店铺信息")
    @GetMapping("/api/ec/member/shop-query")
    public List<PsShop> checkShopExists(@RequestParam String code,
                                        @RequestParam(required = false,defaultValue = "1") Integer type) {

        ParanaUser paranaUser = UserUtil.getCurrentUser();

        if (Strings.isNullOrEmpty(code)) {
            throw new JsonResponseException("code.is.null");
        }
        if (type == null) {
            throw new JsonResponseException("type.is.null");
        }
        List<PsShop> psShops = Lists.newArrayList();
        String zoneIds = null;

        if (!userTypeBean.isAdmin(paranaUser)) {
            Map<String, String> extraMap = paranaUser.getExtra();
            zoneIds = extraMap.get(MANAGE_ZONE_IDS);
        }

        PsShop psShop = null;
        if (Objects.equal(type, MemberFromType.SHOP.value())||Objects.equal(type, MemberFromType.SHOP_STORE.value())) {
            List<MemberShop> shops = memberShopOperationLogic.findShopByCodeAndTypeAndZoneId(code,type,zoneIds);
            for (MemberShop shop : shops) {
                psShop = new PsShop(shop.getId(), shop.getId(),shop.getStoreFullName(), shop.getStoreCode(),
                        shop.getCompanyId(),shop.getCompanyName(),shop.getZoneId(),shop.getZoneName(),shop.getTelphone(),shop.getEmail(),shop.getAddress());
                psShops.add(psShop);
            }
        } else {
            List<MemberSportCity> sportCities = memberShopOperationLogic.findSportCityByCode(code);
            for (MemberSportCity sportCity : sportCities) {
                psShop = new PsShop(sportCity.getId(),sportCity.getId(), sportCity.getSportCityFullName(),
                        sportCity.getSportCityCode(), sportCity.getCompanyId(),sportCity.getCompanyName(),
                        sportCity.getZoneId(),sportCity.getZoneName(),
                        sportCity.getTelphone(),sportCity.getEmail(),sportCity.getAddress());
                psShops.add(psShop);
            }
        }
        return psShops;
    }



    @ApiOperation("区部查询")
    @GetMapping("/api/member/search/zone")
    public List<Zone> searchZone() {
        List<Zone> memberLevels = Lists.newArrayList();
        Map<String, String> criteria = Maps.newHashMap();
        int pageNo = 1;
        criteria.put("pageSize", "50");
        while (true) {
            criteria.put("pageNo", String.valueOf(pageNo));
            Response<Paging<Zone>> resp = memberShopOperationLogic.findZone(criteria);
            if (!resp.isSuccess()) {
                log.error("failed to search member zone by criteria = {}, cause: {}", criteria, resp.getError());
                throw new JsonResponseException(resp.getError());
            }
            Paging<Zone> paging = resp.getResult();
            if (paging.getData().isEmpty()) {
                break;
            }
            memberLevels.addAll(paging.getData());
            pageNo++;
        }
        return memberLevels;
    }

}
