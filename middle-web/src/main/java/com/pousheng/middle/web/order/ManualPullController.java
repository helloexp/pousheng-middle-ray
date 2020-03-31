package com.pousheng.middle.web.order;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.pousheng.middle.open.yunding.JdYunDingManualPullOrderLogic;
import com.pousheng.middle.order.dto.ManualPullOrderRequest;
import com.pousheng.middle.order.enums.MiddleChannel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.center.AfterSaleExchangeServiceRegistryCenter;
import io.terminus.open.client.center.AfterSaleServiceRegistryCenter;
import io.terminus.open.client.center.job.aftersale.api.AfterSaleExchangeReceiver;
import io.terminus.open.client.center.job.aftersale.api.AfterSaleReceiver;
import io.terminus.open.client.center.job.order.api.OrderReceiver;
import io.terminus.open.client.center.order.service.OrderServiceCenter;
import io.terminus.open.client.common.Pagination;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.order.dto.OpenClientAfterSale;
import io.terminus.open.client.order.dto.OpenClientFullOrder;
import io.terminus.open.client.order.dto.OpenClientReceiveFailDto;
import io.terminus.open.client.order.enums.OpenClientOrderStatus;
import io.terminus.open.client.order.service.OpenClientAfterSaleExchangeService;
import io.terminus.parana.common.utils.RespHelper;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 订单售后单预收单人工补偿拉取
 *
 * @author bernie
 * @date 2019/5/27
 */
@RestController
@Api("手动处理")
@Slf4j
public class ManualPullController {

    @Autowired
    private OrderServiceCenter orderServiceCenter;

    @Autowired
    private AfterSaleServiceRegistryCenter afterSaleServiceRegistryCenter;

    @Autowired
    private AfterSaleExchangeServiceRegistryCenter afterSaleExchangeServiceRegistryCenter;

    @Autowired
    private OpenClientAfterSaleExchangeService openClientAfterSaleExchangeService;

    @Autowired
    private OpenShopReadService openShopReadService;

    @Autowired
    private AfterSaleReceiver afterSaleReceiver;

    @Autowired
    private AfterSaleExchangeReceiver afterSaleExchangeReceiver;

    @Autowired
    private OrderReceiver orderReceiver;

    @Autowired
    private JdYunDingManualPullOrderLogic jdYunDingManualPullOrderLogic;

    private final static int pageSize = 50;

    private final static String SALE_ORDER = "1";
    private final static String PRE_SALE_ORDER = "2";
    private final static String AFTER_ORDER = "3";

    private final static DateTimeFormatter dateTimeFormatter= DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");

    @ApiOperation("手动拉取订单")
    @RequestMapping(value = "api/order/manual/pull", method = RequestMethod.GET,
        produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<List<String>> manualPullOrder(ManualPullOrderRequest request) {

        try {

            if(!StringUtils.isEmpty(request.getPullStartDate())){

                request.setStartDate(DateTime.parse(request.getPullStartDate(),dateTimeFormatter).toDate());
            }
            if(!StringUtils.isEmpty(request.getPullEndDate())){
                request.setEndDate(DateTime.parse(request.getPullEndDate(),dateTimeFormatter).toDate());
            }
            log.debug("manual.pull.request={}", JsonMapper.nonEmptyMapper().toJson(request));
            checkoutParam(request);
            String pullCategory = request.getOrderCategory();
            OpenShop openShop = RespHelper.orServEx(openShopReadService.findById(request.getOpenShopId()));
            if (Objects.isNull(openShop) && !openShop.enable()) {
                log.info("shop.status.disable,shopid={},status={}", openShop.getId(), openShop.getStatus());
                return Response.fail("shopid = " + openShop.getId() + "is disable");
            }
            //拉取京东云鼎订单
            if (checkIsYundingShop(openShop)) {
                jdYunDingManualPullOrderLogic.manualPullOrderFromYunding(request);
            } else {
                if (Objects.equals(pullCategory, SALE_ORDER)) {
                    return pullSaleOrder(request, openShop);
                } else if (Objects.equals(pullCategory, PRE_SALE_ORDER)) {
                    return pullPreSaleOrder(request, openShop);
                } else if (Objects.equals(pullCategory, AFTER_ORDER)) {
                    return pullAfterOrder(request, openShop);

                }
            }
        } catch (JsonResponseException jre) {
            throw jre;
        } catch (Exception e) {
            log.error("pull.order.fail,request={},response={}", JsonMapper.nonEmptyMapper().toJson(request),
                Throwables.getStackTraceAsString(e));
            return Response.fail("pull.order.fail");
        }
        return Response.ok(Lists.newArrayList());
    }

    public Boolean checkoutParam(ManualPullOrderRequest request) {

        if (StringUtils.isEmpty(request.getOpenShopId())) {
            throw new JsonResponseException("open.shop.id.not.allow.null");
        }
        if (StringUtils.isEmpty(request.getOrderCategory())) {
            throw new JsonResponseException("order.category.not.allow.null");
        }
        if (StringUtils.isEmpty(request.getOuterOrderId()) && Objects.isNull(request.getPullStartDate())) {
            throw new JsonResponseException("pull.start.time.not.allow.null");
        }

        return Boolean.TRUE;

    }

    /**
     * 拉取预售单
     */
    private Response<List<String>> pullSaleOrder(ManualPullOrderRequest request, OpenShop openShop) {

        List<OpenClientFullOrder> openClientFullOrderList = Lists.newArrayList();

        //按照外部单号+店铺拉取
        if (!StringUtils.isEmpty(request.getOuterOrderId())) {
            log.debug("pull.sale.order.by.order {} start", request.getOuterOrderId());
            Response<OpenClientFullOrder> openClientFullOrderRes = orderServiceCenter.findById(
                request.getOpenShopId(), request.getOuterOrderId());
            if (Objects.isNull(openClientFullOrderRes) || !openClientFullOrderRes.isSuccess()) {
                return Response.fail("pull.order.fail");
            }
            if (Objects.nonNull(
                openClientFullOrderRes.getResult())) {
                openClientFullOrderList.add(openClientFullOrderRes.getResult());
            }
            log.debug("pull.sale.order.by.outorderId {} end", request.getOuterOrderId());
        } else {
            log.debug("pull.sale.order.by.create.time start");
            int pageNo = 1;
            while (true) {
                Response<Pagination<OpenClientFullOrder>> response = orderServiceCenter.searchOrderByCreateTime(
                    request.getOpenShopId(), OpenClientOrderStatus.PAID,
                    request.getStartDate(),
                    request.getEndDate(), pageNo, pageSize);
                if (Objects.isNull(response) || !response.isSuccess()) {
                    return Response.fail("pull.order.fail");
                }
                if (CollectionUtils.isEmpty(response.getResult().getData())) {
                    break;
                }
                openClientFullOrderList.addAll(response.getResult().getData());
                if (!response.getResult().isHasNext()) {
                    break;
                }
                pageNo++;
            }
            log.debug("pull.sale.order.by.create.time end total={}", openClientFullOrderList.size());
        }
        log.debug("pull.sale.order.total={}", openClientFullOrderList.size());
        List<OpenClientReceiveFailDto> receiveFailDtos = null;
        //订单处理
        if (!CollectionUtils.isEmpty(openClientFullOrderList)) {
            receiveFailDtos = orderReceiver.receiveOrder(OpenClientShop.from(openShop), openClientFullOrderList);
        }
        if (!CollectionUtils.isEmpty(receiveFailDtos)) {
            return Response.ok(receiveFailDtos.stream().map(receiveFailDto -> {
                OpenClientFullOrder openClientFullOrder = (OpenClientFullOrder)receiveFailDto.getOrder();
                return openClientFullOrder.getOrderId();
            }).collect(Collectors.toList()));
        }
        return Response.ok(Lists.newArrayList());
    }

    /**
     * 拉取预售单
     */
    private Response<List<String>> pullPreSaleOrder(ManualPullOrderRequest request, OpenShop openShop) {

        List<OpenClientFullOrder> openClientFullOrderList = Lists.newArrayList();

        //按照外部单号+店铺拉取
        if (!StringUtils.isEmpty(request.getOuterOrderId())) {
            log.debug("pull.pre.order.by.order {} start", request.getOuterOrderId());
            Response<OpenClientFullOrder> openClientFullOrderRes = orderServiceCenter.findPresaleById(
                request.getOpenShopId(), request.getOuterOrderId());

            if (Objects.isNull(openClientFullOrderRes) || !openClientFullOrderRes.isSuccess()) {
                return Response.fail("pull.order.fail");
            }

            if (Objects.nonNull(
                openClientFullOrderRes.getResult())) {
                openClientFullOrderList.add(openClientFullOrderRes.getResult());
            }
            log.debug("pull.pre.order.by.outorderId {} end", request.getOuterOrderId());
        } else {
            log.debug("pull.pre.order.by.create.time start");
            int pageNo = 1;
            while (true) {
                Response<Pagination<OpenClientFullOrder>> response = orderServiceCenter.searchPresale(
                    request.getOpenShopId(), request.getStartDate(),
                    request.getEndDate(), pageNo, pageSize);

                if (Objects.isNull(response) || !response.isSuccess()) {
                    return Response.fail("pull.order.fail");
                }
                if (CollectionUtils.isEmpty(response.getResult().getData())) {
                    break;
                }
                openClientFullOrderList.addAll(response.getResult().getData());
                if (!response.getResult().isHasNext()) {
                    break;
                }
                pageNo++;
            }
            log.debug("pull.pre.order.by.create.time end");
        }
        log.debug("pull.sale.order.total={}", openClientFullOrderList.size());
        //订单处理
        List<OpenClientReceiveFailDto> receiveFailDtos = null;
        if (!CollectionUtils.isEmpty(openClientFullOrderList)) {
            receiveFailDtos = orderReceiver.receiveOrder(OpenClientShop.from(openShop), openClientFullOrderList);
        }
        if (!CollectionUtils.isEmpty(receiveFailDtos)) {
            return Response.ok(receiveFailDtos.stream().map(receiveFailDto -> {
                OpenClientFullOrder openClientFullOrder = (OpenClientFullOrder)receiveFailDto.getOrder();
                return openClientFullOrder.getOrderId();
            }).collect(Collectors.toList()));
        }
        return Response.ok(Lists.newArrayList());
    }

    /**
     * 拉取售后单
     */
    private Response<List<String>> pullAfterOrder(ManualPullOrderRequest request, OpenShop openShop) {

        List<OpenClientAfterSale> afterSaleList = Lists.newArrayList();

        List<OpenClientAfterSale> afterExchangeList = Lists.newArrayList();

        //按照外部单号+店铺拉取
        if (!StringUtils.isEmpty(request.getOuterOrderId())) {
            log.debug("pull.after.order.by.after.order {} start", request.getOuterOrderId());
            Response<OpenClientAfterSale> openClientAfterSaleResponse = afterSaleServiceRegistryCenter
                .getAfterSaleService(
                    openShop.getChannel()).findByAfterSaleId(
                    request.getOpenShopId(), request.getOuterOrderId());

            if (Objects.isNull(openClientAfterSaleResponse) || !openClientAfterSaleResponse.isSuccess()) {
                return Response.fail("pull.order.fail");
            }

            if (Objects
                .nonNull(openClientAfterSaleResponse.getResult())) {
                afterSaleList.add(openClientAfterSaleResponse.getResult());
            }
            //如果订单没有拉取到并且是天猫渠道，也有可能是换货单
            if (CollectionUtils.isEmpty(afterSaleList) && Objects.equals(openShop.getChannel(),
                MiddleChannel.TAOBAO.getValue())) {
                Response<OpenClientAfterSale> openClientAfterSaleExchangeResponse = openClientAfterSaleExchangeService
                    .findByAfterSaleId(request.getOpenShopId(), request.getOuterOrderId());
                if (openClientAfterSaleExchangeResponse.isSuccess() && Objects.nonNull(
                    openClientAfterSaleExchangeResponse.getResult())) {
                    afterExchangeList.add(openClientAfterSaleExchangeResponse.getResult());
                }
            }
            log.debug("pull.after.order.by.after.order {} end", request.getOuterOrderId());
        } else {
            log.debug("pull.after.order.by.create.time start");
            int pageNo = 1;
            while (true) {

                Response<Pagination<OpenClientAfterSale>> response = afterSaleServiceRegistryCenter.getAfterSaleService(
                    openShop.getChannel()).searchAfterSale(request.getOpenShopId(), null, request.getStartDate(),
                    request.getEndDate(), pageNo, pageSize);
                if (Objects.isNull(response) || !response.isSuccess()) {
                    return Response.fail("pull.order.fail");
                }
                if (CollectionUtils.isEmpty(response.getResult().getData())) {
                    break;
                }
                afterSaleList.addAll(response.getResult().getData());
                if (!response.getResult().isHasNext()) {
                    break;
                }
                pageNo++;

            }
            //店铺是天猫则去拉取天猫
            if (Objects.equals(openShop.getChannel(), MiddleChannel.TAOBAO.getValue())) {
                pageNo = 1;
                while (true) {
                    //如果天猫的需要把换货单也拉取

                    Response<Pagination<OpenClientAfterSale>> response = afterSaleExchangeServiceRegistryCenter
                        .getAfterSaleExchangeService(openShop.getChannel())
                        .searchAfterSaleExchange(request.getOpenShopId(), null, request.getStartDate(),
                            request.getEndDate(), pageNo, pageSize);
                    if (Objects.isNull(response) || !response.isSuccess()) {
                        break;
                    }
                    if (CollectionUtils.isEmpty(response.getResult().getData())) {
                        break;
                    }
                    afterExchangeList.addAll(response.getResult().getData());
                    if (!response.getResult().isHasNext()) {
                        break;
                    }
                    pageNo++;
                }
            }
            log.debug("pull.after.order.by.create.time end");
        }
        log.debug("pull.sale.order afterTotal={} exchangeTotal={}", afterSaleList.size(), afterExchangeList.size());
        List<OpenClientReceiveFailDto> receiveFailDtos = null;
        //处理售后单到中台
        if (!CollectionUtils.isEmpty(afterSaleList)) {
            receiveFailDtos = afterSaleReceiver.receiveAfterSale(OpenClientShop.from(openShop), afterSaleList);
            if (!CollectionUtils.isEmpty(receiveFailDtos)) {
                return Response.ok(receiveFailDtos.stream().map(receiveFailDto -> {
                    OpenClientAfterSale openClientFullOrder = (OpenClientAfterSale)receiveFailDto.getOrder();
                    return openClientFullOrder.getOpenOrderId();
                }).collect(Collectors.toList()));
            }
        }
        //处理天猫换货到中台,换货拉单失败暂不处理
        if (!CollectionUtils.isEmpty(afterExchangeList)) {
            afterSaleExchangeReceiver.receiveAfterSaleExchange(OpenClientShop.from(openShop), afterExchangeList);
        }
        return Response.ok(Lists.newArrayList());
    }

    private Boolean checkIsYundingShop(OpenShop openShop) {
        if (Objects.equals(openShop.getChannel(), MiddleChannel.JD.getValue())) {
            if (openShop.getExtra().get("isYunDing") != null && Objects.equals(openShop.getExtra().get("isYunDing"),
                "true")) {
                log.info("openShopId is yunding shop");
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

}
