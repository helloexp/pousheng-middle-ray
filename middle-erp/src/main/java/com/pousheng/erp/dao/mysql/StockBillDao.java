package com.pousheng.erp.dao.mysql;

import com.google.common.collect.ImmutableMap;
import com.pousheng.erp.model.StockBill;
import io.terminus.common.mysql.dao.MyBatisDao;
import org.springframework.stereotype.Repository;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-29
 */
@Repository
public class StockBillDao extends MyBatisDao<StockBill> {

    /**
     * 根据单据号查找对应的单据
     *
     * @param billNo 单据号
     * @param billType 单据类型
     * @return 对应的单据
     */
    public StockBill findByBillNoAndBillType(String billNo, String billType){
        return getSqlSession().selectOne(sqlId("findByBillNoAndBillType"),
                ImmutableMap.of("bill_no", billNo, "bill_type", billType));
    }
}
