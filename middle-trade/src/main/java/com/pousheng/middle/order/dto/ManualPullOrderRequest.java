package com.pousheng.middle.order.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.Serializable;
import java.util.Date;

/**
 * @author bernie
 * @date 2019/5/27
 */
@Data
@ApiModel("手动拉取订单请求")
public class ManualPullOrderRequest implements Serializable {

    private static final long serialVersionUID = 8273562593799115445L;

    /**
     * 店铺id
     */
    @ApiModelProperty("外部店铺Id")
    private Long openShopId;
    /**
     * 外部订单号
     */
    @ApiModelProperty("外部订单号/逆向单号")
    private String outerOrderId;
    /**
     * 开始拉取时间
     */
    @ApiModelProperty("拉取开始时间")
    private String pullStartDate;

    @ApiModelProperty("拉取结束时间")
    private String pullEndDate;

    private Date startDate;

    private Date endDate;

    /**
     * 拉取订单类型
     */
    @ApiModelProperty("拉取订单类型1:销售单 2:预售单 3:售后单")
    private String orderCategory;
}
