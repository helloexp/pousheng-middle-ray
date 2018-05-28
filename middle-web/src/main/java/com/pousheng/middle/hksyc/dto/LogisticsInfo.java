package com.pousheng.middle.hksyc.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.BaseEncoding;
import io.terminus.common.utils.JsonMapper;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.UnsupportedEncodingException;

@Data
@NoArgsConstructor
public class LogisticsInfo {

    /**
     * ERP的订单商品id,ERP向WMS下单时会作用行号传给中台(云聚的子订单号),这个子订单号相当于云聚这边的sku_id
     */
    private String order_product_id;

    /**
     * sku条码
     * M
     */
    private String bar_code;

    /**
     * 发货的快递公司的code
     * M
     */
    private String logistics_company_code;

    /**
     * 发货的快递公司单号
     * M
     */
    private String logistics_order;


    /**
     * 发货人
     * M
     */
    private String delivery_name;

    /**
     * 发货时间
     * Mandatory
     */
    private String delivery_time;

    /**
     * 发货地址
     * Optional
     */
    private String delivery_address;


    /**
     * 实际发货商品数量
     * M
     */
    private Integer amount;




}
