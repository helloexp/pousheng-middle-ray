package com.pousheng.middle.web.events.trade;

import lombok.Data;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/30
 * pousheng-middle
 * @author tony
 */
@Data
public class StepOrderNotifyHkEvent implements java.io.Serializable {
    private static final long serialVersionUID = 9114183118273744176L;
    private Long shopOrderId;
}
