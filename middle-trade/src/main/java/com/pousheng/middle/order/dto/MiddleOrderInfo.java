package com.pousheng.middle.order.dto;

import com.opencsv.bean.CsvBindByPosition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/3/13
 * pousheng-middle
 *
 * @author
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MiddleOrderInfo implements java.io.Serializable {

    private static final long serialVersionUID = -8327226899884820593L;
    /**
     * 外部订单号
     */
    @CsvBindByPosition(position = 0)
    private String outOrderId;
    /**
     * 订单来源渠道
     */
    @CsvBindByPosition(position = 1)
    private String channel;
    /**
     * 买家名称
     */
    @CsvBindByPosition(position = 2)
    private String buyerName;

    /**
     * 买家手机号
     */
    @CsvBindByPosition(position = 3)
    private String buyerMobile;

    /**
     * 收货人姓名
     */
    @CsvBindByPosition(position = 4)
    private String receiveUserName;

    /**
     * 收货人手机号
     */
    @CsvBindByPosition(position = 5)
    private String mobile;

    /**
     * 省份
     */
    @CsvBindByPosition(position = 6)
    private String province;

    /**
     * 城市
     */
    @CsvBindByPosition(position = 7)
    private String city;

    /**
     * 县区
     */
    @CsvBindByPosition(position = 8)
    private String region;

    /**
     * 详细地址
     */
    @CsvBindByPosition(position = 9)
    private String detail;

//    /**
//     *
//     */
//    @CsvBindByPosition(position = 4)
//    private String shopId

    /**
     * 订单来源店铺名称
     */
    @CsvBindByPosition(position = 10)
    private String shopName;

//    /**
//     * 物流公司代码
//     */
//    @CsvBindByPosition(position = 11)
//    private String orderExpressCode

    /**
     * 物流公司名称
     */
    @CsvBindByPosition(position = 11)
    private String orderHkExpressName;

//    /**
//     * 运费原价
//     */
//    @CsvBindByPosition(position = 12)
//    private String originShipFee

    /**
     * 实际运费
     */
    @CsvBindByPosition(position = 12)
    private String shipFee;

    /**
     * 订单实付金额
     */
    @CsvBindByPosition(position = 13)
    private String fee;

    /**
     * 订单原价
     */
    @CsvBindByPosition(position = 14)
    private String orderOriginFee;

    /**
     * 订单总的折扣
     */
    @CsvBindByPosition(position = 15)
    private String discount;

    /**
     * 付款类型
     */
    @CsvBindByPosition(position = 16)
    private String payType;

    /**
     * 支付渠道名称
     */
    @CsvBindByPosition(position = 17)
    private String paymentChannelName;

    /**
     * 支付流水号
     */
    @CsvBindByPosition(position = 18)
    private String paymentSerialNo;

    /**
     * 订单创建时间
     */
    @CsvBindByPosition(position = 19)
    private String createdAt;

    /**
     * 发票类型
     */
    @CsvBindByPosition(position = 20)
    private String invoiceType;

    /**
     * 发票抬头类型(个人/公司)
     */
    @CsvBindByPosition(position = 21)
    private String titleType;

    /**
     * 税号/统一信用代码
     */
    @CsvBindByPosition(position = 22)
    private String taxRegisterNo;

    /**
     * 公司名称
     */
    @CsvBindByPosition(position = 23)
    private String companyName;

    /**
     * 公司注册电话
     */
    @CsvBindByPosition(position = 24)
    private String registerPhone;

    /**
     * 公司注册地址
     */
    @CsvBindByPosition(position = 25)
    private String registerAddress;

    /**
     * 注册银行
     */
    @CsvBindByPosition(position = 26)
    private String registerBank;

    /**
     * 银行账户
     */
    @CsvBindByPosition(position = 27)
    private String bankAccount;

    /**
     * 邮箱
     */
    @CsvBindByPosition(position = 28)
    private String email;

    /**
     * 客服备注
     */
    @CsvBindByPosition(position = 29)
    private String sellerRemark;

    /**
     * 买家备注
     */
    @CsvBindByPosition(position = 30)
    private String buyerNote;

    /**
     * 子订单号
     */
    @CsvBindByPosition(position = 31)
    private String outSkuOrderId;
    /**
     * 货品条码
     */
    @CsvBindByPosition(position = 32)
    private String skuCode;

    /**
     * 电商商品id
     */
    @CsvBindByPosition(position = 33)
    private String itemId;
    /**
     * 商品名称
     */
    @CsvBindByPosition(position = 34)
    private String itemName;
    /**
     * 货品数量
     */
    @CsvBindByPosition(position = 35)
    private String quantity;

    /**
     * 商品原价
     */
    @CsvBindByPosition(position = 36)
    private String originFee;

    /**
     * 商品折扣
     */
    @CsvBindByPosition(position = 37)
    private String itemDiscount;


}
