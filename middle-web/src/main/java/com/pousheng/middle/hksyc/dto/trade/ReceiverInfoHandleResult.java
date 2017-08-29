package com.pousheng.middle.hksyc.dto.trade;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by songrenfei on 2017/8/28
 */
@Data
public class ReceiverInfoHandleResult implements Serializable{

    private static final long serialVersionUID = -5331881789079779698L;
    private Boolean success;

    private List<String> errors;
}
