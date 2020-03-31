package com.pousheng.middle.order.dto.reverseLogistic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author bernie
 * @date 2019/6/3
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SenderDto implements Serializable {
    /**
     * 寄件人姓名
     */
    private String senderName;

    /**
     * 寄件人电话
     */
    private String senderMobile;
    /**
     * 寄件省
     */
    private String province;
    /**
     * 寄件市
     */
    private String city;
    /**
     * 寄件区
     */
    private String region;

    /**
     * 寄件人地址
     */
    private String detail;
}
