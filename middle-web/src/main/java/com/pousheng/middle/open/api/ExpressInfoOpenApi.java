package com.pousheng.middle.open.api;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.middle.open.api.dto.ExpressInfoResponse;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.pampas.openplatform.annotations.OpenBean;
import io.terminus.pampas.openplatform.annotations.OpenMethod;
import io.terminus.pampas.openplatform.entity.OPResponse;
import io.terminus.parana.order.dto.ExpressDetails;
import io.terminus.parana.order.dto.PushExpressInfos;
import io.terminus.parana.order.enums.ShipmentExpressStatus;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.model.ShipmentExpress;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.parana.order.service.ShipmentWriteService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.constraints.NotNull;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * ESP推送快递信息
 * @author mz
 * @Date 2018-05-11
 * pousheng-middle
 */
@OpenBean
@Slf4j
public class ExpressInfoOpenApi {

    @Autowired
    private ShipmentReadService shipmentReadService;
    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;

    @Autowired
    private ShipmentReadLogic shipmentReadLogic;

    private static final JsonMapper JSON_MAPPER = JsonMapper.nonEmptyMapper();

    private static final String UPDATE_EXPRESS_INFOS_STATUS = "shipmentExpress.update.fail";

    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final SimpleDateFormat simpleDateFormat1 = new SimpleDateFormat("yyyyMMddHHmmss");

    /**
     * 接收ESP推送的快递单信息
     * @param data 处理结果 约束大小为50个
     * @return 是否同步成功
     */
    @OpenMethod(key = "sync.shipment.express.info.api",  paramNames = {"data"}, httpMethods = RequestMethod.POST)
    public OPResponse<ExpressInfoResponse> receiveExpressInfos(@NotNull(message = "handle.data.is.null") String data) {
        log.info("RECEIVE-ESP-PUSH-EXPRESSINFOS-start results is:{} ", data);
        ExpressInfoResponse error = new ExpressInfoResponse();
        try {
            //根据上送的发货单号以及快递单号查询是否存在,存在更新不存在新增
            List<PushExpressInfos> expressDetailsList = JsonMapper.nonEmptyMapper().fromJson(data, JsonMapper.nonEmptyMapper().createCollectionType(List.class,PushExpressInfos.class));
            ExecutorService executorService = Executors.newFixedThreadPool(10);
            CompletionService<Response<Boolean>> completionService = new ExecutorCompletionService<Response<Boolean>>(executorService);
            for (int i = 0; i < expressDetailsList.size(); i++) {
                // 向线程池提交任务
                completionService.submit(new DealExpressInfoTask(shipmentReadService, shipmentWriteService,expressDetailsList.get(i)));
            }
            int count = 0;
            List<String> failedShipmentCodes = Lists.newArrayList();
            for (int i = 0; i < expressDetailsList.size(); i++) {
                // 获取结果
                Response<Boolean> response = completionService.take().get();
                if (!response.isSuccess()){
                    count ++;
                    failedShipmentCodes.add(response.getError().substring(UPDATE_EXPRESS_INFOS_STATUS.length()));
                }
            }
            executorService.shutdown();
            if ( count >= 1){
                error.setFailShipmentCodes(failedShipmentCodes);
                error.setErrorCode(UPDATE_EXPRESS_INFOS_STATUS);
                error.setErrorMsg("发货单快递详情更新失败");
                String reason = JsonMapper.nonEmptyMapper().toJson(error);
                return OPResponse.fail(error.getErrorCode(),reason);
            }
            return OPResponse.ok();


        } catch (Exception e){
            log.error("RECEIVE-ESP-PUSH-EXPRESSINFO error,cause:{}",Throwables.getStackTraceAsString(e));
            String reason = JsonMapper.nonEmptyMapper().toJson(error);
            return OPResponse.fail(reason);
        }
    }

    private class DealExpressInfoTask implements Callable<Response<Boolean>> {

        private ShipmentReadService shipmentReadService;

        private ShipmentWriteService shipmentWriteService;

        private PushExpressInfos pushExpressInfos;

        /**
         * 处理发货单快递信息
         * @param shipmentReadService
         * @param shipmentWriteService
         * @param pushExpressInfos
         */
        public DealExpressInfoTask(ShipmentReadService shipmentReadService, ShipmentWriteService shipmentWriteService,
                                   PushExpressInfos pushExpressInfos) {
            this.shipmentReadService = shipmentReadService;
            this.shipmentWriteService = shipmentWriteService;
            this.pushExpressInfos = pushExpressInfos;
        }

        @Override
        public Response<Boolean> call()  {
            try {
                Response<Boolean> result = null;
                //判断数据有否问题
                if( StringUtils.isEmpty(pushExpressInfos.getShipmentCode()) || StringUtils.isEmpty(pushExpressInfos.getExpressStatus())
                       || StringUtils.isEmpty(pushExpressInfos.getExpressNo()) || StringUtils.isEmpty(pushExpressInfos.getExpressCompanyCode())
                        || StringUtils.isEmpty(pushExpressInfos.getExpressCompanyName()) || pushExpressInfos.getExpressDetails() == null){
                    return Response.fail("shipmentExpress.receive.fail"+ pushExpressInfos.getShipmentCode());
                }
                //更新发货订单表快递单状态查询一下是否存在如果不存在直接返回
                Shipment shipment = shipmentReadLogic.findShipmentByShipmentCode(pushExpressInfos.getShipmentCode());
                if (shipment == null){
                    return Response.fail("find.shipment.fail"+ pushExpressInfos.getShipmentCode());
                }
                shipmentWriteService.updateExpressStatusByShipmentCode(pushExpressInfos.getShipmentCode(),ShipmentExpressStatus.convert(pushExpressInfos.getExpressStatus()).value());
                Response<ShipmentExpress> shipmentExpressRes = shipmentReadService.findShipmentExpress(pushExpressInfos.getShipmentCode(), pushExpressInfos.getExpressNo());
                Map<String, String> extraMap = Maps.newHashMap();
                //转化一下expressDetail里的nodeAt格式
                convertDateFormate(pushExpressInfos.getExpressDetails());
                extraMap.put(TradeConstants.SHIPMENT_EXPRESS_NODE_DETAILS,JSON_MAPPER.toJson(pushExpressInfos.getExpressDetails()));
                if (shipmentExpressRes.isSuccess() && shipmentExpressRes.getResult() != null){
                    ShipmentExpress shipmentExpress = shipmentExpressRes.getResult();
                    shipmentExpress.setExpressStatus(ShipmentExpressStatus.convert(pushExpressInfos.getExpressStatus()).value());
                    shipmentExpress.setExtra(extraMap);
                    result = shipmentWriteService.updateExpressInfo(shipmentExpress);
                } else{
                    ShipmentExpress shipmentExpress = new ShipmentExpress();
                    shipmentExpress.setShipmentCode(pushExpressInfos.getShipmentCode());
                    shipmentExpress.setExpressNo(pushExpressInfos.getExpressNo());
                    shipmentExpress.setExpressStatus(ShipmentExpressStatus.convert(pushExpressInfos.getExpressStatus()).value());
                    shipmentExpress.setExpressCompanyCode(pushExpressInfos.getExpressCompanyCode());
                    shipmentExpress.setExpressCompanyName(pushExpressInfos.getExpressCompanyName());
                    shipmentExpress.setExtra(extraMap);
                    result = shipmentWriteService.createExpressInfo(shipmentExpress);
                }
                if (result.isSuccess()){
                    return Response.ok(Boolean.TRUE);
                } else {
                    return Response.fail(UPDATE_EXPRESS_INFOS_STATUS+ pushExpressInfos.getShipmentCode());
                }
            } catch (Exception e){
                log.error("DealExpressInfoTask error,cause:{}",Throwables.getStackTraceAsString(e));
                return Response.fail(UPDATE_EXPRESS_INFOS_STATUS+ pushExpressInfos.getShipmentCode());
            }

        }

        private void convertDateFormate(List<ExpressDetails> expressDetails) {
            for (ExpressDetails expressDetail:expressDetails
                 ) {
                try {
                    Date tempDate = simpleDateFormat1.parse(expressDetail.getNodeAt());
                    expressDetail.setNodeAt(simpleDateFormat.format(tempDate));
                } catch (ParseException e) {
                    log.error("convertDateFormate error,cause:{}",Throwables.getStackTraceAsString(e));
                }
            }
        }
    }

}
