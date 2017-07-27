package com.pousheng.middle.hksyc.dto.trade;

import com.pousheng.middle.hksyc.dto.HkResponseHead;
import lombok.Data;

import java.io.Serializable;

/**
 * 同步售后单时返回的响应头和响应体
 * Created by tony on 2017/7/26.
 * pousheng-middle
 */
@Data
public class SycRefundResponse implements Serializable {
    private static final long serialVersionUID = -7906655550101958204L;
    private HkResponseHead head;
    private SycHkRefundResponseBody refundBody;
}
