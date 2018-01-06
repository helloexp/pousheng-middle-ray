package com.pousheng.middle.hksyc.dto.item;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 查询仓或门店对应商品库存信息
 * Created by songrenfei on 2017/12/22
 */
@Data
public class HkSkuStockInfo implements Serializable{

    private static final long serialVersionUID = -9080866179617924285L;

    /**
     * 中台的仓或门店id
     */
    private Long businessId;

    /**
     * 中台仓或门店名称
     */
    private String businessName;

    /**
     * 仓库 id
     */
    private String stock_id;
    /**
     * 仓库代码
     */
    private String stock_code;
    /**
     * 仓库名称
     */
    private String stock_name;
    /**
     * 仓库类别(0 = 不限; 1 = 店仓; 2 = 总仓)
     */
    private String stock_type;

    private List<SkuAndQuantityInfo> material_list;


    @Data
    public static class SkuAndQuantityInfo{

        /**
         * 货品 id
         */
        private String material_id;
        /**
         * 货品名称
         */
        private String material_name;
        /**
         * 货品条码
         */
        private String barcode;
        /**
         * 数量
         */
        private Integer quantity;

    }
}
