package com.pousheng.middle.web.yintai.dto;

import io.terminus.open.client.common.mappings.model.BrandMapping;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * AUTHOR: zhangbin
 * ON: 2019/7/15
 */
@Data
public class MessageDTO implements Serializable {
    private static final long serialVersionUID = 7884996103021023333L;

    private Date start;

    private BrandMapping brandMapping;

    private String skuCode;
}
