package com.pousheng.middle.web.export;

import com.pousheng.middle.common.utils.batchhandle.ExportTitle;
import com.pousheng.middle.web.utils.export.ExportDateFormat;
import lombok.Data;

import java.util.Date;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/21
 * pousheng-middle
 */
@Data
public class SettlementPosEntity {

    @ExportTitle("pos单号")
    private String posSerialNo;

    @ExportTitle("pos金额")
    private String posAmt;

    @ExportTitle("店铺id")
    private Long shopId;

    @ExportTitle("店铺名称")
    private String shopName;

    @ExportTitle("pos单类型")
    private String posType;

    @ExportTitle("订单号")
    private String orderId;
    @ExportTitle("交易完成日期")
    @ExportDateFormat("yyyy-MM-dd HH:mm:ss")
    private Date posCreatedAt;

    @ExportTitle("创建时间")
    @ExportDateFormat("yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
}
