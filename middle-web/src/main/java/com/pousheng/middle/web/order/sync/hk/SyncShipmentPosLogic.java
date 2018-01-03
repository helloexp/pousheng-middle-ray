package com.pousheng.middle.web.order.sync.hk;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * 同步恒康发货单逻辑 for 开pos单
 * Created by songrenfei on 2017/6/27
 */
@Slf4j
@Component
public class SyncShipmentPosLogic {

    private static final ObjectMapper objectMapper = JsonMapper.nonEmptyMapper().getMapper();
    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();
    private static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    /**
     * 同步发货单到恒康
     *
     * @param shipment 发货单
     * @return 同步成功true, 同步失败false
     */
    public Response<Boolean> syncShipmentPosToHk(Shipment shipment) {
        try {
            return Response.ok();
        } catch (Exception e) {
            log.error("sync hk shipment failed,shipmentId is({}) cause by({})", shipment.getId(), e.getMessage());
            return Response.fail("sync.hk.shipment.fail");
        }

    }


}
