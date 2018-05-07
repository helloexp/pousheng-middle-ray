package com.pousheng.middle.hksyc.dto.trade;

import com.pousheng.middle.hksyc.utils.Numbers;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Random;

/**
 * Created by songrenfei on 2017/7/19
 */
@Data
public class SycHkShipmentOrderBody implements Serializable{


    private static final long serialVersionUID = 7410531388126216790L;

    private List<SycHkShipmentOrderDto> orders;

    private String nonce;

    private String timestamp;



    public String getNonce() {
        return Numbers.getNonce();
    }


    public String getTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }
}
