package com.pousheng.middle.order.impl.dao;

import com.google.common.collect.ImmutableMap;
import com.pousheng.middle.order.model.InvoiceExt;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

/**
 * Created by tony on 2017/8/3.
 * pousheng-middle
 */
@Repository
public class InvoiceExtDao extends MyBatisDao<InvoiceExt> {
    public boolean updateInvoiceDetail(InvoiceExt invoiceExt){
        return getSqlSession().update(sqlId("updateInvoiceDetail"),invoiceExt) == 1;
    }
}
