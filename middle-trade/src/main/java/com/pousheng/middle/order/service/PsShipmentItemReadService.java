package com.pousheng.middle.order.service;

import com.pousheng.middle.order.dto.ShipmentItemCriteria;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.ShipmentItem;
import java.util.List;

/**
 * @Description: TODO
 * @author: yjc
 * @date: 2018/9/12下午6:56
 */
public interface PsShipmentItemReadService {


    Response<List<ShipmentItem>> findShipmentItems(ShipmentItemCriteria criteria);
}
