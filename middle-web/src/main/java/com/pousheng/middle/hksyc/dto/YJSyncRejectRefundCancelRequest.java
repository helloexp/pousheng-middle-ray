package com.pousheng.middle.hksyc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @author zhurg
 * @date 2019/5/29 - 上午11:39
 */
@Data
@Builder
public class YJSyncRejectRefundCancelRequest implements Serializable {

    private static final long serialVersionUID = 6123768034423116540L;

    /**
     * 中台退货单号
     */
    @JsonProperty(value = "mg_exchange_sn")
    private String mg_exchange_sn;
}