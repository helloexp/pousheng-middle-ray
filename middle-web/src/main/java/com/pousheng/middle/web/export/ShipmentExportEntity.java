package com.pousheng.middle.web.export;

import com.pousheng.middle.common.utils.batchhandle.ExportTitle;
import com.pousheng.middle.web.utils.export.ExportDateFormat;
import lombok.Data;

import java.util.Date;

/**
 * Created by sunbo@terminus.io on 2017/10/15.
 */
@Data
public class ShipmentExportEntity {

    @ExportTitle("店铺")
    private String shopName;

    @ExportTitle("服务方区部")
    private String serverArea;

    @ExportTitle("订单号")
    private String orderCode;

    @ExportTitle("货号")
    private String materialCode;
    @ExportTitle("发货单号")
    private String shipmenCode;
    @ExportTitle("派单类型")
    private String dispatchType;
    
    @ExportTitle("货品条码")
    private String itemNo;

    @ExportTitle("承运公司")
    private String shipmentCorpName;

    @ExportTitle("快递单号")
    private String carrNo;

    @ExportTitle("收件人")
    private String reciverName;

    @ExportTitle("收货地址")
    private String reciverAddress;

    @ExportTitle("电话")
    private String phone;

//    @ExportTitle("销售类型")
//    private String saleType;

    @ExportTitle("付款方式")
    private String payType;

    @ExportTitle("付款日期")
    @ExportDateFormat("yyyyMMdd")
    private Date paymentdate;

    @ExportTitle("数量")
    private Integer skuQuantity;

    @ExportTitle("金额")
    private Double fee;

//    @ExportTitle("运费")
//    private Double shipFee;

//    @ExportTitle("发票信息")
//    private String invoice;

    @ExportTitle("客服订单备注")
    private String orderMemo;

    @ExportTitle("发货状态")
    private String shipmentStatus;

    @ExportTitle("拒绝原因")
    private String rejectReason;

    @ExportTitle("发货方式")
    private String shipWay;

    @ExportTitle("发货方")
    private String warehouseName;
    @ExportTitle("发货方外码")
    private String warehouseOutCode;

    @ExportTitle("发货方区部")
    private String shipArea;

    @ExportTitle("物流单号")
    private String expressOrderId;

    @ExportTitle("外部交易单号")
    private String outId;

    @ExportTitle("门店自提")
    private String shopCustomerPickUp;
    
    @ExportTitle("订单类型")
    private String orderType;

    @ExportTitle("下单时间")
    @ExportDateFormat("yyyy-MM-dd HH:mm:ss")
    private Date outCreatedDate;

}
