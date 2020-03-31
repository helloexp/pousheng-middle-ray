package com.pousheng.middle.order.service;

import java.util.List;

import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Shipment;

public interface ShipmentReadExtraService {

	Response<List<Shipment>> findByDispatchType(Integer var1);

	Response<List<Shipment>> findBySerialNoAndDispatchType(String var1, Integer var2);

	Response<List<Shipment>> findByWHNameAndWHOutCodeWithShipmentIds(String var1, List<Long> var2);

	Response<List<Shipment>> findByWHNameAndWHOutCodeWithShipmentIds(String var1);
}
