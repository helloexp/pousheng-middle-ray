package com.pousheng.middle.order.dto.reverseLogistic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * @author bernie
 * @date 2019/6/3
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReverseLogisticTime implements Serializable {

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 入库单完成时间
     */
    private Date accomplishAt;

    /**
     * 入库单据流入时间
     */
    private Date inFlowAt;

    /**
     * 无头件关闭时间
     */
    private Date closeAt;

    /**
     * 无头件ASN创建时间
     */
    private Date AsnCreateAt;

    /**
     * 入库单到仓时间
     */
    private Date arriveWmsDate;
    /**
     * 无头件/入库单收货时间
     */
    private Date confirmReceiveDate;
    /**
     * 入库单反馈时间
     */
    private Date responseDate;

}
