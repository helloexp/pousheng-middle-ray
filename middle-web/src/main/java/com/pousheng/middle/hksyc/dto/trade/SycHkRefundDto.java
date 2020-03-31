package com.pousheng.middle.hksyc.dto.trade;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by songrenfei on 2017/7/19
 */
@Data
public class SycHkRefundDto implements Serializable{


    private static final long serialVersionUID = 6089373503090705425L;

    private SycHkRefund tradeRefund;

    private List<SycHkRefundItem> tradeRefundItems;

}
