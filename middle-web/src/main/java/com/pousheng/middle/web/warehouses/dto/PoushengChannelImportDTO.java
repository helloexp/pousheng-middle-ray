package com.pousheng.middle.web.warehouses.dto;

import com.opencsv.bean.CsvBindByPosition;
import lombok.Data;

import java.io.Serializable;

/**
 * 渠道库存指定设置参数
 *
 * @auther feisheng.ch
 * @time 2018/5/20
 */
@Data
public class PoushengChannelImportDTO implements Serializable {
    private static final long serialVersionUID = 9087136566534795240L;

    /**
     * 商品条码
     */
    @CsvBindByPosition(position = 0)
    private String skuCode;

    /**
     * 仓库编号
     */
    @CsvBindByPosition(position = 1)
    private String warehouseCode;

    /**
     * 店铺名称
     */
    @CsvBindByPosition(position = 2)
    private String openShopName;

    /**
     * 最初设置的指定给店铺的数量
     */
    @CsvBindByPosition(position = 3)
    private String channelQuantity;

}
