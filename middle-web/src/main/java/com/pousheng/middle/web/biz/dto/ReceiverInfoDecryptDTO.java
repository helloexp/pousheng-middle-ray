package com.pousheng.middle.web.biz.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 收货人信息脱敏DTO
 * @author tanlongjun
 */
@Data
@Builder
public class ReceiverInfoDecryptDTO implements Serializable {

    private Long shopId;

    private String outId;

    private String outFrom;
}
