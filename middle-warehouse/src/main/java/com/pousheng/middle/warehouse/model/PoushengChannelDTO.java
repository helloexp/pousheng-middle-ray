package com.pousheng.middle.warehouse.model;

import com.opencsv.bean.CsvBindByPosition;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 渠道库存指定设置参数
 *
 * @auther feisheng.ch
 * @time 2018/5/20
 */
@Data
@AllArgsConstructor
public class PoushengChannelDTO implements Serializable {
    private static final long serialVersionUID = 9087136566534795240L;

    /**
     * 商品条码
     */
    @CsvBindByPosition(position = 0)
    private String skuCode;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 仓库名称
     */
    @CsvBindByPosition(position = 1)
    private String warehouseName;

    /**
     * 店铺ID
     */
    private Long openShopId;

    /**
     * 店铺名称
     */
    @CsvBindByPosition(position = 2)
    private String openShopName;

    /**
     * 最初设置的指定给店铺的数量
     */
    @CsvBindByPosition(position = 3)
    private Long channelQuantity;

    /**
     * 目前还剩下的指定给店铺的数量
     */
    private Long channelLeftQuantity;

    /**
     * 店铺外码
     */
    private String openShopCode;

    /**
     * 仓库外码
     */
    private String warehouseCode;

    /**
     * 创建时间/指定时间
     */
    private Date createdAt;

    /**
     * 当前状态：0-失效  1-有效
     */
    private Integer status;

    public PoushengChannelDTO(){}

}
