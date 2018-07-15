package com.pousheng.middle.order.impl.dao;

import com.pousheng.middle.order.enums.StockRecordType;
import com.pousheng.middle.order.model.StockRecordLog;
import io.terminus.applog.rest.service.ApplogService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertNotNull;

/**
 * Created with IntelliJ IDEA
 * User: yujiacheng
 * Date: 2018/7/14
 * Time: 下午12:41
 */
public class StockRecordLogDaoTest extends BaseDaoTest {

    @Autowired
    private StockRecordLogDao stockRecordLogDao;

    private StockRecordLog stockRecordLog;

    @Before
    public void init() {
        stockRecordLog = make();
    }

    public StockRecordLog make () {
        StockRecordLog stockRecordLog = new StockRecordLog();
        stockRecordLog.setSkuCode("22");
        stockRecordLog.setShopId(1L);
        stockRecordLog.setContext("s");
        stockRecordLog.setShipmentId(1L);
        stockRecordLog.setWarehouseId(1L);
        stockRecordLog.setType(StockRecordType.MIDDLE_CREATE_SHIPMENT.toString());
        return stockRecordLog;
    }

    @Test
    public void create() {
        stockRecordLogDao.create(stockRecordLog);
        assertNotNull(stockRecordLog.getId());
    }


}
