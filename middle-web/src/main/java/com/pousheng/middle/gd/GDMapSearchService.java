/*
 * Copyright (c) 2017. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.gd;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.common.utils.Splitters;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author : panxin
 */
@Slf4j
@Service
public class GDMapSearchService {

    private final GDMapToken mapToken;

    @Autowired
    public GDMapSearchService(GDMapToken mapToken) {
        this.mapToken = mapToken;
    }

    /**
     * 根据地址信息查询经纬度, 可能高德地图查询不到该地址.
     *
     * @param address 地址信息
     * @return 经纬度信息
     */
    public Response<Optional<Location>> searchByAddress(String address) {
        try {
            if (Strings.isNullOrEmpty(address)) {
                log.warn("address is empty, address = {}", address);
                return Response.ok(Optional.absent());
            }
            // 请求参数
            GDSearchRequestParams params = new GDSearchRequestParams();
            params.setKey(mapToken.getWebKey());
            params.setKeywords(address);
            params.setOffset(1);
            params.setPage(1);
            params.setExtensions(GDSearchRequestParams.Extensions.ALL.key());

            HttpRequest request = HttpRequest.get(mapToken.getSearchApi(), params.toMap(), true);
            if (!request.ok()) {
                log.error("request amap search failed, cause: {}", request.body());
                return Response.fail("request.amap.failed");
            }

            String result = request.body();
            MapSearchResponse response = JsonMapper.nonEmptyMapper().fromJson(result, MapSearchResponse.class);
            if(Arguments.isNull(response)){
                log.error("amap response poi is empty, resp = {}", result);
                return Response.fail("amap.response.status.failed");
            }
            if (Objects.equal(response.getStatus(), MapSearchResponse.Status.FAILED.value())) {
                log.error("amap response poi is empty, resp = {}", result);
                return Response.fail("amap.response.status.failed");
            }
            Location location = null;
            if (Arguments.notEmpty(response.getPois())) {
                // amap 返回的数据格式: 121.387066,31.107124
                List<String> locations = Splitters.COMMA.splitToList(response.getPois().get(0).getLocation());
                if (Arguments.notEmpty(locations) && locations.size() >= 2) {
                    location = new Location();
                    location.setLon(locations.get(0));
                    location.setLat(locations.get(1));
                    location.setProvinceId(response.getPois().get(0).getPcode());
                    location.setRegionId(response.getPois().get(0).getAdcode());
                }
            }
            return Response.ok(Optional.fromNullable(location));
        }catch (Exception e) {
            log.error("failed to request amap by address = {}, cause: {}"
                    , address, Throwables.getStackTraceAsString(e));
            return Response.fail("request.amap.failed");
        }
    }

}
