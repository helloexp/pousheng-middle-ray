package com.pousheng.middle.web.job.receiver.info;

import com.google.common.collect.Maps;
import com.pousheng.middle.order.service.MiddleShipmentReadService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.common.utils.AESEncryptUtil;
import io.terminus.parana.order.model.Shipment;
import io.terminus.parana.order.service.ShipmentWriteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class EncryptShipmentReceiverInfoJob extends BaseEncryptJob<Shipment> {

    @RpcConsumer
    private MiddleShipmentReadService middleShipmentReadService;

    @RpcConsumer
    private ShipmentWriteService shipmentWriteService;

    /**
     *
     */
    @RequestMapping("/fix/shipment/receiver/info")
    public void fix(){
        run();
    }

    @Override
    protected String getThreadPoolNameFormat() {
        return "fix-shipment-receiver-info-pool-%d";
    }

    @Override
    protected Response<Paging<Shipment>> queryPaging(Integer pageNo,Integer pageSize) {
        PageInfo info = new PageInfo(pageNo, pageSize);
        return middleShipmentReadService.paging(info.getOffset(),info.getLimit(),"id asc", Maps.newHashMap());
    }

    @Override
    protected void updateReceiverInfo(Shipment data) {
        if(data==null){
            return;
        }
        if(data.getId()==null
            || StringUtils.isBlank(data.getReceiverInfos())
            || !StringUtils.startsWith(data.getReceiverInfos(),"{")){
            return;
        }

        try {
            String receiverInfo= AESEncryptUtil.aesEncrypt(data.getReceiverInfos(),AESEncryptUtil.RECEIVER_INFO_ENCRYPT_KEY);
            Shipment shipment=new Shipment();
            shipment.setId(data.getId());
            shipment.setReceiverInfos(receiverInfo);
            shipmentWriteService.update(shipment);
        } catch (Exception e) {
            log.error("failed to fix receiver info of shipment.param:{}",data);
        }
    }
}
