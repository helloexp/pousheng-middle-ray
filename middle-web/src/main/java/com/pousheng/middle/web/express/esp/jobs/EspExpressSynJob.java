package com.pousheng.middle.web.express.esp.jobs;

import cn.hutool.core.date.DatePattern;
import cn.hutool.db.Page;
import com.alibaba.fastjson.JSONObject;
import com.google.common.eventbus.EventBus;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.model.PoushengEspLog;
import com.pousheng.middle.order.service.MiddleShipmentReadService;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.express.ExpressConfigType;
import com.pousheng.middle.web.express.ExpressLogType;
import com.pousheng.middle.web.express.esp.ConfigCacheService;
import com.pousheng.middle.web.express.esp.ExpressProcessService;
import com.pousheng.middle.web.express.esp.bean.ESPExpressCodeSendResponse;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.parana.order.impl.dao.ShopOrderDao;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShopOrder;
import io.terminus.parana.util.ShipmentEncryptUtil;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * @Desc esp快递信息回传
 * @Author GuoFeng
 * @Date 2019/9/10
 */
@Service
@Slf4j
public class EspExpressSynJob {

    @Autowired
    private HostLeader hostLeader;

    @Autowired
    private JedisTemplate jedisTemplate;

    @Autowired
    private MiddleShipmentReadService shipmentReadService;

    private static final Long limit = 500L;

    //redis中保存上次任务运行获取数据时间的key
    private static final String EXPRESS_JOB_LAST_TIME_KEY = "express.job.last.time";

    private static final Integer data_minutes = 5;

    @Autowired
    private ExpressProcessService expressProcessService;

    @Autowired
    private OrderShipmentReadService orderShipmentReadService;

    @Autowired
    private ShopOrderDao shopOrderDao;

    @Autowired
    private EventBus eventBus;

    @Autowired
    private OpenShopCacher openShopCacher;

    @Autowired
    private WarehouseCacher warehouseCacher;


    @Autowired
    private ConfigCacheService configCacheService;
    /**
     * 每隔5分钟回传一次快递信息给esp
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    public void process() {
        //检查是否主节点
        if (!hostLeader.isLeader()) {
            log.info("EspExpressSynJob: current leader is {}, skip", hostLeader.currentLeaderId());
            return;
        }

        //检查配置
        String config = configCacheService.getUnchecked(ExpressConfigType.push_express);
        if (StringUtils.hasText(config) && ("true".equals(config) || "false".equals(config))) {
            if (!Boolean.valueOf(config)) {
                log.info("未开启推送快递，要开启推送，请将content设置为true，生效时间为30分钟后");
                return;
            }
        } else {
            log.info("推送快递没有设置或者设置值错误，默认不运行");
            return;
        }

        DateTime currentTime = DateTime.now();
        String currentTimeString = currentTime.toString(DatePattern.NORM_DATETIME_PATTERN);
        log.info("{}:开始回传ESP快递信息", currentTimeString);
        try {
            String lastTime = jedisTemplate.execute(jedis -> {
                return jedis.get(EXPRESS_JOB_LAST_TIME_KEY);
            });
            DateTime startDate;
            if (StringUtils.hasText(lastTime)) {
                startDate = DateTime.parse(lastTime, DateTimeFormat.forPattern(DatePattern.NORM_DATETIME_PATTERN));
            } else {
                startDate = currentTime.minusMinutes(5);
            }
            runJob(startDate, null);
        } catch (Exception e) {
            log.info("回传ESP任务出错");
            log.error("错误信息:", e);
        }
        log.info("{}:回传ESP快递信息结束", currentTimeString);
    }

    public void runJob(DateTime startDate, DateTime endDate) {
        boolean auto = false;
        //endDate 是null，说明是任务调用，不是手动调用
        if (endDate == null) {
            endDate = DateTime.now();
            //减去1分钟防止漏单，会有1分钟的重复数据推送
            startDate = startDate.minusMinutes(1 + data_minutes);
            auto = true;
        }
        String startTimeString = startDate.toString(DatePattern.NORM_DATETIME_PATTERN);
        String endTimeString = endDate.toString(DatePattern.NORM_DATETIME_PATTERN);
        log.info("数据开始时间:{}, 数据结束时间:{}", startTimeString, endTimeString);

        int pageNumber = 1;
        while (true) {
            Page page = new Page(pageNumber, limit.intValue());
            Response<Paging<Shipment>> shipmentsResponse = shipmentReadService.findShipmentCustom(MiddleShipmentsStatus.SHIPPED.getValue(), startDate.toDate(), endDate.toDate(), limit, page.getStartPosition());
            if (shipmentsResponse.isSuccess()) {
                Paging<Shipment> shipmentPaging = shipmentsResponse.getResult();
                Long total = shipmentPaging.getTotal();
                List<Shipment> listToProcess = shipmentPaging.getData();
                log.info("共{}条数据，正在处理第{}页, 共{}条", total, pageNumber, listToProcess.size());
                if (total > 0) {
                    sendExpress(listToProcess);
                }
                //不相等说明数据取尽了
                if (listToProcess.size() < limit) {
                    break;
                }
            } else {
                break;
            }
            pageNumber++;
        }

        //如果是任务调用，就更新数据时间；手动调用不更新
        if (auto) {
            jedisTemplate.execute(jedis -> {
                return jedis.set(EXPRESS_JOB_LAST_TIME_KEY, endTimeString);
            });
        }
    }

    public void sendExpress(List<Shipment> data) {
        if (data != null && data.size() > 0) {
            for (Shipment shipment : data) {
                log.info("正在处理发货单:{}", shipment.getShipmentCode());
                String failMsg = null;
                try {
                    //解密收货信息
                    ShipmentEncryptUtil.decryptReceiverInfo(shipment);
                    Integer shipWay = shipment.getShipWay();
                    Response<OrderShipment> orderShipmentResponse = orderShipmentReadService.findByShipmentId(shipment.getId());
                    if (orderShipmentResponse.isSuccess()) {
                        OrderShipment orderShipment = orderShipmentResponse.getResult();
                        Long orderId = orderShipment.getOrderId();
                        ShopOrder shopOrder = shopOrderDao.findById(orderId);
                        int status = -1;
                        String errorMsg = null;
                        if (shopOrder != null) {
                            switch (shipWay) {
                                //店发
                                case 1:
                                    JSONObject jsonObject = expressProcessService.sendExpressNo(shopOrder, shipment, shipment.getShipmentCorpCode(), shipment.getShipmentSerialNo());
                                    String code = jsonObject.getString("code");
                                    String message = jsonObject.getString("message");
                                    log.info("店发单:{}, 回传结果code:{}, message:{}", shipment.getShipmentCode(), code, message);
                                    if ("00000".equals(code)) {
                                        status = 1;
                                    } else {
                                        errorMsg = message;
                                    }
                                    break;
                                //仓发
                                case 2:
                                    //仓发只推oxo渠道，跳过其他渠道
                                    Long shopId = shipment.getShopId();
                                    OpenShop openShop = openShopCacher.findById(shopId);
                                    String channel = openShop.getChannel();
                                    if (!MiddleChannel.VIPOXO.getValue().equals(channel)) {
                                        continue;
                                    }
                                    //仓发接口，快递代码必须加oxo后缀
                                    String corpCode = shipment.getShipmentCorpCode();
                                    String corpCodeWithOXO = corpCode + "OXO";
                                    ESPExpressCodeSendResponse espExpressCodeSendResponse = expressProcessService.sendWarehouseExpressNo(shopOrder, shipment, corpCodeWithOXO, shipment.getShipmentSerialNo());
                                    log.info("仓发单:{}, 回传结果code:{}, message:{}", shipment.getShipmentCode(), espExpressCodeSendResponse.getCode(), espExpressCodeSendResponse.getMessage());
                                    //记录日志
                                    if ("00000".equals(espExpressCodeSendResponse.getCode())) {
                                        status = 1;
                                    } else {
                                        errorMsg = espExpressCodeSendResponse.getMessage();
                                    }
                                    break;
                            }
                            //记录日志
                            PoushengEspLog poushengEspLog = buildLog(shipment, shopOrder, status, errorMsg);
                            eventBus.post(poushengEspLog);
                        } else {
                            log.info("没有查询到订单:{}", orderId);
                            failMsg = "没有查询到订单:" + orderId;
                        }
                    } else {
                        log.info("没有查询到orderShipment, shipmentId:{}", shipment.getId());
                        failMsg = "没有查询到发货单:" + shipment.getId();
                    }
                } catch (Exception e) {
                    log.info("处理发货单出错，发货单号:{}", shipment.getShipmentCode());
                    log.error("错误信息:", e);
                    failMsg = "处理发货单出错:" + e.toString();
                }

                //如果出错，记录下错误，防止出粗了日志中没有
                if (failMsg != null) {
                    PoushengEspLog preLog = buildPreLog(shipment, -1);
                    preLog.setResponseContent(failMsg);
                    eventBus.post(preLog);
                }
            }
        } else {
            log.info("没有待处理的数据");
        }
    }

    private PoushengEspLog buildLog(Shipment shipment, ShopOrder shopOrder, Integer status, String errorMsg) {
        PoushengEspLog log = new PoushengEspLog();
        Date now = new Date();
        log.setCreatedAt(now);
        log.setExpressCompanyCode(shipment.getShipmentCorpCode());
        log.setExpressNo(shipment.getShipmentSerialNo());
        log.setMiddleShipmentNo(shipment.getShipmentCode());
        log.setOriginalOrderNo(shopOrder.getOrderCode());
        log.setShipmentCreatedAt(shipment.getCreatedAt());
        log.setShipmentUpdatedAt(shipment.getUpdatedAt());
        //1成功，-1失败
        log.setSynStatus(status);
        log.setResponseContent(errorMsg);
        if (shipment.getShipWay() == 1) {
            //店发
            log.setType(ExpressLogType.stroe_express_log);
            OpenShop openShop = openShopCacher.findById(shipment.getShipId());
            log.setStroeCode(openShop.getAppKey());
            log.setStroeName(openShop.getShopName());
        } else {
            //仓发
            log.setType(ExpressLogType.warehouse_express_log);
            WarehouseDTO warehouseDTO = warehouseCacher.findById(shipment.getShipId());
            log.setStroeCode(warehouseDTO.getWarehouseCode());
            log.setStroeName(warehouseDTO.getWarehouseName());
        }
        log.setUpdatedAt(now);

        return log;
    }


    /**
     * @param shipment
     * @return
     */
    private PoushengEspLog buildPreLog(Shipment shipment, Integer status) {
        PoushengEspLog log = new PoushengEspLog();
        Date now = new Date();
        log.setCreatedAt(now);
        log.setExpressCompanyCode(shipment.getShipmentCorpCode());
        log.setExpressNo(shipment.getShipmentSerialNo());
        log.setMiddleShipmentNo(shipment.getShipmentCode());
        log.setShipmentCreatedAt(shipment.getCreatedAt());
        log.setShipmentUpdatedAt(shipment.getUpdatedAt());
        //1成功，-1失败
        log.setSynStatus(status);
        if (shipment.getShipWay() == 1) {
            //店发
            log.setType(ExpressLogType.stroe_express_log);
            OpenShop openShop = openShopCacher.findById(shipment.getShipId());
            log.setStroeCode(openShop.getAppKey());
            log.setStroeName(openShop.getShopName());
        } else {
            //仓发
            log.setType(ExpressLogType.warehouse_express_log);
            WarehouseDTO warehouseDTO = warehouseCacher.findById(shipment.getShipId());
            log.setStroeCode(warehouseDTO.getWarehouseCode());
            log.setStroeName(warehouseDTO.getWarehouseName());
        }
        log.setUpdatedAt(now);

        return log;
    }
}
