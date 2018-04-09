package com.pousheng.middle.yyedisyc.dto.trade;

import lombok.Data;

import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/7
 * pousheng-middle
 */
@Data
public class YYEdiReturnInfoBody implements java.io.Serializable{
    private static final long serialVersionUID = 7695791106328104770L;

    List<YYEdiReturnInfo> requestData;
}
