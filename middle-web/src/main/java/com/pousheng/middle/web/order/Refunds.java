package com.pousheng.middle.web.order;

import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.MiddleRefundDetail;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.RefundItem;
import com.pousheng.middle.order.dto.RefundPaging;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.order.dto.RefundCriteria;
import io.terminus.parana.order.model.OrderRefund;
import io.terminus.parana.order.model.Refund;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Created by songrenfei on 2017/6/26
 */
@RestController
@Slf4j
public class Refunds {

    @Autowired
    private RefundReadLogic refundReadLogic;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();


    //逆向单分页
    @RequestMapping(value = "/api/refund/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Paging<RefundPaging> findBy(RefundCriteria criteria) {

        Response<Paging<RefundPaging>> pagingRes = refundReadLogic.refundPaging(criteria);
        if(!pagingRes.isSuccess()){
            log.error("paging refund by criteria:{} fail,error:{}",criteria,pagingRes.getError());
            throw new JsonResponseException(pagingRes.getError());
        }

        return pagingRes.getResult();
    }


    //逆向单详情
    @RequestMapping(value = "/api/refund/{id}/detail", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public MiddleRefundDetail detail(@PathVariable(value = "id") Long refundId) {

        Refund refund = refundReadLogic.findRefundById(refundId);
        OrderRefund orderRefund = refundReadLogic.findOrderRefundByRefundId(refundId);
        MiddleRefundDetail refundDetail = new MiddleRefundDetail();
        refundDetail.setOrderRefund(orderRefund);
        refundDetail.setRefund(refund);

        Map<String,String> extraMap = refund.getExtra();
        if(CollectionUtils.isEmpty(extraMap)){
            log.error("refund(id:{}) extra field is null",refundId);
            throw new JsonResponseException("refund.extra.is.empty");
        }
        if(!extraMap.containsKey(TradeConstants.REFUND_ITEM_INFO)){
            log.error("refund(id:{}) extra map not contain key:{}",refundId,TradeConstants.REFUND_ITEM_INFO);
            throw new JsonResponseException("refund.exit.not.contain.item.info");
        }
        if(!extraMap.containsKey(TradeConstants.REFUND_EXTRA_INFO)){
            log.error("refund(id:{}) extra map not contain key:{}",refundId,TradeConstants.REFUND_EXTRA_INFO);
            throw new JsonResponseException("refund.exit.not.contain.extra.info");
        }

        refundDetail.setRefundItems(mapper.fromJson(extraMap.get(TradeConstants.REFUND_ITEM_INFO),mapper.createCollectionType(List.class,RefundItem.class)));
        refundDetail.setRefundExtra(mapper.fromJson(extraMap.get(TradeConstants.REFUND_EXTRA_INFO),RefundExtra.class));

        return refundDetail;
    }

}
