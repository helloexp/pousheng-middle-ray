package com.pousheng.middle.web.user.component;

import com.google.common.collect.Lists;
import com.pousheng.middle.constants.Constants;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.parana.auth.model.Operator;
import io.terminus.parana.auth.service.OperatorReadService;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by songrenfei on 2017/7/17
 */
@Slf4j
@Component
public class UserManageShopReader {

    @RpcConsumer
    private OperatorReadService operatorReadService;
    @RpcConsumer
    private OpenShopReadService openShopReadService;



    public List<OpenClientShop> findManageShops(ParanaUser paranaUser) {

        List<String> roles = paranaUser.getRoles();
        //超级管理员可管理全部店铺（这里为了获取用户管理店铺（调用方）统一处理，对admin也查询出店铺）
        if(roles.contains(UserRole.ADMIN.name())){
            Response<List<OpenShop>> openShopRes = openShopReadService.findAll();
            if(!openShopRes.isSuccess()){
                log.error("find all open shop fail,error:{}",openShopRes.getError());
                throw new JsonResponseException(openShopRes.getError());
            }

            return makeOpenClientShops(openShopRes.getResult());
        }


        Response<Operator> operatorResp = operatorReadService.findByUserId(paranaUser.getId());
        if (!operatorResp.isSuccess()) {
            log.warn("operator find fail, userId={}, error={}", paranaUser.getId(), operatorResp.getError());
            throw new JsonResponseException(operatorResp.getError());
        }

        Operator existOp = operatorResp.getResult();
        Map<String,String> extraMap = existOp.getExtra();
        List<Long> shopIds = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(extraMap.get(Constants.MANAGE_SHOP_IDS),JsonMapper.JSON_NON_EMPTY_MAPPER.createCollectionType(List.class,Long.class));

        if(CollectionUtils.isEmpty(shopIds)){
            return Lists.newArrayList();
        }

        Response<List<OpenShop>> openShopRes = openShopReadService.findByIds(shopIds);
        if(!openShopRes.isSuccess()){
            log.error("find open shop by ids:{} fail,error:{}",shopIds,openShopRes.getError());
            throw new JsonResponseException(openShopRes.getError());
        }

        return makeOpenClientShops(openShopRes.getResult());
    }


    private List<OpenClientShop> makeOpenClientShops(List<OpenShop> openShops){
        List<OpenClientShop> openClientShops = Lists.newArrayListWithCapacity(openShops.size());

        openShops.forEach(openShop -> {
            OpenClientShop openClientShop = new OpenClientShop();
            openClientShop.setShopName(openShop.getShopName());
            openClientShop.setChannel(openShop.getChannel());
            openClientShop.setOpenShopId(openShop.getId());
            openClientShops.add(openClientShop);

        });

        return openClientShops;
    }

    /**
     * 检查当前登录用户是否有操作对应店铺的权限
     *
     * @param shopId 店铺id
     */
    public void authCheck(Long shopId) {
        ParanaUser user = UserUtil.getCurrentUser();
        List<OpenClientShop> shops =  this.findManageShops(user);
        List<Long> shopIds = Lists.newArrayListWithCapacity(shops.size());
        for (OpenClientShop shop : shops) {
            shopIds.add(shop.getOpenShopId());
        }
        if(!shopIds.contains(shopId)){
            log.error("user({}) can not create shopStockRule for  shop(id={})", user, shopId);
            throw new JsonResponseException("shop.not.allowed");
        }
    }


}
