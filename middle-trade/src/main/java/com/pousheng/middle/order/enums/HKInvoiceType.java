package com.pousheng.middle.order.enums;

import com.google.common.base.Objects;

/**
 * 中台发票类型
 * Created by tony on 2017/7/25.
 * pousheng-middle
 */
public enum HKInvoiceType {
    PLAIN_INVOICE(1), //普通发票
    ELECTRONIC_INVOCE(2),//电子发票
    VALUE_ADDED_TAX_INVOICE(3);//增值税发票
    private final int value;

    HKInvoiceType(int value){
        this.value = value;
    }
    public int getValue(){return value;}

    public static HKInvoiceType fromInt(int value){
        for (HKInvoiceType payType : HKInvoiceType.values()) {
            if(Objects.equal(payType.value, value)){
                return payType;
            }
        }
        throw new IllegalArgumentException("unknown invo type: "+value);
    }
}
