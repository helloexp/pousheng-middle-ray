package com.pousheng.middle.web.job.receiver.info;

import com.google.common.collect.Maps;
import com.pousheng.middle.order.impl.manager.MiddleReceiverInfoManager;
import io.terminus.common.model.PageInfo;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.parana.common.utils.AESEncryptUtil;
import io.terminus.parana.order.model.ReceiverInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class EncryptReceiverInfoJob extends BaseEncryptJob<ReceiverInfo> {

    public static final String ENCRYPT_SYMBOL="*";

    @Autowired
    private MiddleReceiverInfoManager middleReceiverInfoManager;

    /**
     *
     */
    @RequestMapping("/fix/receiver/info")
    public void fix(){
        long start=System.currentTimeMillis();
        run();
        log.info("success to fix receiver info with encrypt data.cost time:{} ms",System.currentTimeMillis()-start);
    }

    @Override
    protected String getThreadPoolNameFormat() {
        return "fix-receiver-info-pool-%d";
    }

    @Override
    protected Response<Paging<ReceiverInfo>> queryPaging(Integer pageNo,Integer pageSize) {
        PageInfo info = new PageInfo(pageNo, pageSize);
        try {
            Paging<ReceiverInfo> paging = middleReceiverInfoManager.paging(info.getOffset(), info.getLimit(), "id asc", Maps.newHashMap());
            return Response.ok(paging);
        }catch (Exception e){
            log.error("failed paging receiver info.",e);
        }
        return Response.fail("failed paging receiver info");
    }

    @Override
    protected void updateReceiverInfo(ReceiverInfo data) {
        if(data==null){
            return;
        }
        if(data.getId()==null){
            return;
        }

        try {
            String encryptedMobile=null;
            String encryptedName=null;

            if(!StringUtils.startsWith(data.getReceiveUserName(),ENCRYPT_SYMBOL)){
                encryptedName=ENCRYPT_SYMBOL+AESEncryptUtil.aesEncrypt(data.getMobile(), AESEncryptUtil.RECEIVER_INFO_ENCRYPT_KEY);
            }

            if(!StringUtils.startsWith(data.getMobile(),ENCRYPT_SYMBOL)) {
                encryptedMobile = ENCRYPT_SYMBOL+AESEncryptUtil.aesEncrypt(data.getMobile(), AESEncryptUtil.RECEIVER_INFO_ENCRYPT_KEY);
            }

            ReceiverInfo info=new ReceiverInfo();
            info.setId(data.getId());
            info.setMobile(encryptedMobile);
            info.setReceiveUserName(encryptedName);
            middleReceiverInfoManager.update(info);
        } catch (Exception e) {
            log.error("failed to fix receiver info of order .param:{}",data,e);
        }
    }
}
