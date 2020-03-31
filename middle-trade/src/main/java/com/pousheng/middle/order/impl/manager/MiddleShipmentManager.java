package com.pousheng.middle.order.impl.manager;

import com.pousheng.middle.order.impl.dao.ShipmentExtDao;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.constans.TradeConstants;
import io.terminus.parana.order.impl.dao.OrderShipmentDao;
import io.terminus.parana.order.impl.dao.ShipmentDao;
import io.terminus.parana.order.impl.dao.ShipmentItemDao;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShipmentItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;

/**
 * Created by songrenfei on 2017/7/6
 */
@Component
public class MiddleShipmentManager {

    private final ShipmentDao shipmentDao;

    private final OrderShipmentDao orderShipmentDao;

    private final ShipmentItemDao shipmentItemDao;

    @Autowired
    private ShipmentExtDao shipmentExtDao;


    @Autowired
    public MiddleShipmentManager(ShipmentDao shipmentDao, OrderShipmentDao orderShipmentDao, ShipmentItemDao shipmentItemDao) {
        this.shipmentDao = shipmentDao;
        this.orderShipmentDao = orderShipmentDao;
        this.shipmentItemDao = shipmentItemDao;
    }

    @Transactional
    public Long create(Shipment shipment, OrderShipment  orderShipment) {
        boolean success = shipmentDao.create(shipment);
        if (!success) {
            throw new ServiceException("shipment.create.fail");
        }
        Long shipmentId = shipment.getId();

        Shipment newShipment = new Shipment();
        newShipment.setId(shipmentId);
        String shipmentCode = "SHP" + shipmentId;
        newShipment.setShipmentCode(shipmentCode);

        Map<String,String> extraMap = shipment.getExtra();
        List<ShipmentItem> shipmentItems = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(extraMap.get(TradeConstants.SHIPMENT_ITEM_INFO),
                JsonMapper.JSON_NON_EMPTY_MAPPER.createCollectionType(List.class,ShipmentItem.class));
        extraMap.put(TradeConstants.SHIPMENT_ITEM_INFO, "");
        newShipment.setExtra(extraMap);

        shipmentItems.stream().forEach(it->{
            it.setShipmentId(shipmentId);
            it.setExtraJson(JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(it.getAttrs()));
        });

        boolean updateSuccess = shipmentDao.update(newShipment);
        if (!updateSuccess) {
            throw new ServiceException("shipment.update.fail");
        } else {
            orderShipment.setShipmentId(shipmentId);
            orderShipment.setShipmentCode(shipmentCode);
            orderShipmentDao.create(orderShipment);
        }

        shipmentItemDao.creates(shipmentItems);

        return shipmentId;
    }

    /**
     * 能指定排序的分页查询
     * @param offset
     * @param limit
     * @param sort
     * @param criteria
     * @return
     */
    public Paging<Shipment> paging(Integer offset, Integer limit,String sort,Map<String,Object> criteria){
        return shipmentExtDao.paging(offset,limit,sort,criteria);
    }


}
