package com.pousheng.middle.web.job.receiver.info;

import com.google.common.collect.Maps;
import com.pousheng.middle.order.impl.manager.MiddleOrderReceiverInfoManager;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.common.utils.AESEncryptUtil;
import io.terminus.parana.order.model.OrderReceiverInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class EncryptOrderReceiverInfoJob extends BaseEncryptJob<OrderReceiverInfo> {

    @Autowired
    private MiddleOrderReceiverInfoManager middleOrderReceiverInfoManager;

    /**
     *
     */
    @RequestMapping("/fix/order/receiver/info")
    public void fix(){
        long start=System.currentTimeMillis();
        run();
        log.info("success to fix order receiver info with encrypt data.cost time:{} ms",System.currentTimeMillis()-start);
    }

    @Override
    protected String getThreadPoolNameFormat() {
        return "fix-order-receiver-info-pool-%d";
    }

    @Override
    protected Response<Paging<OrderReceiverInfo>> queryPaging(Integer pageNo,Integer pageSize) {
        PageInfo info = new PageInfo(pageNo, pageSize);
        try {
            Paging<OrderReceiverInfo> paging = middleOrderReceiverInfoManager.paging(info.getOffset(), info.getLimit(), "id asc", Maps.newHashMap());
            return Response.ok(paging);
        }catch (Exception e){
            log.error("failed paging order receiver info.",e);
        }
        return Response.fail("failed paging order receiver info");
    }

    @Override
    protected void updateReceiverInfo(OrderReceiverInfo data) {
        if(data==null){
            return;
        }
        if(data.getId()==null
            || StringUtils.isBlank(data.getReceiverInfoJson())){
            return;
        }

        try {
            String receiverInfo;
            if(StringUtils.startsWith(data.getReceiverInfoJson(),"{")) {
                receiverInfo = AESEncryptUtil.aesEncrypt(data.getReceiverInfoJson(), AESEncryptUtil.RECEIVER_INFO_ENCRYPT_KEY);
            }else{
                receiverInfo=data.getReceiverInfoJson();
            }
            OrderReceiverInfo orderReceiverInfo=new OrderReceiverInfo();
            orderReceiverInfo.setId(data.getId());
            orderReceiverInfo.setReceiverInfoJson(receiverInfo);
            middleOrderReceiverInfoManager.update(orderReceiverInfo);
        } catch (Exception e) {
            log.error("failed to fix receiver info of order .param:{}",data,e);
        }
    }
}
