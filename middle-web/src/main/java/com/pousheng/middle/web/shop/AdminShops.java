package com.pousheng.middle.web.shop;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.auth.dto.UcUserInfo;
import com.pousheng.erp.component.MposWarehousePusher;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.shop.dto.ShopExpresssCompany;
import com.pousheng.middle.shop.dto.ShopExtraInfo;
import com.pousheng.middle.shop.dto.ShopPaging;
import com.pousheng.middle.shop.dto.ShopServerInfo;
import com.pousheng.middle.shop.service.PsShopReadService;
import com.pousheng.middle.web.shop.event.CreateShopEvent;
import com.pousheng.middle.web.shop.event.UpdateShopEvent;
import com.pousheng.middle.web.user.component.ParanaUserOperationLogic;
import com.pousheng.middle.web.user.component.UcUserOperationLogic;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.Splitters;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopWriteService;
import io.terminus.open.client.parana.item.SyncParanaShopService;
import io.terminus.parana.common.utils.Iters;
import io.terminus.parana.common.utils.RespHelper;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.AdminShopWriteService;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.shop.service.ShopWriteService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Author:cp
 * Created on 6/30/16.
 */
@Api(description = "门店API")
@RestController
@Slf4j
@RequestMapping("/api/shop")
public class AdminShops {

    @RpcConsumer
    private ShopReadService shopReadService;

    @RpcConsumer
    private PsShopReadService psShopReadService;

    @RpcConsumer
    private ShopWriteService shopWriteService;

    @RpcConsumer
    private AdminShopWriteService adminShopWriteService;
    @Autowired
    private SyncParanaShopService syncParanaShopService;

    @Autowired
    private UcUserOperationLogic ucUserOperationLogic;
    @Autowired
    private EventBus eventBus;
    @Autowired
    private ParanaUserOperationLogic paranaUserOperationLogic;
    @Autowired
    private MposWarehousePusher mposWarehousePusher;
    @Autowired
    private OpenShopCacher openShopCacher;
    @RpcConsumer
    private OpenShopWriteService openShopWriteService;



    @ApiOperation("根据门店id查询门店信息")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Shop findShopById(@PathVariable("id") Long shopId) {
        Response<Shop> rShop = shopReadService.findById(shopId);
        if (!rShop.isSuccess()) {
            throw new JsonResponseException(rShop.getError());
        }
        return rShop.getResult();
    }


    @ApiOperation("根据用户id查询门店信息")
    @RequestMapping(value = "/user/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Shop> findShopByUserId(@PathVariable("id") Long userId) {
        return shopReadService.findByUserId(userId);
    }

    /**
     *  分页查询门店信息
     * @param id id
     * @param name 名称
     * @param userId 用户id
     * @param type 类型
     * @param status 状态
     * @param outerId 门店外码
     * @param companyId 公司Id
     * @param pageNo 页码
     * @param pageSize 页大小
     * @return 门店信息
     */
    @ApiOperation("分页查询门店信息")
    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<ShopPaging> paging(@RequestParam(required = false) Long id,
                                     @RequestParam(required = false) String name,
                                     @RequestParam(required = false) Long userId,
                                     @RequestParam(required = false) Integer type,
                                     @RequestParam(required = false) Integer status,
                                     @RequestParam(required = false) String outerId,
                                     @RequestParam(required = false) Long companyId,
                                     @RequestParam(required = false) Integer pageNo,
                                     @RequestParam(required = false) Integer pageSize) {
        if (id != null) {
            Response<Shop> rShop = shopReadService.findById(id);
            if (!rShop.isSuccess()) {
                log.debug("shop find by id={} failed, error={}", id, rShop.getError());
                return Paging.empty();
            }
            return new Paging<>();
        }
        Response<Paging<Shop>> resp = psShopReadService.pagination(name, userId, type, status,outerId,companyId, pageNo, pageSize);
        if (!resp.isSuccess()) {
            throw new JsonResponseException(resp.getError());
        }

        return transShopPaging(resp.getResult());
    }

    private Paging<ShopPaging> transShopPaging(Paging<Shop> result) {
        Paging<ShopPaging> shopPaging = new Paging<>();
        shopPaging.setTotal(result.getTotal());
        List<Shop> shops = result.getData();
        List<ShopPaging> shopPagingList = Lists.newArrayListWithCapacity(shops.size());

        for (Shop shop : shops){
            ShopPaging shopPag = new ShopPaging();
            shopPag.setShop(shop);
            shopPag.setShopExtraInfo(ShopExtraInfo.fromJson(shop.getExtra()));

            shopPagingList.add(shopPag);
        }
        shopPaging.setData(shopPagingList);

        return shopPaging;
    }


    @ApiOperation("创建门店信息")
    @RequestMapping(value = "/create", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Long> createShop(@RequestBody ShopWithUser shop) {
        //处理密码
        String password = Strings.nullToEmpty(shop.getUserPassword());
        judgePassword(password);

        if(Strings.isNullOrEmpty(shop.getPhone())){
            log.error("shop phone is null");
            throw new JsonResponseException("shop.mobile.invalid");
        }

        //交易门店外码是否正确
        Shop recoverShop = checkOuterId(shop.getOuterId());

        //创建门店
        shop.setBusinessId(shop.getCompanyId());//这里把公司id塞入到businessId字段中
        ShopExtraInfo shopExtraInfo = new ShopExtraInfo();
        shopExtraInfo.setCompanyId(shop.getCompanyId());
        shopExtraInfo.setShopInnerCode(shop.getStoreId());
        shopExtraInfo.setCompanyName(shop.getCompanyName());
        Map<String,String> extraMap = Maps.newHashMap();
        shop.setExtra(ShopExtraInfo.putExtraInfo(extraMap,shopExtraInfo));


        Long id;
        if(Arguments.isNull(recoverShop)){
            //创建门店用户
            Response<UcUserInfo> userInfoRes = ucUserOperationLogic.createUcUserForShop(shop.getOuterId(),password);
            if(!userInfoRes.isSuccess()){
                log.error("create user(name:{}) fail,error:{}",shop.getOuterId(),userInfoRes.getError());
                throw new JsonResponseException(userInfoRes.getError());
            }
            //创建门店信息
            id = createShop(shop, userInfoRes.getResult().getUserId(), shop.getOuterId());

            CreateShopEvent addressEvent = new CreateShopEvent(id,shop.getCompanyId(),shop.getOuterId(),shop.getOuterId());
            eventBus.post(addressEvent);
        }else {
            //更新门店用户
            Response<UcUserInfo> userInfoRes = ucUserOperationLogic.updateUcUser(recoverShop.getUserId(),shop.getUserName(),password);
            if(!userInfoRes.isSuccess()){
                log.error("update user(name:{}) fail,error:{}",recoverShop.getOuterId(),userInfoRes.getError());
                throw new JsonResponseException(userInfoRes.getError());
            }
            //解冻
            RespHelper.or500(paranaUserOperationLogic.updateUserStatus(1,recoverShop.getUserId()));
            //更新门店信息
            id = updateShop(recoverShop.getId());

            UpdateShopEvent updateShopEvent = new UpdateShopEvent(id,shop.getCompanyId(),recoverShop.getOuterId());
            eventBus.post(updateShopEvent);

            //同步电商
            syncParanaShop(recoverShop);
        }





        return Collections.singletonMap("id", id);
    }

    private Shop checkOuterId(String outerId){
        if(Strings.isNullOrEmpty(outerId)){
            log.error("shop outer id is null");
            throw new JsonResponseException("shop.outer.in.invalid");
        }
        Response<Shop> shopRes = shopReadService.findByOuterId(outerId);
        if(shopRes.isSuccess()){
            Shop shop = shopRes.getResult();
            log.warn("shop(id:{}) ,status:{} exist outer id:{}",shop.getId(),shop.getStatus(),outerId);
            if(Objects.equal(shop.getStatus(),-1)){
                log.warn("shop(id:{}),status:{} exist outer id:{} ,so to recover status 1",shop.getId(),shop.getStatus(),outerId);
                return shop;
            }
            throw new JsonResponseException("duplicate.shop.outer.id");
        }

        return null;

    }

    //直接恢复状态
    private Long updateShop(Long shopId){
        Response<Boolean> updateRes = adminShopWriteService.unfrozen(shopId);
        if(!updateRes.isSuccess()){
            log.error("update shop(id:{}) status to:{} fail,error:{}",shopId,1,updateRes.getError());
            throw new JsonResponseException(updateRes.getError());
        }

        return shopId;
    }

    private Long createShop(Shop shop, Long userId, String userName) {
        Response<Boolean> checkResp = shopReadService.checkExistByName(shop.getName());
        if (!checkResp.isSuccess()) {
            log.error("fail to check shop if existed by name:{},cause:{}",
                    shop.getName(), checkResp.getError());
            throw new JsonResponseException(checkResp.getError());
        }
        if (checkResp.getResult()) {
            log.error("shop name({}) duplicated");
            throw new JsonResponseException("shop.name.duplicated");
        }

        Shop toCreate = new Shop();
        toCreate.setName(shop.getName());
        toCreate.setBusinessId(shop.getBusinessId());
        toCreate.setImageUrl(shop.getImageUrl());
        toCreate.setType(MoreObjects.firstNonNull(shop.getType(),1));
        toCreate.setStatus(1);

        toCreate.setUserId(userId);
        toCreate.setOuterId(shop.getOuterId());
        toCreate.setUserName(userName);
        toCreate.setPhone(shop.getPhone());
        toCreate.setAddress(shop.getAddress());
        toCreate.setExtra(shop.getExtra());

        Response<Long> rShop = shopWriteService.create(toCreate);
        if (!rShop.isSuccess()) {
            log.warn("shop create failed, error={}", rShop.getError());
            throw new JsonResponseException(rShop.getError());
        }
        //同步电商
        syncParanaShop(toCreate);

        return rShop.getResult();
    }


    private void judgePassword(String password) {
        if (!password.matches("[\\s\\S]{6,16}")) {
            throw new JsonResponseException(500, "user.password.invalid");
        }
    }



    @ApiOperation("更新门店信息")
    @RequestMapping(value = "/{shopId}", method = RequestMethod.PUT)
    public void updateShop(@PathVariable Long shopId, @RequestBody Shop shop) {
        //判断店铺名称是否重复
        checkShopNameIfDuplicated(shopId,shop.getName());

        val rExist = shopReadService.findById(shopId);
        if (!rExist.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}",shopId,rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        Shop exist = rExist.getResult();

        Shop toUpdate = new Shop();
        toUpdate.setId(shopId);
        toUpdate.setName(shop.getName());
        toUpdate.setImageUrl(shop.getImageUrl());
        toUpdate.setPhone(shop.getPhone());
        toUpdate.setAddress(shop.getAddress());
        toUpdate.setExtra(Iters.deltaUpdate(exist.getExtra(), shop.getExtra()));
        Response<Boolean> resp = shopWriteService.update(toUpdate);
        if (!resp.isSuccess()) {
            log.error("update shop failed, shopId={}, error={}", shopId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
    }

    @ApiOperation("更新门店用户登录密码")
    @RequestMapping(value = "/{shopId}/reset/password", method = RequestMethod.PUT)
    public void updateShop(@PathVariable Long shopId, @RequestParam String password) {
        val rExist = shopReadService.findById(shopId);
        if (!rExist.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}",shopId,rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        Shop exist = rExist.getResult();
        judgePassword(password);

        //更新用户中心用户信息
        Response<UcUserInfo> ucUserInfoRes = ucUserOperationLogic.updateUcUser(exist.getUserId(), exist.getUserName(), password);
        if (!ucUserInfoRes.isSuccess()) {
            log.error("update user center user(id:{}) fail,error:{}", exist.getUserId(), ucUserInfoRes.getError());
            throw new JsonResponseException(ucUserInfoRes.getError());
        }
    }


    @ApiOperation("更新单个门店安全库存信息")
    @RequestMapping(value = "/{shopId}/safe/stock", method = RequestMethod.PUT)
    public void updateShopSafeStock(@PathVariable Long shopId, @RequestParam Integer safeStock) {
        val rExist = shopReadService.findById(shopId);
        if (!rExist.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}",shopId,rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        Shop exist = rExist.getResult();

        ShopExtraInfo existShopExtraInfo = ShopExtraInfo.fromJson(exist.getExtra());
        if (existShopExtraInfo != null) {
            existShopExtraInfo.setSafeStock(safeStock);
        }

        Shop toUpdate = new Shop();
        toUpdate.setId(shopId);
        toUpdate.setExtra(ShopExtraInfo.putExtraInfo(exist.getExtra(),existShopExtraInfo));
        Response<Boolean> resp = shopWriteService.update(toUpdate);
        if (!resp.isSuccess()) {
            log.error("update shop extra:{}failed, shopId={}, error={}",existShopExtraInfo, shopId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
    }


    /**
     * 批量更新门店安全库存信息
     * @param shopIds 多个id用逗号隔开
     * @param safeStock 安全库存
     */
    @ApiOperation("批量更新门店安全库存信息")
    @RequestMapping(value = "/batch-set/safe/stock", method = RequestMethod.PUT)
    public void batchUpdateShopSafeStock(@RequestParam String shopIds, @RequestParam Integer safeStock) {
        List<Long> ids  = Splitters.splitToLong(shopIds,Splitters.COMMA);
        for (Long shopId : ids){
            this.updateShopSafeStock(shopId,safeStock);
        }

    }


    /**
     * 更新门店快递信息
     * @param shopId 店铺id
     * @param expresssCompanyJson 快递公司code name逗号隔开 例如：[{"code":"zhontong","name":"中通"},{"code":"shunfeng","name":"顺丰"}]
     */
    @ApiOperation("更新单个门店快递信息")
    @RequestMapping(value = "/{shopId}/express/company", method = RequestMethod.PUT)
    public void updateShopExpressCompany(@PathVariable Long shopId, @RequestParam String expresssCompanyJson) {

        val rExist = shopReadService.findById(shopId);
        if (!rExist.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}",shopId,rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        Shop exist = rExist.getResult();

        ShopExtraInfo existShopExtraInfo = ShopExtraInfo.fromJson(exist.getExtra());
        if (existShopExtraInfo != null) {
            existShopExtraInfo.setExpresssCompanyJson(expresssCompanyJson);
            Shop toUpdate = new Shop();
            toUpdate.setId(shopId);
            toUpdate.setExtra(ShopExtraInfo.putExtraInfo(exist.getExtra(),existShopExtraInfo));
            Response<Boolean> resp = shopWriteService.update(toUpdate);
            if (!resp.isSuccess()) {
                log.error("update shop extra:{} failed, shopId={}, error={}",existShopExtraInfo, shopId, resp.getError());
                throw new JsonResponseException(500, resp.getError());
            }


            //同步电商更新门店物流公司信息
            if(!CollectionUtils.isEmpty(existShopExtraInfo.getExpresssCompanyList())){
                ShopExpresssCompany shopExpresssCompany = existShopExtraInfo.getExpresssCompanyList().get(0);
                Response<Boolean> syncParanaExpressRes = syncParanaShopService.syncShopExpress(exist.getOuterId(),shopExpresssCompany.getCode(),shopExpresssCompany.getName());
                if(!syncParanaExpressRes.isSuccess()){
                    log.error("sync parana shop(id:{}) express code:{} name:{} fail,error:{}",shopId,shopExpresssCompany.getName(),shopExpresssCompany.getName());
                }
            }

        }
    }


    /**
     * 批量更新门店快递信息
     * @param shopIds 多个店铺id用逗号隔开
     * @param expresssCompanyJson 快递公司code name逗号隔开 例如：[{"code":"zhontong","name":"中通"},{"code":"shunfeng","name":"顺丰"}]
     */
    @ApiOperation("批量更新门店快递信息")
    @RequestMapping(value = "/batch-set/express/company", method = RequestMethod.PUT)
    public void batchUpdateShopExpressCompany(@RequestParam String shopIds, @RequestParam String expresssCompanyJson) {
        List<Long> ids  = Splitters.splitToLong(shopIds,Splitters.COMMA);
        for (Long shopId : ids){
            this.updateShopExpressCompany(shopId,expresssCompanyJson);
        }
    }


    /**
     * 更新门店服务信息
     * @param shopId 店铺id
     * @param shopServerInfo 服务信息
     */
    @ApiOperation("更新单个门店快递信息")
    @RequestMapping(value = "/{shopId}/server/info", method = RequestMethod.PUT)
    public void updateShopServerInfo(@PathVariable Long shopId, @RequestBody ShopServerInfo shopServerInfo) {

        val rExist = shopReadService.findById(shopId);
        if (!rExist.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}",shopId,rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        Shop exist = rExist.getResult();

        ShopExtraInfo existShopExtraInfo = ShopExtraInfo.fromJson(exist.getExtra());
        existShopExtraInfo.setShopServerInfo(shopServerInfo);
        if(Arguments.isNull(existShopExtraInfo.getOpenShopId())){
            log.error("shop(id:{}) not related open shop",shopId);
            throw new JsonResponseException("shop.not.related.open.shop");
        }

        Shop toUpdate = new Shop();
        toUpdate.setId(shopId);
        toUpdate.setExtra(ShopExtraInfo.putExtraInfo(exist.getExtra(),existShopExtraInfo));
        Response<Boolean> resp = shopWriteService.update(toUpdate);
        if (!resp.isSuccess()) {
            log.error("update shop extra:{} failed, shopId={}, error={}",existShopExtraInfo, shopId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }

        OpenShop openShop = openShopCacher.findById(existShopExtraInfo.getOpenShopId());
        Map<String,String> openExtra = openShop.getExtra();
        if(CollectionUtils.isEmpty(openExtra)){
            openExtra = Maps.newHashMap();
        }
        openExtra.put(TradeConstants.HK_PERFORMANCE_SHOP_CODE,shopServerInfo.getVirtualShopInnerCode());
        openExtra.put(TradeConstants.HK_PERFORMANCE_SHOP_NAME,shopServerInfo.getVirtualShopName());
        openExtra.put(TradeConstants.HK_PERFORMANCE_SHOP_OUT_CODE,shopServerInfo.getVirtualShopCode());
        openExtra.put(TradeConstants.HK_COMPANY_CODE,shopServerInfo.getCompanyId());
        if(Arguments.notNull(shopServerInfo.getReturnWarehouseId())){
            openExtra.put(TradeConstants.DEFAULT_REFUND_WAREHOUSE_ID,shopServerInfo.getReturnWarehouseId().toString());
            openExtra.put(TradeConstants.DEFAULT_REFUND_OUT_WAREHOUSE_CODE,shopServerInfo.getReturnWarehouseCode());
            openExtra.put(TradeConstants.DEFAULT_REFUND_WAREHOUSE_NAME,shopServerInfo.getReturnWarehouseName());
        }

        OpenShop toUpdateOpenShop = new OpenShop();
        toUpdateOpenShop.setId(existShopExtraInfo.getOpenShopId());
        toUpdateOpenShop.setExtra(openExtra);

        Response<Boolean>  updateRes = openShopWriteService.update(toUpdateOpenShop);
        if(!updateRes.isSuccess()){
            log.error("update open shop(id:{}) extra fail,error:{}",existShopExtraInfo.getOpenShopId(),updateRes.getError());
            throw new JsonResponseException(updateRes.getError());
        }
    }


    /**
     * 批量更新门店快递信息
     * @param shopIds 多个店铺id用逗号隔开
     * @param shopServerInfo 服务信息
     */
    @ApiOperation("批量更新门店快递信息")
    @RequestMapping(value = "/batch-set/server/info", method = RequestMethod.PUT)
    public void batchUpdateShopServerInfo(@RequestParam String shopIds, @RequestBody ShopServerInfo shopServerInfo) {
        List<Long> ids  = Splitters.splitToLong(shopIds,Splitters.COMMA);
        for (Long shopId : ids){
            this.updateShopServerInfo(shopId,shopServerInfo);
        }
    }



    @ApiOperation("冻结门店")
    @RequestMapping(value = "/{shopId}/frozen", method = RequestMethod.PUT)
    public void frozenSeller(@PathVariable Long shopId) {
        val rExist = shopReadService.findById(shopId);
        if (!rExist.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}",shopId,rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        Shop exist = rExist.getResult();
        RespHelper.or500(adminShopWriteService.frozen(shopId));
        RespHelper.or500(paranaUserOperationLogic.updateUserStatus(-2,exist.getUserId()));
        //同步电商
        syncParanaFrozenShop(exist.getOuterId());
    }

    @ApiOperation("解冻门店")
    @RequestMapping(value = "/{shopId}/unfrozen", method = RequestMethod.PUT)
    public void unfrozenSeller(@PathVariable Long shopId) {
        val rExist = shopReadService.findById(shopId);
        if (!rExist.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}",shopId,rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        Shop exist = rExist.getResult();
        RespHelper.or500(adminShopWriteService.unfrozen(shopId));
        RespHelper.or500(paranaUserOperationLogic.updateUserStatus(1,exist.getUserId()));
        //同步电商
        syncParanaUnFrozenShop(exist.getOuterId());

    }

    @ApiOperation("删除门店")
    @RequestMapping(value = "/{shopId}/close", method = RequestMethod.PUT)
    public void closeSeller(@PathVariable Long shopId) {
        val rExist = shopReadService.findById(shopId);
        if (!rExist.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}",shopId,rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        Shop exist = rExist.getResult();
        RespHelper.or500(adminShopWriteService.close(shopId));
        RespHelper.or500(paranaUserOperationLogic.updateUserStatus(-2,exist.getUserId()));
        //同步恒康mpos门店范围
        mposWarehousePusher.removeWarehouses(exist.getBusinessId().toString(),exist.getOuterId());
        //同步电商
        syncParanaCloseShop(exist.getOuterId());

    }




    private void checkShopNameIfDuplicated(Long currentShopId,String updatedShopName) {
        if (!StringUtils.hasText(updatedShopName)) {
            return;
        }

        Response<Boolean> checkResp = shopReadService.checkExistByName(updatedShopName);
        if (!checkResp.isSuccess()) {
            log.error("fail to check shop if existed by name:{},cause:{}",
                    updatedShopName, checkResp.getError());
            throw new JsonResponseException(checkResp.getError());
        }

        if (!checkResp.getResult()) {
            return;
        }

        Response<Shop> findShop = shopReadService.findByName(updatedShopName);
        if (!findShop.isSuccess()) {
            log.error("fail to find shop by name:{},cause:{}",
                    updatedShopName, findShop.getError());
            throw new JsonResponseException(findShop.getError());
        }
        Shop shop = findShop.getResult();

        if (!Objects.equal(shop.getId(), currentShopId)) {
            log.error("shop name({}) duplicated");
            throw new JsonResponseException("shop.name.duplicated");
        }
    }


    private void syncParanaShop(Shop shop){
        Response<Boolean> response =  syncParanaShopService.syncShopInfo(shop);
        if(!response.isSuccess()){
            log.error("sync parana shop fail,error:{}",shop);
            throw new JsonResponseException(response.getError());
        }
    }


    private void syncParanaFrozenShop(String outerId){
        Response<Boolean> response =  syncParanaShopService.frozenShop(outerId);
        if(!response.isSuccess()){
            log.error("sync parana frozen shop(outerId:{}) fail,error:{}",outerId,response.getError());
            throw new JsonResponseException(response.getError());
        }
    }

    private void syncParanaUnFrozenShop(String outerId){
        Response<Boolean> response =  syncParanaShopService.unfrozenShop(outerId);
        if(!response.isSuccess()){
            log.error("sync parana unfrozen shop(outerId:{}) fail,error:{}",outerId,response.getError());
            throw new JsonResponseException(response.getError());
        }
    }

    private void syncParanaCloseShop(String outerId){
        Response<Boolean> response =  syncParanaShopService.closeShop(outerId);
        if(!response.isSuccess()){
            log.error("sync parana close shop(outerId:{}) fail,error:{}",outerId,response.getError());
            throw new JsonResponseException(response.getError());
        }
    }


    @Data
    public static class ShopWithUser extends Shop {

        private static final long serialVersionUID = 7122636456538456745L;

        private String userPassword;
        /**
         * 公司id
         */
        private Long companyId;
        //公司名称
        private String companyName;

        //店铺内码
        private String storeId;
    }
}
