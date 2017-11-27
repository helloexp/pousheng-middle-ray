package com.pousheng.middle.web.order.job;

import com.pousheng.middle.order.dto.OrderShipmentCriteria;
import com.pousheng.middle.order.dto.RefundExtra;
import com.pousheng.middle.order.dto.ShipmentExtra;
import com.pousheng.middle.order.dto.ShipmentPagingInfo;
import com.pousheng.middle.order.enums.MiddleRefundStatus;
import com.pousheng.middle.order.enums.MiddleShipmentsStatus;
import com.pousheng.middle.order.model.PoushengSettlementPos;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.order.service.PoushengSettlementPosReadService;
import com.pousheng.middle.order.service.PoushengSettlementPosWriteService;
import com.pousheng.middle.web.order.component.RefundReadLogic;
import com.pousheng.middle.web.order.component.ShipmentReadLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.order.dto.RefundCriteria;
import io.terminus.parana.order.model.OrderShipment;
import io.terminus.parana.order.model.Refund;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.service.RefundReadService;
import io.terminus.parana.order.service.ShipmentReadService;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 通过跑定时任务生成pos单信息，最终需要删除
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/27
 * pousheng-middle
 */
@Component
@Slf4j
public class PosJob {
    @Autowired
    private HostLeader hostLeader;
    @RpcConsumer
    private OrderShipmentReadService orderShipmentReadService;
    @RpcConsumer
    private PoushengSettlementPosReadService poushengSettlementPosReadService;
    @RpcConsumer
    private PoushengSettlementPosWriteService poushengSettlementPosWriteService;
    @Autowired
    private ShipmentReadLogic shipmentReadLogic;
    @Autowired
    private RefundReadLogic refundReadLogic;
    @RpcConsumer
    private RefundReadService refundReadService;



    /**
     * 每隔5分钟执行一次
     */
    @Scheduled(cron = "0 0/5 * * * ? ")
    public void doInsertPosInfo(){

        if(!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }
        int pageNo=1;
        //获取所有的发货单信息,目前生产环境上的有效的数据不超过1000
        while(true){
            OrderShipmentCriteria criteria = new OrderShipmentCriteria();
            criteria.setPageNo(pageNo++);
            criteria.setPageSize(20);
            Response<Paging<ShipmentPagingInfo>> r =  orderShipmentReadService.findBy(criteria);
            if (!r.isSuccess()){
                log.error("find shipment info failed,criteria is {},caused by {}",criteria,r.getError());
                return;
            }
            if (r.getResult().getData().size()==0){
                break;
            }
            List<ShipmentPagingInfo> shipmentPagingInfos =  r.getResult().getData();
            List<Shipment> shipments = Lists.newArrayList();
            for (ShipmentPagingInfo shipmentPagingInfo:shipmentPagingInfos){
                shipments.add(shipmentPagingInfo.getShipment());
            }
            //获取正向的发货单
            List<Shipment> shipmentList = shipments.stream().filter(Objects::nonNull).filter(it -> (it.getStatus()>= MiddleShipmentsStatus.ACCEPTED.getValue())).collect(Collectors.toList());
            for (Shipment shipment:shipmentList){
                ShipmentExtra shipmentExtra = shipmentReadLogic.getShipmentExtra(shipment);
                OrderShipment orderShipment =  shipmentReadLogic.findOrderShipmentByShipmentId(shipment.getId());
                PoushengSettlementPos pos = new PoushengSettlementPos();
                if (StringUtils.isEmpty(shipmentExtra.getPosSerialNo())){
                    continue;
                }
                if (Objects.equals(orderShipment.getType(),1)){
                    pos.setOrderId(orderShipment.getOrderId());
                    pos.setShipType(1);
                }else{
                    pos.setOrderId(orderShipment.getAfterSaleOrderId());
                    pos.setShipType(2);
                }
                String posAmt = String.valueOf(new BigDecimal(shipmentExtra.getPosAmt()).setScale(0, RoundingMode.HALF_DOWN));
                pos.setPosAmt(Long.valueOf(posAmt));
                pos.setPosType(Integer.valueOf(shipmentExtra.getPosType()));
                pos.setPosSerialNo(shipmentExtra.getPosSerialNo());
                pos.setShopId(orderShipment.getShopId());
                pos.setShopName(orderShipment.getShopName());
                pos.setPosCreatedAt(shipmentExtra.getPosCreatedAt());
                Response<PoushengSettlementPos> rP = poushengSettlementPosReadService.findByPosSerialNo(shipmentExtra.getPosSerialNo());
                if (!r.isSuccess()){
                    log.error("find pousheng settlement pos failed, posSerialNo is {},caused by {}",shipmentExtra.getPosSerialNo(),rP.getError());
                    continue;
                }
                if(!Objects.isNull(rP.getResult())){
                    continue;
                }
                Response<Long> rL = poushengSettlementPosWriteService.create(pos);
                if (!rL.isSuccess()){
                    log.error("create pousheng settlement pos failed,pos is {},caused by {}",pos,rL.getError());
                    continue;
                }
            }
        }

    }


    /**
     * 每隔9分钟执行一次
     */
    @Scheduled(cron = "0 0/9 * * * ? ")
    public void doInsertAfterSalePosInfo(){

        if(!hostLeader.isLeader()) {
            log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
            return;
        }
        //获取所有的发货单信息,目前生产环境上的有效的数据不超过1000
        int pageNo=1;
        while(true){
            RefundCriteria criteria = new RefundCriteria();
            Response<Paging<Refund>> r = refundReadService.findRefundBy(pageNo++,1000,criteria);
            if (!r.isSuccess()){
                log.error("find.refund failed,criteria is {},caused by {}",criteria,r.getError());
                return;
            }
            List<Refund> refunds = r.getResult().getData();
            List<Refund> refundList = refunds.stream().filter(Objects::nonNull).filter(it->(it.getStatus()>= MiddleRefundStatus.REFUND_SYNC_HK_SUCCESS.getValue())).collect(Collectors.toList());
            for (Refund refund:refundList){
                RefundExtra refundExtra = refundReadLogic.findRefundExtra(refund);
                if (StringUtils.isEmpty(refundExtra.getPosSerialNo())){
                    continue;
                }
                PoushengSettlementPos pos = new PoushengSettlementPos();
                pos.setOrderId(refund.getId());
                String posAmt = String.valueOf(new BigDecimal(refundExtra.getPosAmt()).setScale(0, RoundingMode.HALF_DOWN));
                pos.setPosAmt(Long.valueOf(posAmt));
                pos.setPosType(Integer.valueOf(refundExtra.getPosType()));
                pos.setShipType(3);
                pos.setPosSerialNo(refundExtra.getPosSerialNo());
                pos.setShopId(refund.getShopId());
                pos.setShopName(refund.getShopName());
                pos.setPosCreatedAt(refundExtra.getPosCreatedAt());
                Response<PoushengSettlementPos> rP = poushengSettlementPosReadService.findByPosSerialNo(refundExtra.getPosSerialNo());
                if (!r.isSuccess()){
                    log.error("find pousheng settlement pos failed, posSerialNo is {},caused by {}",refundExtra.getPosSerialNo(),rP.getError());
                    continue;
                }
                if(!Objects.isNull(rP.getResult())){
                    continue;
                }
                Response<Long> rL = poushengSettlementPosWriteService.create(pos);
                if (!rL.isSuccess()){
                    log.error("create pousheng settlement pos failed,pos is {},caused by {}",pos,rL.getError());
                    continue;
                }
            }
        }
    }
}
