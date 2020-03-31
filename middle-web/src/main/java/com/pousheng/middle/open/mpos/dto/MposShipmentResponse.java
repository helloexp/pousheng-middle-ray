package com.pousheng.middle.open.mpos.dto;

import lombok.Data;

/**
 * Created by will.gong on 2019/06/17
 */
@Data
public class MposShipmentResponse {

    private Boolean success;
    private MposShipmentExtra result;
    private String error;
    private String errorMessage;

}
