package com.pousheng.middle.web.shop.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.pousheng.middle.gd.Location;
import com.pousheng.middle.order.dispatch.component.DispatchComponent;
import com.pousheng.middle.order.enums.AddressBusinessType;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.shop.dto.MemberShop;
import com.pousheng.middle.shop.dto.MemberSportCity;
import com.pousheng.middle.shop.enums.MemberFromType;
import com.pousheng.middle.warehouse.cache.WarehouseAddressCacher;
import com.pousheng.middle.warehouse.model.WarehouseAddress;
import com.pousheng.middle.web.shop.dto.MemberCenterAddressDto;
import com.pousheng.middle.web.shop.dto.Zone;
import com.pousheng.middle.web.user.component.MemberCenterClient;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员中心店铺操作逻辑
 * @author songrenfei
 */
@Slf4j
@Component
public class MemberShopOperationLogic {

    private static final Long duration = 12L;

    @Autowired
    private MemberCenterClient mcClient;
    @Autowired
    private DispatchComponent dispatchComponent;
    @Autowired
    private WarehouseAddressCacher warehouseAddressCacher;


    private List<MemberSportCity> findSportCities(String code) {
        Response<List<MemberSportCity>> resp = findSrvSportCityByCode(code);
        if (!resp.isSuccess()) {
            log.error("find sportCity failed, code = {}, cause: {}", code, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        return resp.getResult();
    }

    private List<MemberShop> findShops(String code) {
        Response<List<MemberShop>> resp = findSrvShopByCode(code);
        if (!resp.isSuccess()) {
            log.error("find shop failed, code = {}, cause: {}", code, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        return resp.getResult();
    }


    /**
     * 区部查询
     *
     * @param criteria 查询条件
     * @return 区部信息
     */
    public Response<Paging<Zone>> findZone(Map<String, String> criteria) {
        try {
            String result = mcClient.doGet("/api/member/pousheng/zone/list", criteria);
            Paging paging = convertStringToPaging(result);
            String listData = convertListToString(paging.getData());
            return Response.ok(new Paging<>(paging.getTotal()
                    , jsonToObject(listData, new TypeReference<List<Zone>>() {})));
        }catch (Exception e) {
            log.error("failed to find zone by criteria = {}, cause: {}"
                    , criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("find.zone.failed");
        }
    }


    /**
     * 服务店铺查询
     *
     * @param code 查询条件
     * @return 店铺信息
     */
    private Response<List<MemberShop>> findSrvShopByCode(String code) {
        try {
            String result = mcClient.doGet("/api/member/pousheng/shop/store-code/" + code, null);
            if (Strings.isNullOrEmpty(result)) {
                return Response.ok(Collections.emptyList());
            }
            return Response.ok(jsonToObject(result, new TypeReference<List<MemberShop>>() {}));
        } catch (Exception e) {
            log.error("failed to find shop by criteria = {}, cause: {}", code, Throwables.getStackTraceAsString(e));
            return Response.fail("find.shop.failed");
        }
    }

    /**
     * 服务运动城查询
     *
     * @param code 查询条件
     * @return 运动城信息
     */
    private Response<List<MemberSportCity>> findSrvSportCityByCode(String code) {
        try {
            String result = mcClient.doGet("/api/member/pousheng/sport-city/sport-city-code/" + code, null);
            if (Strings.isNullOrEmpty(result)) {
                return Response.ok(Collections.emptyList());
            }
            return Response.ok(jsonToObject(result, new TypeReference<List<MemberSportCity>>() {}));
        } catch (Exception e) {
            log.error("failed to find sport city by code = {}, cause: {}", code, Throwables.getStackTraceAsString(e));
            return Response.fail("find.sport.city.failed");
        }
    }


    /**
     * 查询门店地址
     *
     * @param companyId 查询条件
     * @param storeCode 查询条件
     * @param type 查询条件{@link MemberFromType}
     * @return 门店地址信息
     */
    public Response<MemberCenterAddressDto> findShopAddress(String companyId, String storeCode, Integer type) {
        try {
            Map<String, String> criteria = new HashMap<>();
            criteria.put("companyId",companyId);
            criteria.put("storeCode",storeCode);
            criteria.put("type",type.toString());

            String result = mcClient.doGet("/api/member/pousheng/area/get-area-info",criteria);
            if (Strings.isNullOrEmpty(result)) {
                return Response.fail("not.find.address.info");
            }
            return Response.ok(jsonToObject(result, new TypeReference<MemberCenterAddressDto>() {}));
        } catch (Exception e) {
            log.error("failed to find shop address by company id:{} code:{} type:{}, cause: {}", companyId,storeCode,type, Throwables.getStackTraceAsString(e));
            return Response.fail("find.member.center.shop.address.failed");
        }
    }


    private Paging convertStringToPaging(String data) throws IOException {
        return JsonMapper.JSON_NON_DEFAULT_MAPPER.getMapper().readValue(data, Paging.class);
    }

    private String convertListToString(List data) throws JsonProcessingException {
        return JsonMapper.JSON_NON_DEFAULT_MAPPER.getMapper().writeValueAsString(data);
    }

    private <T> T jsonToObject(String data, TypeReference<T> tRef) throws IOException {
        return JsonMapper.JSON_NON_DEFAULT_MAPPER.getMapper().readValue(data, tRef);
    }

    public List<MemberSportCity> findSportCityByCode(String code) {
        Map<String, String> criteria = new HashMap<>();
        criteria.put("sportCityCode", code);
        Integer pageNo = 1;
        criteria.put("pageNo", pageNo+"");
        criteria.put("openStateList", "0,1,2");
        Response<Paging<MemberSportCity>> resp = findSrvSportCity(criteria);
        if (!resp.isSuccess()) {
            log.error("find SportCity failed, criteria = {}, cause: {}", criteria, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        Paging<MemberSportCity> paging = resp.getResult();
        return paging.getData();
    }

    /**
     * Modify by lt 增加店铺类型判断 types 1:店铺 3:店柜
     */
    public List<MemberShop> findShopByCodeAndTypeAndZoneId(String code,Integer type,String zoneIds) {
        Map<String, String> criteria = new HashMap<>();
        criteria.put("storeCode", code);
        Integer pageNo = 1;
        criteria.put("pageNo", pageNo+"");
        criteria.put("openStateList", "0,1,2");
        if(!Strings.isNullOrEmpty(zoneIds)){
            criteria.put("zoneIds", zoneIds);
        }
        criteria.put("types", type+"");
        Response<Paging<MemberShop>> resp = findSrvShop(criteria);
        if (!resp.isSuccess()) {
            log.error("find shop failed, criteria = {}, cause: {}", criteria, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        Paging<MemberShop> paging = resp.getResult();
        return paging.getData();
    }

    /**
     * 查询店铺信息
     * @param code  外码
     * @param type  类型
     * @param companyId 公司ID
     */
    public Optional<MemberShop> findShopByCodeAndType(String code,Integer type,String companyId) {
        Map<String, String> criteria = new HashMap<>();
        criteria.put("storeCode", code);
        Integer pageNo = 1;
        criteria.put("pageNo", pageNo+"");
        criteria.put("openStateList", "0,1,2");
        criteria.put("types", type+"");
        criteria.put("companyId",companyId);
        Response<Paging<MemberShop>> resp = findSrvShop(criteria);
        if (!resp.isSuccess()) {
            log.error("find shop failed, criteria = {}, cause: {}", criteria, resp.getError());
            throw new JsonResponseException(resp.getError());
        }
        Paging<MemberShop> paging = resp.getResult();
        if(Objects.equal(paging.getTotal(),0L)){
            return Optional.absent();
        }
        return Optional.of(paging.getData().get(0));
    }



    public AddressGps getAddressGps(Long shopId, String companyId, String storeCode){
        //1、调用会员中心查询门店地址
        Response<MemberCenterAddressDto> addressDtoRes =  this.findShopAddress(companyId,storeCode, MemberFromType.SHOP.value());
        if(!addressDtoRes.isSuccess()){
            log.error("find shop address by company id:{} code:{} type:{} fail,error:{}",companyId,storeCode,MemberFromType.SHOP.value(),addressDtoRes.getError());
            return null;
        }

        MemberCenterAddressDto addressDto = addressDtoRes.getResult();
        if(Arguments.isNull(addressDto)){
            log.error("not find shop address by company id:{} code:{} type:{}",companyId,storeCode,MemberFromType.SHOP.value());
            return null;
        }
        if(Strings.isNullOrEmpty(addressDto.getAddress())){
            log.error("shop address is null for company id:{} code:{} type:{}",companyId,storeCode,MemberFromType.SHOP.value());
            return null;
        }


        //2、调用高德地图查询地址坐标
        Optional<Location> locationOp = dispatchComponent.getLocation(addressDto.getAddress());
        Location location;
        if(!locationOp.isPresent()){
            log.error("[ADDRESS-LOCATION]:not find shop(id:{}) location by address:{}",shopId,addressDto.getAddress());

            String addressRegion = addressDto.getProvinceName() + addressDto.getCityName() + addressDto.getAreaName();
            //如果根据详细地址查询不到则用粗粒度的地址
            Optional<Location>  locationRegionOp = dispatchComponent.getLocation(addressRegion);
            if(!locationRegionOp.isPresent()){
                log.error("not find location by address:{} for shop id:{}",addressRegion,shopId);
                throw new ServiceException("shop.address.info.invalid");
            }
            location = locationRegionOp.get();
        } else {
            location= locationOp.get();
        }


        //3、创建门店地址定位信息
        AddressGps addressGps = new AddressGps();
        addressGps.setLatitude(location.getLat());
        addressGps.setLongitude(location.getLon());
        addressGps.setBusinessId(shopId);
        addressGps.setBusinessType(AddressBusinessType.SHOP.getValue());
        addressGps.setDetail(addressDto.getAddress());
        //省

        log.info("[START-TRANS-ADDRESS] addressDto:{}",addressDto);
        WarehouseAddress province = transToWarehouseAddress(addressDto.getProvinceCode(),addressDto.getProvinceName());
        if(Arguments.isNull(province)){
            log.error("not find middle province by code:{} and name:{}",addressDto.getProvinceCode(),addressDto.getProvinceName());
            return null;
        }
        addressGps.setProvinceId(province.getId());
        addressGps.setProvince(province.getName());

        //市
        WarehouseAddress city = transToWarehouseAddress(addressDto.getCityCode(),addressDto.getCityName());
        if(Arguments.isNull(city)){
            log.error("not find middle city by code:{} and name:{}",addressDto.getCityCode(),addressDto.getCityName());
            //如果根据id 查询不到则用pid和name查询
            city = transToWarehouseAddress(province.getId(),addressDto.getCityName());
            if(Arguments.isNull(city)){
                log.error("not find middle city by pid:{} and name:{}",province.getId(),addressDto.getCityName());
                return null;
            }
        }
        addressGps.setCityId(city.getId());
        addressGps.setCity(city.getName());

        //区
        WarehouseAddress region = transToWarehouseAddress(addressDto.getAreaCode(),addressDto.getAreaName());
        if(Arguments.isNull(region)){
            log.error("not find middle region by code:{} and name:{}",addressDto.getAreaCode(),addressDto.getAreaName());
            //如果根据id 查询不到则用pid和name查询
            region = transToWarehouseAddress(city.getId(),addressDto.getAreaName());
            if(Arguments.isNull(region)){
                log.error("not find middle region by pid:{} and name:{}",city.getId(),addressDto.getAreaName());
                return null;
            }
        }
        addressGps.setRegionId(region.getId());
        addressGps.setRegion(region.getName());

        return addressGps;
    }


    /**
     * 服务店铺查询
     *
     * @param criteria 查询条件
     * @return 店铺信息
     */
    private Response<Paging<MemberShop>> findSrvShop(Map<String, String> criteria) {
        try {
            String result = mcClient.doGet("/api/member/pousheng/shop/list", criteria);
            if (Strings.isNullOrEmpty(result)) {
                return Response.ok(Paging.empty());
            }
            Paging paging = convertStringToPaging(result);
            String listData = convertListToString(paging.getData());
            return Response.ok(new Paging<>(paging.getTotal()
                    , jsonToObject(listData, new TypeReference<List<MemberShop>>() {})));
        }catch (Exception e) {
            log.error("failed to find shop by criteria = {}, cause: {}"
                    , criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("find.shop.failed");
        }
    }

    /**
     * 服务运动城查询
     *
     * @param criteria 查询条件
     * @return 运动城信息
     */
    private Response<Paging<MemberSportCity>> findSrvSportCity(Map<String, String> criteria) {
        try {
            String result = mcClient.doGet("/api/member/pousheng/sport-city/list", criteria);
            if (Strings.isNullOrEmpty(result)) {
                return Response.ok(Paging.empty());
            }
            Paging paging = convertStringToPaging(result);
            String listData = convertListToString(paging.getData());
            return Response.ok(new Paging<>(paging.getTotal()
                    , jsonToObject(listData, new TypeReference<List<MemberSportCity>>() {})));
        }catch (Exception e) {
            log.error("failed to find sport city by criteria = {}, cause: {}"
                    , criteria, Throwables.getStackTraceAsString(e));
            return Response.fail("find.sport.city.failed");
        }
    }

    //将会员中心的地址与中台地址做比较，转换为中台的地址
    private WarehouseAddress transToWarehouseAddress(String code,String name){
        try {
            if(Strings.isNullOrEmpty(code)){
                log.error("[QUERY-MIDDLE-ADDRESS] code is null",code,name);
                return null;
            }
            Long addressId = Long.valueOf(code);
            WarehouseAddress address = warehouseAddressCacher.findById(addressId);
            if(Arguments.isNull(address)){
                log.error("[QUERY-MIDDLE-ADDRESS] by code:{} name:{} not find",code,name);
                return null;
            }
            return address;
        }catch (ServiceException e){
            log.error("check is matching warehouse address by code:{} name:{} fail,error:{}",code,name, Throwables.getStackTraceAsString(e));
            return null;
        }catch (Exception e){
            log.error("check is matching warehouse address by code:{} name:{} fail,cause:{}",code,name, Throwables.getStackTraceAsString(e));
            return null;
        }

    }


    //将会员中心的地址与中台地址做比较，转换为中台的地址
    private WarehouseAddress transToWarehouseAddress(Long pid,String name){
        try {
            Optional<WarehouseAddress> addressOptional = warehouseAddressCacher.findByPidAndName(pid,name);
            if(!addressOptional.isPresent()){
                log.error("[QUERY-MIDDLE-ADDRESS] by pid:{} name:{} not find",pid,name);
                return null;
            }
            return addressOptional.get();
        }catch (ServiceException e){
            log.error("check is matching warehouse address by pid:{} name:{} fail,error:{}",pid,name, Throwables.getStackTraceAsString(e));
            return null;
        }catch (Exception e){
            log.error("check is matching warehouse address by pid:{} name:{} fail,cause:{}",pid,name, Throwables.getStackTraceAsString(e));
            return null;
        }

    }

}
