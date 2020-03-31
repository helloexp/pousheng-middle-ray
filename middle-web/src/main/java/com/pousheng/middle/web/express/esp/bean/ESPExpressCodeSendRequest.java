package com.pousheng.middle.web.express.esp.bean;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @Desc
 * @Author GuoFeng
 * @Date 2019/8/27
 */
@Data
public class ESPExpressCodeSendRequest {
    private String sid;
    private Date tranReqDate;
    private List<ESPExpressCodeSendRequestContent> bizContent;
}
