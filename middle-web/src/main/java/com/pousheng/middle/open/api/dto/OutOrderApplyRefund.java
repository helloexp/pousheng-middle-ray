package com.pousheng.middle.open.api.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by songrenfei on 2018/4/1
 */
@Data
public class OutOrderApplyRefund implements Serializable{

    private static final long serialVersionUID = -3376339772841952349L;

    private OutRefundOrder refund;

    private List<OutOrderRefundItem> items;
}
