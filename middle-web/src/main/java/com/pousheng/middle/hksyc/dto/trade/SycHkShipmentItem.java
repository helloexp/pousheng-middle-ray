package com.pousheng.middle.hksyc.dto.trade;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Created by songrenfei on 2017/7/19
 */
@Data
public class SycHkShipmentItem implements Serializable{

    private static final long serialVersionUID = 1760133624676690164L;


    private String orderNo;
    private String orderSubNo;
    private String barcode;
    private Integer num;
    private String preferentialMon;
    private String salePrice;
    private String totalPrice;
    private Integer isGifts;
}
