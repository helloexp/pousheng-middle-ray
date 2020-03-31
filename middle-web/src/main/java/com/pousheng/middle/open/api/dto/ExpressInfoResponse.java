package com.pousheng.middle.open.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author mz
 * @Date 2018/05/21
 */
@Data
public class ExpressInfoResponse implements Serializable {


    private static final long serialVersionUID = 472167507768365896L;
    List<String> failShipmentCodes;

    private String errorCode;
    /**
     * 错误信息
     */
    private String errorMsg;
}
