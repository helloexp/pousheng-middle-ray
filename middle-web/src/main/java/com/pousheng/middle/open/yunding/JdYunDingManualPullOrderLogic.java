package com.pousheng.middle.open.yunding;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import com.pousheng.middle.order.dto.ManualPullOrderRequest;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.OpenClientException;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Author: bernie
 * Date: 2019/5/17
 */
@Component
@Slf4j
public class JdYunDingManualPullOrderLogic {

    @Autowired
    private JdYunDingUtils jdYunDingUtils;

    public Response<Boolean> manualPullOrderFromYunding(ManualPullOrderRequest request) {
        try {

            SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //调用云鼎拉单接口
            HashMap params = Maps.newHashMap();
            params.put("openShopId", request.getOpenShopId());
            if(Objects.nonNull(request.getPullStartDate())){
                params.put("pullStartTime", request.getPullStartDate());
            }
            if(Objects.nonNull(request.getPullStartDate())){
                params.put("pullEndTime", request.getPullEndDate());
            }
            params.put("outerOrderId", request.getOuterOrderId());
            params.put("orderCategory", request.getOrderCategory());
            String result = jdYunDingUtils.post(request.getOpenShopId(), "jdyd.order.pull.api", params);
            if (!result.contains("success")) {
                log.error("manual pull order from yun ding failed params:{} fail, request result:{}", params, result);
                return Response.fail(result);
            }
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("manual pull order from yun ding failed, caused by {}", Throwables.getStackTraceAsString(e));
            return Response.fail("push.yunding.order.failed");
        }
    }



}
