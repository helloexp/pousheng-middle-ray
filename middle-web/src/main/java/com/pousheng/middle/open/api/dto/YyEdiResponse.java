package com.pousheng.middle.open.api.dto;

import lombok.Data;

import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/24
 * pousheng-middle
 */
@Data
public class YyEdiResponse implements java.io.Serializable {
    private static final long serialVersionUID = -5062603184685687802L;
    List<YyEdiResponseDetail> fields;
    YyEdiResponseDetail field;
}
