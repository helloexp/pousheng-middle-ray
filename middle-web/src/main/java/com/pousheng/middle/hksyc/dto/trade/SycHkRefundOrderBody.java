package com.pousheng.middle.hksyc.dto.trade;

import com.pousheng.middle.hksyc.utils.Numbers;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/7/19
 */
@Data
public class SycHkRefundOrderBody implements Serializable{


    private static final long serialVersionUID = 6785005236400472777L;
    private SycHkRefundDto refundorder;

    private String nonce;

    private String timestamp;



    public String getNonce() {
        return Numbers.getNonce();
    }


    public String getTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }
}
