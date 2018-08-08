package com.pousheng.middle.open.api.dto;

import lombok.Data;

import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/2
 * pousheng-middle
 */
@Data
public class YyEdiShipInfo {
    /**
     * 中台发货单号
     */
    private String shipmentId;
    /**
     * yyEDI发货单号
     */
    private String yyEDIShipmentId;
    /**
     * 物流公司代码
     */
    private String shipmentCorpCode;
    /**
     * 物流单号，可以用逗号隔开，可以有多个物流单号
     */
    private String shipmentSerialNo;
    /**
     * 发货时间
     */
    private String shipmentDate;

    /**
     * 发货重量
     */
    private double weight;

    /**
     * yjERP发货单号
     */
    private String yjShipmentId;

    /**
     * 详细发货情况
     */
    private List<ItemInfo> itemInfos;


    @Data
    public static class ItemInfo {

        /**
         * 商品编码
         */
        private String skuCode;

        /**
         * 实际发货数量
         */
        private Integer quantity;

        /**
         * 物流公司代码
         */
        private String shipmentCorpCode;

        /**
         * 物流单号
         */
        private String shipmentSerialNo;

        public String getSkuCode() {
            return skuCode;
        }

        public ItemInfo skuCode(String skuCode) {
            this.skuCode = skuCode;
            return this;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public ItemInfo quantity(Integer quantity) {
            this.quantity = quantity;
            return this;
        }
    }
}
