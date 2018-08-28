package com.pousheng.middle.order.impl.manager;

import com.pousheng.middle.order.impl.dao.OrderReceiverInfoExtDao;
import io.terminus.common.model.Paging;
import io.terminus.parana.order.model.OrderReceiverInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 订单收货人信息管理
 * @author tanlongjun
 */
@Component
public class MiddleOrderReceiverInfoManager {

    private final OrderReceiverInfoExtDao orderReceiverInfoExtDao;


    @Autowired
    public MiddleOrderReceiverInfoManager(OrderReceiverInfoExtDao orderReceiverInfoExtDao) {
        this.orderReceiverInfoExtDao = orderReceiverInfoExtDao;
    }

    /**
     * 能指定排序的分页查询
     * @param offset
     * @param limit
     * @param sort
     * @param criteria
     * @return
     */
    public Paging<OrderReceiverInfo> paging(Integer offset, Integer limit, String sort, Map<String,Object> criteria){
        return orderReceiverInfoExtDao.paging(offset,limit,sort,criteria);
    }

    /**
     * 更新订单收货人信息
     * @param data
     * @return
     */
    public Boolean update(OrderReceiverInfo data){
        return orderReceiverInfoExtDao.update(data);
    }

}
