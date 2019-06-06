package com.pousheng.middle.web.order;

import com.pousheng.middle.order.dto.MiddleOrderCriteria;
import com.pousheng.middle.order.dto.ShopOrderPagingInfo;
import com.pousheng.middle.search.order.OrderSearchComponent;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.utils.permission.PermissionCheck;
import com.pousheng.middle.web.utils.permission.PermissionUtil;
import io.swagger.annotations.Api;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.common.constants.JitConsts;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-20 19:27<br/>
 */
@Api(tags = "订单读服务搜索API")
@RestController
@Slf4j
@PermissionCheck(PermissionCheck.PermissionCheckType.SHOP_ORDER)
@OperationLogModule(OperationLogModule.Module.ORDER)
public class AdminOrderSearcher {
    private final PermissionUtil permissionUtil;
    private final OrderSearchComponent orderSearchComponent;

    public AdminOrderSearcher(PermissionUtil permissionUtil, OrderSearchComponent orderSearchComponent) {
        this.permissionUtil = permissionUtil;
        this.orderSearchComponent = orderSearchComponent;
    }

    @GetMapping("/api/order/search")
    public Response<Paging<ShopOrderPagingInfo>> search(MiddleOrderCriteria middleOrderCriteria) {
        // 店铺
        List<Long> currentUserCanOperatShopIds = permissionUtil.getCurrentUserCanOperateShopIDs();
        if (middleOrderCriteria.getShopId() == null) {
            middleOrderCriteria.setShopIds(currentUserCanOperatShopIds);
        } else if (!currentUserCanOperatShopIds.contains(middleOrderCriteria.getShopId())) {
            throw new JsonResponseException("permission.check.query.deny");
        }
        // 结束时间格式化
        if (middleOrderCriteria.getOutCreatedEndAt() != null) {
            middleOrderCriteria.setOutCreatedEndAt(new DateTime(middleOrderCriteria.getOutCreatedEndAt()).plusDays(1).minusSeconds(1).toDate());
        }
        if (middleOrderCriteria.getStatus() != null && middleOrderCriteria.getStatus().contains(99)) {
            // 开始结束时间
            if (middleOrderCriteria.getOutCreatedStartAt() == null) {
                middleOrderCriteria.setOutCreatedStartAt(DateTime.now().withTimeAtStartOfDay().minusMonths(1).toDate());
            }
            if (middleOrderCriteria.getOutCreatedEndAt() == null) {
                middleOrderCriteria.setOutCreatedEndAt(DateTime.now().withTimeAtStartOfDay().plusDays(1).toDate());
            }
            long diff = middleOrderCriteria.getOutCreatedEndAt().getTime() - middleOrderCriteria.getOutCreatedStartAt().getTime();
            diff = diff / 1000 / 60 / 60 / 24;
            if (diff > 32) {
                return Response.fail("over 30 days");
            }
        }
        // 不显示jit时效订单来源
        middleOrderCriteria.setExcludeOutFrom(JitConsts.YUNJU_REALTIME);
        // es 搜索
        return orderSearchComponent.search(middleOrderCriteria);
    }
}
