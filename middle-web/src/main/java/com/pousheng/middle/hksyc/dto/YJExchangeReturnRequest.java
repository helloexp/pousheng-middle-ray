package com.pousheng.middle.hksyc.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class YJExchangeReturnRequest implements Serializable {
    private static final long serialVersionUID = 507593501221548045L;

    //退货单号(唯一)
    private String exchange_id;

    private List<ExchangeDetail> exchange_detail; //list<ExchangeDetail>


}
