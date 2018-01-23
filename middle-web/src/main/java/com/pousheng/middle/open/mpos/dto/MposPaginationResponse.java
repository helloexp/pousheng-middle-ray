package com.pousheng.middle.open.mpos.dto;

import io.terminus.common.model.Paging;
import lombok.Data;

/**
 * Created by penghui on 2018/1/12
 */
@Data
public class MposPaginationResponse {

    private Boolean success;
    private Paging<MposShipmentExtra> result;
    private String error;
    private String errorMessage;

}
