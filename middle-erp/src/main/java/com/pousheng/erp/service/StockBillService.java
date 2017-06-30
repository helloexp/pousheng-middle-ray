package com.pousheng.erp.service;

import com.google.common.base.Throwables;
import com.pousheng.erp.dao.mysql.StockBillDao;
import com.pousheng.erp.model.StockBill;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 单据服务
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-29
 */
@Service
@Slf4j
public class StockBillService {

    private StockBillDao stockBillDao;

    @Autowired
    public StockBillService(StockBillDao stockBillDao) {
        this.stockBillDao = stockBillDao;
    }

    /**
     * 根据单据号查找对应的单据
     *
     * @param billNo 单据号
     * @return 对应的单据
     */
    public Response<StockBill> findByBillNo(String billNo, String billType){
        try {
            StockBill stockBill = stockBillDao.findByBillNoAndBillType(billNo, billType);
            if(stockBill == null){
                log.error("StockBill(bill_no={}, bill_type={}) not found", billNo, billType);
                return Response.fail("stockBill.not.found");
            }
            return Response.ok(stockBill);
        } catch (Exception e) {
            log.error("failed to find stockBill by billNo={} and bill_type={}, cause:{}",
                    billNo, billType, Throwables.getStackTraceAsString(e));
            return Response.fail("stockBill.find.fail");
        }
    }

    /**
     * 创建或者更新单据, 如果单据号已经在数据库中存在, 则更新
     *
     * @param stockBill 要持久化的单据
     * @return 是否持久化成功
     */
    public Response<Boolean> persist(StockBill stockBill){
        try {
            StockBill existed = stockBillDao.findByBillNoAndBillType(stockBill.getBill_no(), stockBill.getBill_type());
            if(existed != null){ //如果已经存在则更新
                stockBillDao.update(stockBill);
            }else {
                stockBillDao.create(stockBill);
            }
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("failed to persist {}, cause:{}", stockBill,Throwables.getStackTraceAsString(e));
            return Response.fail("stockBill.persist.fail");
        }
    }
}
