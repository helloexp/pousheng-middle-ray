package com.pousheng.middle.web.order;

import com.pousheng.middle.hksyc.component.SyncYunJuJitShipmentApi;
import com.pousheng.middle.hksyc.dto.YJSyncShipmentRequest;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("api/jit/shipment")
public class JitShipmentController {

    @Autowired
    private SyncYunJuJitShipmentApi syncYunJuJitShipmentApi;

    public static final JsonMapper MAPPER=JsonMapper.JSON_NON_EMPTY_MAPPER;
    /**
     *
     * 发货回执云聚修复数据
     * @param request
     */
    @RequestMapping(value = "/fix/callback", method = RequestMethod.GET)
    public void callbackJitShipment(String request,Long shopId) {

        YJSyncShipmentRequest data=MAPPER.fromJson(request,YJSyncShipmentRequest.class);
        syncYunJuJitShipmentApi.doSyncShipmentOrder(data,shopId);
    }

}
