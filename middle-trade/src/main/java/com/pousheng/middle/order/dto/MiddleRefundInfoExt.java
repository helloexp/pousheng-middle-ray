package com.pousheng.middle.order.dto;

import com.opencsv.bean.CsvBindByPosition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MiddleRefundInfoExt implements Serializable {

    private static final long serialVersionUID = -485065498747530839L;

    /**
     * 交易单号
     */
    @CsvBindByPosition(position = 0)
    private String saleCode;

    /**
     * 售后类型
     */
    @CsvBindByPosition(position = 1)
    private String refundType;

    /**
     * 发货单号
     */
    @CsvBindByPosition(position = 2)
    private String shipCode;

    /**
     * 商品条码
     */
    @CsvBindByPosition(position = 3)
    private String skuCode;

    /**
     * 退货退款商品数量
     */
    @CsvBindByPosition(position = 4)
    private String num;
}
