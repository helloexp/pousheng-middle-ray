package com.pousheng.middle.order.impl.manager;

import com.pousheng.middle.order.impl.dao.ReceiverInfoExtDao;
import io.terminus.common.model.Paging;
import io.terminus.parana.order.model.ReceiverInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 收货人信息管理
 * @author tanlongjun
 */
@Component
public class MiddleReceiverInfoManager {

    private final ReceiverInfoExtDao receiverInfoExtDao;


    @Autowired
    public MiddleReceiverInfoManager(ReceiverInfoExtDao receiverInfoExtDao) {
        this.receiverInfoExtDao = receiverInfoExtDao;
    }

    /**
     * 能指定排序的分页查询
     * @param offset
     * @param limit
     * @param sort
     * @param criteria
     * @return
     */
    public Paging<ReceiverInfo> paging(Integer offset, Integer limit, String sort, Map<String,Object> criteria){
        return receiverInfoExtDao.paging(offset,limit,sort,criteria);
    }

    /**
     * 更新订单收货人信息
     * @param data
     * @return
     */
    public Boolean update(ReceiverInfo data){
        return receiverInfoExtDao.update(data);
    }

}
