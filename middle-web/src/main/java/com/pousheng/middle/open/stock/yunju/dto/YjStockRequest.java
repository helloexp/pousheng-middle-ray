package com.pousheng.middle.open.stock.yunju.dto;

import io.terminus.common.utils.JsonMapper;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Description: TODO
 * User:        liangyj
 * Date:        2018/6/26
 */
@Data
@ToString
@Builder
public class YjStockRequest implements Serializable {

    private static final long serialVersionUID = 6693766962801255789L;
    String serialno;
    List<YjStockInfo> stockInfo;
}
