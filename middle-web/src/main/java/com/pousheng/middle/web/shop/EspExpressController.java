package com.pousheng.middle.web.shop;

import cn.hutool.core.date.DatePattern;
import com.alibaba.fastjson.JSONObject;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.web.express.esp.ConfigCacheService;
import com.pousheng.middle.web.express.esp.ExpressProcessService;
import com.pousheng.middle.web.express.esp.bean.ESPExpressCodeSendResponse;
import com.pousheng.middle.web.express.esp.bean.MposResponse;
import com.pousheng.middle.web.express.esp.jobs.EspExpressRetryJob;
import com.pousheng.middle.web.express.esp.jobs.EspExpressSynJob;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.common.model.Response;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.order.service.ShopOrderReadService;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Desc 中台快递服务
 * @Author GuoFeng
 * @Date 2019/9/4
 */

@Api(description = "快递API")
@RestController
@Slf4j
@RequestMapping("/api/express")
public class EspExpressController {

    @Autowired
    private ExpressProcessService expressProcessService;

    @Autowired
    private ShopOrderReadService shopOrderReadService;

    @Autowired
    private OrderShipmentReadService orderShipmentReadService;

    @Autowired
    private ShipmentReadService shipmentReadService;

    @Autowired
    private EspExpressSynJob espExpressSynJob;

    @Autowired
    private ConfigCacheService configCacheService;

    @Autowired
    private EspExpressRetryJob espExpressRetryJob;


    /**
     * @param shopUserName
     * @param middleShipmentId
     * @param force            强制获取快递单号，不检查配置
     * @return
     */
    @ApiOperation("获取esp快递单号")
    @RequestMapping(value = "/esp/expressno", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public MposResponse getExpressNo(String shopUserName, String middleShipmentId, boolean force) {
        MposResponse mposResponse = new MposResponse();
        if (StringUtils.isEmpty(shopUserName) || StringUtils.isEmpty(middleShipmentId)) {
            mposResponse.setSuccess(true);
            mposResponse.setUseMiddleService(false);
            mposResponse.setProcess(false);
            mposResponse.setMsg("参数不能为空");
            return mposResponse;
        }

        boolean useESP;
        if (force) {
            //强制使用ESP服务
            useESP = true;
        } else {
            useESP = expressProcessService.checkShopUseESP(shopUserName);
        }
        log.info("使用ESP服务:{}", useESP);
        if (useESP) {
            Response<Shipment> shipmentResponse = shipmentReadService.findById(Long.valueOf(middleShipmentId));
            if (shipmentResponse.isSuccess()) {
                Shipment shipment = shipmentResponse.getResult();

                Response<OrderShipment> orderShipmentResponse = orderShipmentReadService.findByShipmentId(shipment.getId());
                if (orderShipmentResponse.isSuccess()) {
                    OrderShipment orderShipment = orderShipmentResponse.getResult();
                    Long orderId = orderShipment.getOrderId();
                    Response<ShopOrder> shopOrderResponse = shopOrderReadService.findById(orderId);
                    if (shopOrderResponse.isSuccess()) {
                        ShopOrder shopOrder = shopOrderResponse.getResult();

                        mposResponse = expressProcessService.getExpressNo(shopOrder, shipment);
                        return mposResponse;
                    } else {
                        log.error("订单查询失败,orderId:{}:", orderId);
                    }
                } else {
                    log.error("orderShipment查询失败,发货单id:{}:", middleShipmentId);
                }
            } else {
                log.error("发货单查询失败,id:{}:", middleShipmentId);
            }

            mposResponse.setSuccess(false);
            mposResponse.setMsg("获取单号出错");
            return mposResponse;
        } else {
            //不使用esp服务
            mposResponse.setSuccess(true);
            mposResponse.setUseMiddleService(false);
            return mposResponse;
        }
    }

    @ApiOperation("取消esp快递单")
    @RequestMapping(value = "/esp/expressno/cancel", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public MposResponse cancelExpressNo(String shopUserName, String middleShipmentId) {
        MposResponse mposResponse = new MposResponse();

        if (StringUtils.isEmpty(shopUserName) || StringUtils.isEmpty(middleShipmentId)) {
            mposResponse.setSuccess(true);
            mposResponse.setUseMiddleService(false);
            mposResponse.setProcess(false);
            mposResponse.setMsg("参数不能为空");
            return mposResponse;
        }

        boolean useESP = expressProcessService.checkShopUseESP(shopUserName);
        if (useESP) {
            Response<Shipment> shipmentResponse = shipmentReadService.findById(Long.valueOf(middleShipmentId));
            if (shipmentResponse.isSuccess()) {
                Shipment shipment = shipmentResponse.getResult();
                ESPExpressCodeSendResponse espExpressCodeSendResponse = expressProcessService.cancelExpressNo(shipment);
                log.info("取消快递结果:{}", espExpressCodeSendResponse);
                mposResponse.setUseMiddleService(true);
                mposResponse.setSuccess(true);
                if ("00000".equals(espExpressCodeSendResponse.getCode())) {
                    mposResponse.setProcess(true);
                } else {
                    mposResponse.setProcess(false);
                    mposResponse.setMsg(espExpressCodeSendResponse.getMessage());
                }
                return mposResponse;
            }
        } else {
            mposResponse.setProcess(false);
            mposResponse.setUseMiddleService(false);
            mposResponse.setSuccess(true);
            return mposResponse;
        }
        return null;
    }


    /**
     * 调用快递回传任务
     *
     * @param startDate
     * @param endDate
     * @return
     */
    @RequestMapping(value = "/esp/express/job", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String runJob(String startDate, String endDate) {
        if (StringUtils.isEmpty(startDate) || StringUtils.isEmpty(endDate)) {
            return "参数不能为空";
        }
        DateTime start = DateTime.parse(startDate, DateTimeFormat.forPattern(DatePattern.NORM_DATETIME_PATTERN));
        DateTime end = DateTime.parse(endDate, DateTimeFormat.forPattern(DatePattern.NORM_DATETIME_PATTERN));
        espExpressSynJob.runJob(start, end);
        return "true";
    }


    /**
     * 仓库订单回传测试
     *
     * @param shopOrderId
     * @param shipmentCorpCode
     * @param shipmentSerialNo
     * @return
     */
    @RequestMapping(value = "/esp/warehouse/test", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ESPExpressCodeSendResponse testSendWarehouseShipmentInfo(String shopOrderId, String shipmentId, String shipmentCorpCode, String shipmentSerialNo) {
        Response<ShopOrder> shopOrderResponse = shopOrderReadService.findById(Long.valueOf(shopOrderId));
        if (shopOrderResponse.isSuccess()) {
            ShopOrder shopOrder = shopOrderResponse.getResult();
            Response<Shipment> shipmentResponse = shipmentReadService.findById(Long.valueOf(shipmentId));
            if (shipmentResponse.isSuccess()) {
                Shipment shipment = shipmentResponse.getResult();
                ESPExpressCodeSendResponse espExpressCodeSendResponse = expressProcessService.sendWarehouseExpressNo(shopOrder, shipment, shipmentCorpCode, shipmentSerialNo);
                return espExpressCodeSendResponse;
            }
        }
        return null;
    }

    /**
     * 门店回传测试
     *
     * @param shopOrderId
     * @param shipmentId
     * @param shipmentCorpCode
     * @param shipmentSerialNo
     * @return
     */
    @RequestMapping(value = "/esp/store/test", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public JSONObject testSendStoreShipmentInfo(String shopOrderId, String shipmentId, String shipmentCorpCode, String shipmentSerialNo) {
        Response<ShopOrder> shopOrderResponse = shopOrderReadService.findById(Long.valueOf(shopOrderId));
        if (shopOrderResponse.isSuccess()) {
            ShopOrder shopOrder = shopOrderResponse.getResult();
            Response<Shipment> shipmentResponse = shipmentReadService.findById(Long.valueOf(shipmentId));
            if (shipmentResponse.isSuccess()) {
                Shipment shipment = shipmentResponse.getResult();
                JSONObject espExpressCodeSendResponse = expressProcessService.sendExpressNo(shopOrder, shipment, shipmentCorpCode, shipmentSerialNo);
                return espExpressCodeSendResponse;
            }
        }
        return null;
    }

    @RequestMapping(value = "/esp/cache/clean", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean cleanCache() {
        return configCacheService.cleanCache();
    }


    @RequestMapping(value = "/esp/retry", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public boolean testEspExpressRetryJob() {
        espExpressRetryJob.retry();
        return true;
    }
}
