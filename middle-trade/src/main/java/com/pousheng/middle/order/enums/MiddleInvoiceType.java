package com.pousheng.middle.order.enums;

import com.google.common.base.Objects;

/**
 * 中台发票类型
 * Created by tony on 2017/7/25.
 * pousheng-middle
 */
public enum MiddleInvoiceType {
    PLAIN_INVOICE(1), //普通发票
    VALUE_ADDED_TAX_INVOICE(2),//增值税发票
    ELECTRONIC_INVOCE(3);//电子发票
    private final int value;

    MiddleInvoiceType(int value){
        this.value = value;
    }
    public int getValue(){return value;}

    public static MiddleInvoiceType fromInt(int value){
        for (MiddleInvoiceType payType : MiddleInvoiceType.values()) {
            if(Objects.equal(payType.value, value)){
                return payType;
            }
        }
        throw new IllegalArgumentException("unknown invoice type: "+value);
    }
}
