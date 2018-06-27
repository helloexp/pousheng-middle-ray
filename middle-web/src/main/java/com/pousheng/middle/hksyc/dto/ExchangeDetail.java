package com.pousheng.middle.hksyc.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ExchangeDetail implements Serializable{

    //sku条码 M
    private String bar_code;
    //云聚传给中台的行号(子退货单号id) M
    private Long line_number;
    //退货入库的正品数量(无数量传0) M
    private int ok_num;
    //退货入库的残品数量 M
    private int error_num;

}
