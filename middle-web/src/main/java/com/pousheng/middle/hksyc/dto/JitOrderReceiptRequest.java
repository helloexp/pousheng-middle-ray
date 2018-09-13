package com.pousheng.middle.hksyc.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * JIT 订单回执请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JitOrderReceiptRequest implements Serializable {


    /**
     * 云聚订单号
     */
    private String order_sn;

    /**
     * 错误码
     * 0 代表成功 1 代表失败
     */
    private Integer error_code;

    /**
     * 错误原因
     * 成功可不填
     */
    private String error_info;
}
