package com.pousheng.middle.order.dto.reverseLogistic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Date;
import java.util.List;

/**
 * Date: 2019/06/06
 *
 * @author bernie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReverseLogisticsDto implements java.io.Serializable {

    private static final long serialVersionUID = 4282263665769465196L;

    /**
     * 今日待处理
     */
    private Long todayNoProcessCount;
    /**
     * 总待处理
     */
    private Long totalNoProcessCount;

    /**
     * 盘盈数量
     */
    private Long profitCount;

    /**
     * 拒收寄回数量
     */
    private Long refuseCount;

}
