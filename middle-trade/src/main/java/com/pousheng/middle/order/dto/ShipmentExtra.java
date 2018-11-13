package com.pousheng.middle.order.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 发货单扩展信息
 * Created by songrenfei on 2017/6/26
 */
@Data
public class ShipmentExtra implements Serializable{

    private static final long serialVersionUID = 1970760321501280133L;

    //所属订单是否是预售订单
    private String isStepOrder;
    //发货仓ID
    private Long warehouseId;

    //发货仓名称
    private String warehouseName;
    
    //发货仓电话号码
    private String warehouseTelephone;

    //绩效店铺名称
    private String erpPerformanceShopName;
    //绩效店铺编码
    private String erpPerformanceShopCode;
    //绩效店铺外码
    private String erpPerformanceShopOutCode;
    //下单店铺名称
    private String erpOrderShopName;
    //下单店铺编码
    private String erpOrderShopCode;
    //下单店铺外码
    private String erpOrderShopOutCode;
    //下单店铺电话号码
    private String erpOrderShopTelephone;
    //发货单商品金额
    private Long shipmentItemFee;
    //发货单运费金额
    private Long shipmentShipFee;
    //发货单运费优惠金额
    private Long shipmentShipDiscountFee;
    //发货单优惠金额
    private Long shipmentDiscountFee;
    //发货单净价
    private Long shipmentTotalFee;
    //发货单订单总额
    private Long shipmentTotalPrice;
    //ERP发货单号
    private String outShipmentId;
    //物流公司代码
    private String shipmentCorpCode;
    //物流公司名称
    private String shipmentCorpName;
    //物流单号
    private String shipmentSerialNo;
    //发货时间
    private Date shipmentDate;
    //pos单号
    private String posSerialNo;
    //pos单类型
    private String posType;
    //pos单金额
    private String posAmt;
    //pos单生成时间
    private Date posCreatedAt;
    //物流编码--外码
    private String vendCustID;
    //同步淘宝的状态,仅限于淘宝使用
    private Integer syncTaobaoStatus;
    //发货方式 1店发 2仓发
    private String shipmentWay;
    //仓库外码
    private String warehouseOutCode;
    //中台选择的恒康快递代码
    private String orderHkExpressCode;
    //中台选择的快递名称
    private String orderHkExpressName;
    //mpos接单员工
    private String receiveStaff;
    //mpos拒绝原因
    private String rejectReason;
    //取货方式
    private String takeWay;
    //是否指定门店
    private String isAppint;

    //网店零售订单号 当同步发货单到恒康开pos单同步成功时恒康返回的id
    private String hkResaleOrderId;

    //mpos发货单号
    private String mposShipmentId;

    //重量
    private double weight;

    //同步渠道(包含云聚)的状态
    private Integer syncChannelStatus;

    // 圆通回传快递单号
    private String callbackMailNo;

    // 物流单号
    private String expressOrderId;

    //备注
    private String remark;

    /**
     * 预计出/入库日期，维品会预计到货时间格式：yyyyMMddHHmmss
     */
    private String expectDate;

    /**
     * 发运方式编码
     */
    private String transportMethodCode;

    /**
     * 发运方式名称
     */
    private String transportMethodName;

    /**
     * 品牌
     */
    private String cardRemark;

    /**
     * 箱号Map
     * key为skuCode
     * value为箱号
     */
    private Map<String,String> boxNoMap;

    /**
     * 物流公司Map
     * key为skuCode
     * value为物流公司编号
     */
    private Map<String,String> shipmentCorpCodeMap;

    /**
     * 物流单号Map
     * key为skuCode
     * value为物流单号
     */
    private Map<String,String> shipmentSerialNoMap;

    /**
     * 箱号分配信息
     * shipment_item_id,box_no,quantity
     */
    private Map<Long, Map<String, Integer>> assignBoxDetail;

    /**
     * wms回传的发货信息json
     */
    private String wmsShipInfoListJson;

}
