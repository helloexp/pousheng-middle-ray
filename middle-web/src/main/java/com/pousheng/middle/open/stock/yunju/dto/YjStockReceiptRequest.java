package com.pousheng.middle.open.stock.yunju.dto;

import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Description: TODO
 * User:        liangyj
 * Date:        2018/7/7
 */
@Data
@ToString
public class YjStockReceiptRequest {

    @NotEmpty(message = "serialNo is not empty")
    @NotNull(message = "seriaNo is not null")
    private String serialNo;
    @NotEmpty(message = "error is not empty")
    @NotNull(message = "error is not null")
    private String error;
    private String errorInfo;
    private List<StockItem> items;
}
