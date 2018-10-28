package com.pousheng.middle.web.order.job;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.dto.MiddleOrderCriteria;
import com.pousheng.middle.order.dto.fsm.MiddleOrderStatus;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.service.MiddleOrderReadService;
import com.pousheng.middle.web.order.component.OrderWriteLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.jd.order.JdOrderService;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShopOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @Description: not paid order for jd to cancel status
 * @author: yjc
 * @date: 2018/10/12下午1:26
 */
@ConditionalOnProperty(name = "trade.job.enable", havingValue = "true", matchIfMissing = true)
@Slf4j
@RestController
@RequestMapping(value = "/api/jd")
public class JdNotPaidOrderJob{

    @Autowired
    private ShopOrderReadService shopOrderReadService;
    @Autowired
    private JdOrderService jdOrderService;
    @Autowired
    private OrderWriteLogic orderWriteLogic;
    @RpcConsumer
    private MiddleOrderReadService middleOrderReadService;
    @RpcConsumer
    private OpenShopReadService openShopReadService;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    /**
     * 定时轮询取消04:00 16:00
     */
    @Scheduled(cron = "0 0 4,16 * * ? *")
    public void orderCancelJob() {

        // 京东渠道店铺
        Response<List<OpenShop>> findR = openShopReadService.findByChannel(MiddleChannel.JD.getValue());
        if (!findR.isSuccess()) {
            log.error("fail to search all open shop by cause:{}", findR.getError());
            return;
        }
        List<OpenShop> openShopList = findR.getResult();
        openShopList.stream().forEach(openShop -> {
            Long openShopId = openShop.getId();
            MiddleOrderCriteria middleOrderCriteria = new MiddleOrderCriteria();
            middleOrderCriteria.setShopId(openShopId);
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                middleOrderCriteria.setOutCreatedStartAt(sdf.parse("2018-10-19"));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            Integer pageNo = 1;
            Integer pageSize = 200;
            while(true) {
                middleOrderCriteria.setPageNo(pageNo);
                middleOrderCriteria.setPageSize(pageSize);
                Response<Paging<ShopOrder>> pagingRes =  middleOrderReadService.pagingShopOrder(middleOrderCriteria);
                if(!pagingRes.isSuccess()){
                    log.error("find order paging by middleOrderCriteria {} fail", mapper.toJson(middleOrderCriteria));
                    break;
                }
                List<ShopOrder> shopOrders = pagingRes.getResult().getData();
                if (CollectionUtils.isEmpty(shopOrders)) {
                    break;
                }

                shopOrders.stream().forEach(shopOrder -> {

                    // 已取消的跳过
                    if(Objects.equals(shopOrder.getStatus(),MiddleOrderStatus.CANCEL.getValue())) {
                        return;
                    }

                    // 不是预售单的跳过
                    Map<String,String> extra = shopOrder.getExtra();
                    if(CollectionUtils.isEmpty(extra) ||  !extra.containsKey(TradeConstants.IS_STEP_ORDER)) {
                        return;
                    }

                    Response<Boolean> response = jdOrderService.existOrder(openShopId, shopOrder.getOutId(), "TRADE_CANCELED", "orderId");

                    if(!response.isSuccess()) {
                        log.error("find cancel order detail fail by shopId {}, outId {}, error {}", openShopId, shopOrder.getOutId(), response.getError());
                        return;
                    }

                    doCancel(shopOrder.getOutId());

                    log.info("cancel order success by openShopId {}, outerOrderId {}", openShopId, shopOrder.getOutId());
                });

                pageNo++;
            }
        });
    }


    /**
     * 手动单个取消
     * @param openShopId 店铺id
     * @param outerOrderId 外部单号
     */
    @RequestMapping(value = "/cancel/single/order", method = RequestMethod.GET)
    public void singleCancelOrder(@RequestParam(value = "openShopId") Long openShopId,
                            @RequestParam(value = "outerOrderId") String outerOrderId) {

        Response<Boolean> response = jdOrderService.existOrder(openShopId, outerOrderId, "TRADE_CANCELED", "orderId");

        if(!response.isSuccess()) {
            log.error("find cancel order detail fail by shopId {}, outId {}, error {}", openShopId, outerOrderId, response.getError());
            return;
        }

        doCancel(outerOrderId);

        log.info("cancel order success by openShopId {}, outerOrderId {}", openShopId, outerOrderId);
    }


    /**
     * 手动批量取消店铺单据
     * @param openShopId 店铺id
     */
    @RequestMapping(value = "/cancel/batch/order", method = RequestMethod.GET)
    public void batchCancelOrder(@RequestParam(value = "openShopId") Long openShopId) {
        MiddleOrderCriteria middleOrderCriteria = new MiddleOrderCriteria();
        middleOrderCriteria.setShopId(openShopId);
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            middleOrderCriteria.setOutCreatedStartAt(sdf.parse("2018-10-19"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Integer pageNo = 1;
        Integer pageSize = 200;
        while(true) {
            middleOrderCriteria.setPageNo(pageNo);
            middleOrderCriteria.setPageSize(pageSize);
            Response<Paging<ShopOrder>> pagingRes = middleOrderReadService.pagingShopOrder(middleOrderCriteria);
            if (!pagingRes.isSuccess()) {
                log.error("find order paging by middleOrderCriteria {} fail", mapper.toJson(middleOrderCriteria));
                break;
            }
            List<ShopOrder> shopOrders = pagingRes.getResult().getData();
            if (CollectionUtils.isEmpty(shopOrders)) {
                break;
            }
//
            shopOrders.stream().forEach(shopOrder -> {

                // 已取消的跳过
                if(Objects.equals(shopOrder.getStatus(),MiddleOrderStatus.CANCEL.getValue())) {
                    return;
                }

                // 不是预售单的跳过
                Map<String,String> extra = shopOrder.getExtra();
                if(CollectionUtils.isEmpty(extra) ||  !extra.containsKey(TradeConstants.IS_STEP_ORDER)) {
                    return;
                }

                Response<Boolean> response = jdOrderService.existOrder(openShopId, shopOrder.getOutId(), "TRADE_CANCELED", "orderId");

                if(!response.isSuccess()) {
                    log.error("find batch cancel order detail fail by shopId {}, outId {}, error {}", openShopId, shopOrder.getOutId(), response.getError());
                    return;
                }

                doCancel(shopOrder.getOutId());

                log.info("cancel batch order success by openShopId {}, outerOrderId {}", openShopId, shopOrder.getOutId());
            });
            pageNo++;
        }

    }


    /**
     * 中台取消逻辑
     * @param outerOrderId
     */
    public void doCancel(String outerOrderId) {
        Response<Optional<ShopOrder>> shopOrderResponse = shopOrderReadService.findByOutIdAndOutFrom(outerOrderId,MiddleChannel.JD.getValue());
        if (!shopOrderResponse.isSuccess()) {
            log.error("find order fail by outerOrderId {}, error {}", outerOrderId, shopOrderResponse.getError());
            return;
        }

        Optional<ShopOrder> shopOrderOptional = shopOrderResponse.getResult();
        if (!shopOrderOptional.isPresent()){
            log.error("find order fail by outerOrderId {}", outerOrderId);
            return;
        }
        ShopOrder shopOrder = shopOrderResponse.getResult().get();

        // 逆向取消
        try {
            orderWriteLogic.autoCancelShopOrder(shopOrder.getId());
        } catch (Exception e) {
            log.error("auto cancel jd shop order {} fail, error {} ", shopOrder.getId(), Throwables.getStackTraceAsString(e));
            return;
        }
    }


}
