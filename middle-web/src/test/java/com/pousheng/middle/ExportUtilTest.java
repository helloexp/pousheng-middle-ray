package com.pousheng.middle;

import com.pousheng.middle.web.export.ExportContext;
import com.pousheng.middle.web.export.ExportTitleContext;
import com.pousheng.middle.web.export.ExportUtil;
import com.pousheng.middle.web.export.OrderExportEntity;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;

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
        OrderExportEntity orderExport = new OrderExportEntity();

    }

    @Test
    public void orderTest() {
        OrderExportEntity orderExport = new OrderExportEntity();
        orderExport.setOrderID(3434888387674L);
        orderExport.setShopName("shangzhaer");
        orderExport.setPaymentDate(new Date());
        orderExport.setFee(4489L);
        ExportUtil.export(new ExportContext(Collections.singletonList(orderExport)));

    }
}
