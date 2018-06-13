package com.pousheng.middle.open.mpos.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by penghui on 2018/1/2
 */
@Data
public class MposResponse implements Serializable{

    private static final long serialVersionUID = 5245400091900032562L;

    private boolean success;

    private String result;

    private String error;

    private String errorMessage;
}
