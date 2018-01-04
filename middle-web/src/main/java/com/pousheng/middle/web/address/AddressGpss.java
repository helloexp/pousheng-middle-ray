package com.pousheng.middle.web.address;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.pousheng.middle.order.cache.AddressGpsCacher;
import com.pousheng.middle.order.enums.AddressBusinessType;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.order.service.AddressGpsReadService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.ShopReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by songrenfei on 2017/12/15
 */
@Api(description = "门店或仓库地址定位信息API")
@RestController
@Slf4j
public class AddressGpss {

    @Autowired
    private AddressGpsCacher addressGpsCacher;
    @RpcConsumer
    private AddressGpsReadService addressGpsReadService;
    @RpcConsumer
    private ShopReadService shopReadService;

    /**
     * 获取某一个区内的门店
     *
     * @param regionId 区id
     * @return 门店信息
     */
    @ApiOperation("获取某一个区内的门店")
    @RequestMapping(value = "/api/middle/region/{id}/shops", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Shop> findShopByRegionId(@PathVariable("id") Long regionId) {

        Response<List<AddressGps>> gpsRes = addressGpsReadService.findByRegionIdAndBusinessType(regionId, AddressBusinessType.SHOP);
        if(!gpsRes.isSuccess()){
            throw new JsonResponseException(gpsRes.getError());
        }
        List<Long> shopIds = Lists.transform(gpsRes.getResult(), new Function<AddressGps, Long>() {
            @Nullable
            @Override
            public Long apply(@Nullable AddressGps input) {
                return input.getBusinessId();
            }
        });

        Response<List<Shop>> shopsRes = shopReadService.findByIds(shopIds);
        if(!shopsRes.isSuccess()){
            throw new JsonResponseException(shopsRes.getError());
        }
        List<Shop> shops = shopsRes.getResult().stream().filter(shop -> !Objects.equals(shop.getStatus(),-2)).collect(Collectors.toList());
        return shops;

    }

    /**
     * 获取门店地址
     * @param shopId 门店id
     * @return 门店地址信息
     */
    @ApiOperation("获取门店地址信息")
    @RequestMapping(value = "/api/middle/shop/{id}/address", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<AddressGps> findGpsByShopId(@PathVariable("id") Long shopId) {
        return addressGpsReadService.findByBusinessIdAndType(shopId, AddressBusinessType.SHOP);
    }

}
