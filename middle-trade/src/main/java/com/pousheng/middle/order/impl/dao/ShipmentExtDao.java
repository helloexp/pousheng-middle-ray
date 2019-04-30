package com.pousheng.middle.order.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.util.ShipmentEncryptUtil;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tanlongjun
 */
@Repository
public class ShipmentExtDao extends MyBatisDao<Shipment> {


    public Paging<Shipment> pagingExt(Integer offset, Integer limit, Map<String, Object> criteria) {
        if (criteria == null) {
            criteria = Maps.newHashMap();
        }

        Long total = (Long)this.sqlSession.selectOne(this.sqlId("countExt"), criteria);
        if (total <= 0L) {
            return new Paging(0L, Collections.emptyList());
        } else {
            ((Map)criteria).put("offset", offset);
            ((Map)criteria).put("limit", limit);
            List<Shipment> datas = this.sqlSession.selectList(this.sqlId("pagingExt"), criteria);
            return new Paging(total, datas);
        }
    }

    /**
     * 能指定排序规则的分页查询
     * @param offset
     * @param limit
     * @param sort
     * @param criteria
     * @return
     */
    public Paging<Shipment> paging(Integer offset, Integer limit,String sort,Map<String, Object> criteria) {
        if (criteria == null) {
            criteria = Maps.newHashMap();
        }
        if(StringUtils.isNotBlank(sort)){
            criteria.put("sort",sort);
        }
        criteria.put("offset", offset);
        criteria.put("limit", limit);
        Long total = (Long)this.sqlSession.selectOne(this.sqlId("count"), criteria);
        if (total <= 0L) {
            return new Paging(0L, Collections.emptyList());
        } else {
            List<Shipment> datas = this.sqlSession.selectList(this.sqlId("pagingSort"), criteria);
            return new Paging(total, datas);
        }
    }

    /**
     * 根据派单类型查询
     * @param dispatchType
     * @return
     */
	public List<Shipment> findByDispatchType(Integer dispatchType) {
		return this.getSqlSession().selectList(this.sqlId("findByDispatchType"), dispatchType);
	}

	/**
     * 根据快递单号及派单类型查询
	 * @param shipmentSerialNo
	 * @param dispatchType
	 * @return
	 */
	public List<Shipment> findBySerialNoAndDispatchType(String shipmentSerialNo, Integer dispatchType) {
		List<Shipment> shipments = this.getSqlSession().selectList(this.sqlId("findBySerialNoAndDispatchType"), 
				ImmutableMap.of("shipmentSerialNo", shipmentSerialNo, "dispatchType", dispatchType));
		if (CollectionUtils.isNotEmpty(shipments)) {
			Iterator var3 = shipments.iterator();
			while (var3.hasNext()) {
				Shipment shipment = (Shipment) var3.next();
				ShipmentEncryptUtil.decryptReceiverInfo(shipment);
			}
		}
		return shipments;
	}

	/**
	 * 根据仓库名称  or 外部编码及发货单ids查询
	 * @param warehouseName
	 * @param warehouseOutCode
	 * @param shipmentIds
	 * @return
	 */
	public List<Shipment> findByWHNameAndWHOutCodeWithShipmentIds(String warehouseNameOrOutCode, List<Long> shipmentIds) {
		List<Shipment> shipments = this.getSqlSession().selectList(this.sqlId("findByWHNameAndWHOutCodeWithShipmentIds"), 
				ImmutableMap.of("warehouseNameOrOutCode", warehouseNameOrOutCode, "shipmentIds", shipmentIds));
		if (CollectionUtils.isNotEmpty(shipments)) {
			Iterator var5 = shipments.iterator();
			while (var5.hasNext()) {
				Shipment shipment = (Shipment)var5.next();
				ShipmentEncryptUtil.decryptReceiverInfo(shipment);
			}
		}
		return shipments;
	}
	
	/**
	 * 根据仓库名称  or 外部编码查询
	 * @param warehouseName
	 * @param warehouseOutCode
	 * @param shipmentIds
	 * @return
	 */
	public List<Shipment> findByWHNameAndWHOutCodeWithShipmentIds(String warehouseNameOrOutCode) {
		List<Shipment> shipments = this.getSqlSession().selectList(this.sqlId("findByWHNameAndWHOutCodeWithShipmentIds"), 
				ImmutableMap.of("warehouseNameOrOutCode", warehouseNameOrOutCode));
		if (CollectionUtils.isNotEmpty(shipments)) {
			Iterator var5 = shipments.iterator();
			while (var5.hasNext()) {
				Shipment shipment = (Shipment)var5.next();
				ShipmentEncryptUtil.decryptReceiverInfo(shipment);
			}
		}
		return shipments;
	}
}
