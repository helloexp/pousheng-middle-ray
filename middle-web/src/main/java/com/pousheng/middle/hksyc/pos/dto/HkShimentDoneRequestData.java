package com.pousheng.middle.hksyc.pos.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 发货完成请求数据
 * Created by songrenfei on 2018/1/17
 */
@Data
public class HkShimentDoneRequestData implements Serializable{

    private static final long serialVersionUID = 3755663716401859167L;

    private String sid = "PS_ERP_POS_netsalReceiptDate";
    private String tranReqDate;
    private List<HkShimentDoneInfo> bizContent;
}
