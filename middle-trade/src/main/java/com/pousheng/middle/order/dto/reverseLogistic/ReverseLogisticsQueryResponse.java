package com.pousheng.middle.order.dto.reverseLogistic;

import com.pousheng.middle.order.dto.ShopOrderPagingInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.terminus.common.model.Paging;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Date: 2019/06/06
 *
 * @author bernie
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel
public class ReverseLogisticsQueryResponse<T> implements java.io.Serializable {

    private static final long serialVersionUID = 4282263665769465196L;

    @ApiModelProperty("逆向物流信息")
    Paging<T> page;

    @ApiModelProperty("无头件个数")
    private Long headlessCount=0L;

    @ApiModelProperty("退货入库明细数")
    private Long instoreCount=0L;

    @ApiModelProperty("快递交接明细")
    private Long expressCount=0L;

    @ApiModelProperty("当前类型 headless express instore")
    private String currentType;

}
