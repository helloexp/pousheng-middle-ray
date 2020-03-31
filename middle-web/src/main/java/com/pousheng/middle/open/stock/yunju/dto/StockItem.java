package com.pousheng.middle.open.stock.yunju.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Description: TODO
 * User:        liangyj
 * Date:        2018/7/7
 */
@Data
public class StockItem implements Serializable {

    private static final long serialVersionUID = -2085008494101765232L;
    String lineNo;
    String error;
    String errorInfo;
}
