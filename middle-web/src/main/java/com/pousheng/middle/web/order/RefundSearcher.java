package com.pousheng.middle.web.order;

import com.google.common.base.MoreObjects;
import com.pousheng.middle.order.dto.MiddleRefundCriteria;
import com.pousheng.middle.order.dto.RefundPaging;
import com.pousheng.middle.order.enums.MiddleRefundType;
import com.pousheng.middle.search.refund.RefundSearchComponent;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.utils.permission.PermissionUtil;
import io.swagger.annotations.Api;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.common.utils.DateUtil;
import io.terminus.parana.order.model.OrderLevel;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.service.RefundReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-22 10:54<br/>
 */
@Api(tags = "售后单读服务搜索API")
@RestController
@Slf4j
@OperationLogModule(OperationLogModule.Module.REFUND)
public class RefundSearcher {
    private final PermissionUtil permissionUtil;
    private final RefundReadService refundReadService;
    private final RefundSearchComponent refundSearchComponent;

    public RefundSearcher(PermissionUtil permissionUtil, RefundReadService refundReadService, RefundSearchComponent refundSearchComponent) {
        this.permissionUtil = permissionUtil;
        this.refundReadService = refundReadService;
        this.refundSearchComponent = refundSearchComponent;
    }

    @GetMapping("/api/refund/search")
    public Paging<RefundPaging> search(MiddleRefundCriteria criteria) {
        if (criteria.getRefundEndAt() != null) {
            criteria.setRefundEndAt(new DateTime(criteria.getRefundEndAt().getTime()).plusDays(1).minusSeconds(1).toDate());
        }
        criteria.setExcludeRefundType(MiddleRefundType.ON_SALES_REFUND.value());

        List<Long> currentUserCanOperateShopIds = permissionUtil.getCurrentUserCanOperateShopIDs();
        if (criteria.getShopId() == null) {
            criteria.setShopIds(currentUserCanOperateShopIds);
        } else if (!currentUserCanOperateShopIds.contains(criteria.getShopId())) {
            throw new JsonResponseException("permission.check.query.deny");
        }

        transformOrderIdAndOrderType(criteria);
        if (StringUtils.isNotEmpty(criteria.getOrderCode()) && CollectionUtils.isEmpty(criteria.getIds())) {
            return Paging.empty();
        }
        handleDate(criteria);

        return refundSearchComponent.search(criteria);
    }


    private void handleDate(MiddleRefundCriteria refundCriteria) {
        if (refundCriteria.getStartAt() != null) {
            refundCriteria.setStartAt(DateUtil.withTimeAtStartOfDay(refundCriteria.getStartAt()));
        }
        if (refundCriteria.getEndAt() != null) {
            refundCriteria.setEndAt(DateUtil.withTimeAtEndOfDay(refundCriteria.getEndAt()));
        }
        if (refundCriteria.getHkReturnDoneAtStart() != null) {
            refundCriteria.setHkReturnDoneAtStart(DateUtil.withTimeAtStartOfDay(refundCriteria.getHkReturnDoneAtStart()));
        }
        if (refundCriteria.getHkReturnDoneAtEnd() != null) {
            refundCriteria.setHkReturnDoneAtEnd(DateUtil.withTimeAtEndOfDay(refundCriteria.getHkReturnDoneAtEnd()));
        }
    }

    private void transformOrderIdAndOrderType(MiddleRefundCriteria refundCriteria) {
        String orderCode = refundCriteria.getOrderCode();
        if (null == orderCode) {
            return;
        }
        Integer orderLevel = MoreObjects.firstNonNull(refundCriteria.getOrderLevel(), OrderLevel.SHOP.getValue());
        Response<List<Refund>> r = refundReadService.findByOrderCodeAndOrderLevel(orderCode, OrderLevel.fromInt(orderLevel));
        if (!r.isSuccess()) {
            log.error("failed to find refunds by code: {}, level: {}, cause: {}", orderCode, orderLevel, r.getError());
            throw new JsonResponseException(r.getError());
        }

        List<Long> refundIds = r.getResult().stream().map(Refund::getId).collect(Collectors.toList());
        refundCriteria.setIds(refundIds);
    }
}
