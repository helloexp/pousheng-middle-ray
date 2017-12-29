package com.pousheng.middle.order.dto;

import io.terminus.parana.order.model.ReceiverInfo;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by songrenfei on 2017/6/26
 */
@Data
public class RefundExtra implements Serializable{

    //销售发货的发货单id 如果不能自动匹配到发货单号，则需要人工拆单
    private Long shipmentId;

    //发货仓ID
    private Long warehouseId;

    //发货仓名称
    private String warehouseName;

    //买家收货地址
    private ReceiverInfo receiverInfo;

    //物流公司代码
    private String shipmentCorpCode;
    //物流公司名称
    private String shipmentCorpName;
    //物流单号
    private String shipmentSerialNo;

    //处理完成时间
    private Date handleDoneAt;
    //恒康同步退货完成时间
    private Date hkReturnDoneAt;
    //换货发货单创建时间
    private Date changeShipmentAt;
    //丢件补发售后单创建时间
    private Date lostShipmentAt;
    //发货时间
    private Date shipAt;
    //确认收货时间
    private Date confirmReceivedAt;
    //退款时间
    private Date refundAt;

    //pos单号
    private String posSerialNo;
    //pos单类型
    private String posType;
    //pos单金额
    private String posAmt;
    //pos单生成时间
    private Date posCreatedAt;
    //订单类型,整单还是子单
    private String orderType;
    //换货取消发货单的标记
    private String cancelShip;

    //恒康确认收到买家退货商品时间
    //private Date hkConfirmAt;
    //恒康确认收到买家退货商品信息
    private List<HkConfirmReturnItemInfo> hkConfirmItemInfos;



}
