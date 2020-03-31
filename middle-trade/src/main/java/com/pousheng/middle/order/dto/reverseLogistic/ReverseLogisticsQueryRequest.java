package com.pousheng.middle.order.dto.reverseLogistic;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * Date: 2019/06/06
 *
 * @author bernie
 */
@Data
@ApiModel("逆向物流查询")
public class ReverseLogisticsQueryRequest extends PagingCriteria implements java.io.Serializable {

    private static final long serialVersionUID = 4282263665769465196L;

    private List<String> statuses;

    /**
     * 状态
     */
    private String status;

    /**
     * 快递单号
     */
    @ApiModelProperty("快递单号")
    private String expressNo;

    /**
     * * 寄件人电话
     */
    @ApiModelProperty("电话")
    private String phone;

    /**
     * 店铺
     */
    @ApiModelProperty("店铺")
    private String shop;

    @ApiModelProperty("店铺Id")
    private String shopId;

    /**
     * 中台售后单号
     */
    @ApiModelProperty("中台售后单号")
    private String refundCode;

    /**
     * 中台销售单号
     */
    @ApiModelProperty("中台销售单号")
    private String orderCode;

    /**
     * 入库开始时间
     */
    @ApiModelProperty("入库开始时间")
    private Date startInstoreDate;
    /**
     * 入库结束时间
     *
     */
    @ApiModelProperty("入库结束时间")
    private Date endInstoreDate;

    /**
     * 当前查询类型
     */
    @ApiModelProperty("当前查询类型")
    private String currentType;
    /**
     * 客服备注
     */
    @ApiModelProperty("客服备注")
    private String customerNote;

}
