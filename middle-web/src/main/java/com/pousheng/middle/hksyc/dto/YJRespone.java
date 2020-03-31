package com.pousheng.middle.hksyc.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 云聚响应对象
 */
@Data
public class YJRespone implements Serializable {


    private static final long serialVersionUID = -5645127165936510533L;

    /**
     * 错误码,0代表成功
     */
    private  int error = 1;

    /**
     * 信息描述
     */
    private String error_info;

    /**
     * 响应体
     */
    private List data;
}
