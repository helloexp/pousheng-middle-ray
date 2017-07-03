package com.pousheng.erp.dao.mysql;

import com.pousheng.erp.dao.BaseDaoTest;
import com.pousheng.erp.model.StockBill;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-29
 */
public class StockBillDaoTest extends BaseDaoTest {

    @Autowired
    private StockBillDao stockBillDao;

    private StockBill stockBill;

    @Before
    public void setUp() throws Exception {
        stockBill = new StockBill();
        stockBill.setBill_no("bill_no_1");
        stockBill.setBarcode("barcode1");
        stockBill.setQuantity(123);
        stockBill.setBill_status("1");
        stockBill.setBill_type("0");
        stockBill.setCompany_id("company_id_1");
        stockBill.setStock_id("stock_id_1");
        stockBill.setOriginal_bill_no("original_bill_no_0");
        stockBill.setModify_datetime(new Date());

        boolean success = stockBillDao.create(stockBill);
        assertThat(success, is(true));
    }

    @Test
    public void findByBillNo() throws Exception {

        StockBill actual = stockBillDao.findByBillNoAndBillType(stockBill.getBill_no(), stockBill.getBill_type());
        assertThat(actual, is(stockBill));
    }

}