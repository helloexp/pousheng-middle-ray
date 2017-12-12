package com.pousheng.middle.web.shop.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Throwables;
import com.pousheng.middle.shop.dto.MemberShop;
import com.pousheng.middle.shop.dto.MemberSportCity;
import com.pousheng.middle.web.user.component.MemberCenterClient;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
        criteria.put("status", 1+"");
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
    public List<MemberShop> findShopByCodeAndType(String code,Integer type) {
        Map<String, String> criteria = new HashMap<>();
        criteria.put("storeCode", code);
        Integer pageNo = 1;
        criteria.put("pageNo", pageNo+"");
        criteria.put("status", 1+"");
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

}
