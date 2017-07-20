package com.pousheng.middle.hksyc.dto.trade;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/7/19
 */
@Data
public class SycHkShipmentItem implements Serializable{

    private static final long serialVersionUID = 1760133624676690164L;


    private String orderNo = "o00001";
    private String orderSubNo = "001";
    private String barcode = "xh001";
    private Integer num = 1;
    private Integer preferentialMon = 5;
    private Integer salePrice = 199;
    private Integer totalPrice = 199;
    private Integer isGifts = 0;
}
