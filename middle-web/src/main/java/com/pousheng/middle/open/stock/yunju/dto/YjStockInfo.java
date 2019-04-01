package com.pousheng.middle.open.stock.yunju.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/**
 * Description: TODO
 * User:        liangyj
 * Date:        2018/6/26
 */
@Data
@ToString
@Builder
public class YjStockInfo implements Serializable {

    private static final long serialVersionUID = 1644838138993862945L;
    String lineNo;
    String companyCode;
    String warehouseCode;
    String goodsCode;
    String size;
    String barCode;
    int num;
}

