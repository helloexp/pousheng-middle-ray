package com.pousheng.middle.yyedisyc.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/8
 * pousheng-middle
 */
@Data
public class YYEdiCancelResponse implements Serializable {
    private static final long serialVersionUID = 938351538642662035L;
    //200:整体成功,100:部分成功,-100:整体失败
    private String errorCode;

    private String description;

}
