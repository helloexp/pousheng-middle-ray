package com.pousheng.middle.order.impl.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.common.base.Throwables;
import com.pousheng.middle.order.impl.dao.ShipmentExtDao;
import com.pousheng.middle.order.service.ShipmentReadExtraService;

import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.Shipment;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by will.gong on 2019/04/26
 */
@Service
@Slf4j
@RpcProvider
public class ShipmentReadExtraServiceImpl implements ShipmentReadExtraService {
	private static final Logger log = LoggerFactory.getLogger(ShipmentReadExtraServiceImpl.class);
	private ShipmentExtDao shipmentExtDao;
	
	@Autowired
	public ShipmentReadExtraServiceImpl(ShipmentExtDao shipmentExtDao) {
		this.shipmentExtDao = shipmentExtDao;
	}
	
	@Override
	public Response<List<Shipment>> findByDispatchType(Integer dispatchType) {
		try {
			List<Shipment> shipments = this.shipmentExtDao.findByDispatchType(dispatchType);
			return Response.ok(shipments);
		} catch (Exception var1) {
			log.error("failed to find order shipment(dispatchType={}), cause:{}", dispatchType,
					Throwables.getStackTraceAsString(var1));
			return Response.fail("order.shipment.find.fail");
		}
	}

	@Override
	public Response<List<Shipment>> findBySerialNoAndDispatchType(String shipmentSerialNo, Integer dispatchType) {
		try {
			List<Shipment> shipments = this.shipmentExtDao.findBySerialNoAndDispatchType(shipmentSerialNo, dispatchType);
			return Response.ok(shipments);
		} catch (Exception var2) {
			log.error("failed to find order shipment(shipmentSerialNo={}, dispatchType={}), cause:{}", 
					shipmentSerialNo, dispatchType, Throwables.getStackTraceAsString(var2));
			return Response.fail("order.shipment.find.fail");
		}
	}

	@Override
	public Response<List<Shipment>> findByWHNameAndWHOutCodeWithShipmentIds(String warehouseNameOrOutCode, List<Long> shipmentIds) {
		try {
			List<Shipment> shipments = this.shipmentExtDao.findByWHNameAndWHOutCodeWithShipmentIds(warehouseNameOrOutCode, shipmentIds);
			return Response.ok(shipments);
		} catch (Exception var3) {
			log.error("failed to find order shipment(warehouseNameOrOutCode={}, shipmentIds={}, cause{}:",
					warehouseNameOrOutCode, shipmentIds);
			return Response.fail("order.shipment.find.fail");
		}
	}

	@Override
	public Response<List<Shipment>> findByWHNameAndWHOutCodeWithShipmentIds(String warehouseNameOrOutCode) {
		try {
			List<Shipment> shipments = this.shipmentExtDao.findByWHNameAndWHOutCodeWithShipmentIds(warehouseNameOrOutCode);
			return Response.ok(shipments);
		} catch (Exception var3) {
			log.error("failed to find order shipment(warehouseNameOrOutCode={}, cause{}:",
					warehouseNameOrOutCode);
			return Response.fail("order.shipment.find.fail");
		}
	}

}
