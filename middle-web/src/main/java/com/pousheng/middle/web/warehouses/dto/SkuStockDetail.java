package com.pousheng.middle.web.warehouses.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-08-02
 */
@Data
public class SkuStockDetail implements Serializable{
    private static final long serialVersionUID = -1651411323261554539L;

    private Long total;

    private List<StockInWarehouse> details;

    @Data
    public static class StockInWarehouse implements Serializable{
        private static final long serialVersionUID = -3417388266723802622L;

        private Long id;

        private String warehouseInnerCode;

        private String warehouseName;

        private Long quantity;
    }
}
