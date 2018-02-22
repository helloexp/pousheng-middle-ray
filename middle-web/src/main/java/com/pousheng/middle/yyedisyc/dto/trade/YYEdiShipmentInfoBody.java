package com.pousheng.middle.yyedisyc.dto.trade;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/1/7
 * pousheng-middle
 */
@Data
public class YYEdiShipmentInfoBody implements Serializable {
    private List<YYEdiShipmentInfo> requestData;
}
