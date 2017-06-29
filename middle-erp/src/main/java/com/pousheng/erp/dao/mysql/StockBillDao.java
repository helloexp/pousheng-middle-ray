package com.pousheng.erp.dao.mysql;

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
     * @return 对应的单据
     */
    public StockBill findByBillNo(String billNo){
        return getSqlSession().selectOne(sqlId("findByBillNo"), billNo);
    }
}
