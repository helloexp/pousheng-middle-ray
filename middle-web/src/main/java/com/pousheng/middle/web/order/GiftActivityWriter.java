package com.pousheng.middle.web.order;

import com.pousheng.middle.order.dto.EditSubmitGiftActivityInfo;
import com.pousheng.middle.order.dto.SubmitRefundInfo;
import com.pousheng.middle.order.service.PoushengGiftActivityWriteService;
import com.pousheng.middle.web.order.component.PoushengGiftActivityWriteLogic;
import com.pousheng.middle.web.utils.operationlog.OperationLogModule;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import com.pousheng.middle.web.utils.permission.PermissionCheck;
import com.pousheng.middle.web.utils.permission.PermissionCheckParam;
import io.terminus.common.model.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2017/11/29
 * pousheng-middle
 */
@RestController
@Slf4j
@OperationLogModule(OperationLogModule.Module.POUSHENG_GIFT_ACTIVITY)
public class GiftActivityWriter {

    @Autowired
    private PoushengGiftActivityWriteLogic poushengGiftActivityWriteLogic;
    /**
     * 创建活动规则
     *
     * @param editSubmitGiftActivityInfo 提交信息
     * @return
     */
    @RequestMapping(value = "/api/gift/actvity/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @PermissionCheck(PermissionCheck.PermissionCheckType.SHOP_ORDER)
    @OperationLogType("创建活动规则")
    public Response<Long> createRe(@RequestBody @PermissionCheckParam("orderId") EditSubmitGiftActivityInfo editSubmitGiftActivityInfo) {
        return Response.ok(poushengGiftActivityWriteLogic.createGiftActivity(editSubmitGiftActivityInfo));
    }

}
