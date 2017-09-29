package com.pousheng.middle;

import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.web.export.OrderExportEntity;
import com.pousheng.middle.web.utils.export.ExportContext;
import com.pousheng.middle.web.utils.export.ExportTitleContext;
import com.pousheng.middle.web.utils.export.ExportUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by sunbo@terminus.io on 2017/7/20.
 */
public class ExportUtilTest {

    @Test
    public void test() {

        ExportContext context = new ExportContext(Collections.singletonList(new OrderExportEntity()));
        context.addTitle(new ExportTitleContext("店铺编号"));
        ExportUtil.export(context);

    }

    @Test
    public void orderExportTest() {

        List<OrderExportEntity> entities = new ArrayList<>();

        OrderExportEntity orderExport = new OrderExportEntity();
        orderExport.setOrderID(System.currentTimeMillis());
        orderExport.setShopName("张三的店");
        entities.add(orderExport);

        OrderExportEntity orderExport2 = new OrderExportEntity();
        orderExport2.setOrderID(System.currentTimeMillis());
        orderExport2.setShopName("里斯的店");
        entities.add(orderExport2);

        OrderExportEntity orderExport3 = new OrderExportEntity();
        orderExport3.setOrderID(System.currentTimeMillis());
        orderExport3.setShopName("王武的店");
        entities.add(orderExport3);

        ExportContext context = new ExportContext(entities);
        context.setResultType(ExportContext.ResultType.FILE);
        ExportUtil.export(context);
    }


    @Test
    public void orderTest() {
        OrderExportEntity orderExport = new OrderExportEntity();
        orderExport.setOrderID(3434888387674L);
        orderExport.setShopName("shangzhaer");
        orderExport.setPaymentDate(new Date());
        orderExport.setOrderStatus(MiddleOrderStatus.CONFIRMED.getName());
        orderExport.setFee(448.9);

        ExportContext context = new ExportContext(Collections.singletonList(orderExport));
        context.setResultType(ExportContext.ResultType.FILE);
        ExportUtil.export(context);
    }
}
