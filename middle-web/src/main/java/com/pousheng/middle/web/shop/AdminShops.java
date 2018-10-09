package com.pousheng.middle.web.shop;

import com.google.common.base.*;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.eventbus.EventBus;
import com.pousheng.auth.dto.LoginTokenInfo;
import com.pousheng.auth.dto.UcUserInfo;
import com.pousheng.erp.component.MposWarehousePusher;
import com.pousheng.middle.constants.Constants;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.order.model.AddressGps;
import com.pousheng.middle.order.model.ZoneContract;
import com.pousheng.middle.order.service.OrderShipmentReadService;
import com.pousheng.middle.order.service.ZoneContractReadService;
import com.pousheng.middle.shop.cacher.MiddleShopCacher;
import com.pousheng.middle.shop.dto.*;
import com.pousheng.middle.shop.enums.ShopOpeningStatus;
import com.pousheng.middle.shop.service.PsShopReadService;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.companent.WarehouseClient;
import com.pousheng.middle.warehouse.dto.WarehouseDTO;
import com.pousheng.middle.web.order.component.ShopMaxOrderLogic;
import com.pousheng.middle.web.order.sync.mpos.MiddleParanaClient;
import com.pousheng.middle.web.shop.cache.ShopChannelGroupCacher;
import com.pousheng.middle.web.shop.component.MemberShopOperationLogic;
import com.pousheng.middle.web.shop.component.ShopBusinessLogic;
import com.pousheng.middle.web.shop.dto.Channel;
import com.pousheng.middle.web.shop.dto.OrderExpireInfo;
import com.pousheng.middle.web.shop.event.CreateShopEvent;
import com.pousheng.middle.web.shop.event.UpdateShopEvent;
import com.pousheng.middle.web.shop.event.listener.CreateOpenShopRelationListener;
import com.pousheng.middle.web.user.component.ParanaUserOperationLogic;
import com.pousheng.middle.web.user.component.UcUserOperationLogic;
import com.pousheng.middle.web.utils.operationlog.OperationLogParam;
import com.pousheng.middle.web.utils.operationlog.OperationLogType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.common.utils.Splitters;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.common.shop.service.OpenShopWriteService;
import io.terminus.open.client.parana.dto.ParanaCallResult;
import io.terminus.open.client.parana.item.SyncParanaShopService;
import io.terminus.parana.cache.ShopCacher;
import io.terminus.parana.common.exception.InvalidException;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.Iters;
import io.terminus.parana.common.utils.RespHelper;
import io.terminus.parana.common.utils.UserUtil;
import io.terminus.parana.shop.model.Shop;
import io.terminus.parana.shop.service.AdminShopWriteService;
import io.terminus.parana.shop.service.ShopReadService;
import io.terminus.parana.shop.service.ShopWriteService;
import io.terminus.parana.user.ext.UserTypeBean;
import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static com.pousheng.middle.constants.Constants.MANAGE_ZONE_IDS;

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
    @Setter
    private ShopReadService shopReadService;

    @RpcConsumer
    @Setter
    private PsShopReadService psShopReadService;

    @RpcConsumer
    @Setter
    private ShopWriteService shopWriteService;

    @RpcConsumer
    private AdminShopWriteService adminShopWriteService;
    @Autowired(required = false)
    private SyncParanaShopService syncParanaShopService;

    @Autowired(required = false)
    private UcUserOperationLogic ucUserOperationLogic;
    @Autowired
    private EventBus eventBus;
    @Autowired
    @Setter
    private ShopCacher shopCacher;
    @Autowired
    private ParanaUserOperationLogic paranaUserOperationLogic;
    @Autowired
    private MposWarehousePusher mposWarehousePusher;
    @RpcConsumer
    private OpenShopReadService openShopReadService;
    @RpcConsumer
    private OpenShopWriteService openShopWriteService;
    @Autowired
    private MemberShopOperationLogic memberShopOperationLogic;
    @Autowired
    @Setter
    private MiddleShopCacher middleShopCacher;
    @Autowired
    private UserTypeBean userTypeBean;
    @Autowired
    private ShopChannelGroupCacher shopChannelGroupCacher;
    @Autowired
    private CreateOpenShopRelationListener createOpenShopRelationListener;
    @Autowired
    private ZoneContractReadService zoneContractReadService;
    @Value("${pousheng.order.email.remind.group}")
    private String[] mposEmailGroup;
    @Autowired
    private WarehouseCacher warehouseCacher;
    @Autowired
    private WarehouseClient warehouseClient;
    @Autowired
    @Setter
    private ShopBusinessLogic shopBusinessLogic;
    @Autowired
    @Setter
    private OrderShipmentReadService orderShipmentReadService;
    @Autowired
    private MiddleParanaClient paranaClient;
    @Autowired
    private ShopMaxOrderLogic shopMaxOrderLogic;

    private static final JsonMapper mapper = JsonMapper.nonEmptyMapper();

    private static String OPEN_SHOP_ID = "openShopId";


    @ApiOperation("根据门店id查询门店信息")
    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Shop findShopById(@PathVariable("id") Long shopId) {
        Response<Shop> rShop = shopReadService.findById(shopId);
        if (!rShop.isSuccess()) {
            throw new JsonResponseException(rShop.getError());
        }
        return rShop.getResult();
    }

    @ApiOperation("根据门店id调用会员中心查询门店地址信息")
    @RequestMapping(value = "/address/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public AddressGps findShopAddress(@PathVariable("id") Long shopId) {
        Response<Shop> rShop = shopReadService.findById(shopId);
        if (!rShop.isSuccess()) {
            throw new JsonResponseException(rShop.getError());
        }
        Shop shop = rShop.getResult();
        return memberShopOperationLogic.getAddressGps(shopId,String.valueOf(shop.getBusinessId()),shop.getOuterId());
    }

    @ApiOperation("根据门店id调用会员中心查询门店地址信息并修复到中台")
    @RequestMapping(value = "/address/{id}/fix", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public String fixShopAddress(@PathVariable("id") Long shopId) {
        Response<Shop> rShop = shopReadService.findById(shopId);
        if (!rShop.isSuccess()) {
            throw new JsonResponseException(rShop.getError());
        }

        Shop shop = rShop.getResult();
        UpdateShopEvent updateShopEvent = new UpdateShopEvent(shop.getId(), shop.getBusinessId(), shop.getOuterId());
        eventBus.post(updateShopEvent);
        return "success";
    }


    @ApiOperation("查询渠道列表信息")
    @RequestMapping(value = "/channel", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Channel> findChannels() {
        List<Channel> list = Lists.newArrayList();
        for (MiddleChannel channel : MiddleChannel.values()) {
            list.add(new Channel().name(channel.getDesc()).code(channel.getValue()));
        }
        return list;
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

        ParanaUser paranaUser = UserUtil.getCurrentUser();

        List<String> zoneIds = null;

        if (!userTypeBean.isAdmin(paranaUser)) {
            Map<String, String> extraMap = paranaUser.getExtra();

            //如果没有设置区域则返回空
            if(CollectionUtils.isEmpty(extraMap)||!extraMap.containsKey(MANAGE_ZONE_IDS)){
                return new Paging<>();
            }

            String zoneIdStr = extraMap.get(MANAGE_ZONE_IDS);
            //如果没有设置区域则返回空
            if(Strings.isNullOrEmpty(zoneIdStr)){
                return new Paging<>();
            }

            zoneIds = JsonMapper.JSON_NON_EMPTY_MAPPER.fromJson(extraMap.get(Constants.MANAGE_ZONE_IDS),JsonMapper.JSON_NON_EMPTY_MAPPER.createCollectionType(List.class,String.class));
        }

        Response<Paging<Shop>> resp = psShopReadService.pagination(name, userId, type, status,outerId,companyId, zoneIds,pageNo, pageSize);
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
            ShopExtraInfo shopExtraInfo = ShopExtraInfo.fromJson(shop.getExtra());
            try {
                //门店手机号、邮箱、地址实时查询会员中心
                Optional<MemberShop> memberShopOptional = memberShopOperationLogic.findShopByCodeAndType(shop.getOuterId(),1,shopExtraInfo.getCompanyId().toString());
                if(memberShopOptional.isPresent()){
                    MemberShop memberShop = memberShopOptional.get();
                    shopExtraInfo.setPhone(memberShop.getTelphone());
                    shopExtraInfo.setEmail(memberShop.getEmail());
                    shop.setPhone(memberShop.getTelphone());
                    //如果发现中台和会员的地址不一致，则发出更新地址事件（增量修复）
                    if(!Strings.isNullOrEmpty(memberShop.getAddress())&&!Objects.equal(memberShop.getAddress(),shop.getAddress())){
                        UpdateShopEvent updateShopEvent = new UpdateShopEvent(shop.getId(),shop.getBusinessId(),shop.getOuterId());
                        eventBus.post(updateShopEvent);
                    }
                    if(Strings.isNullOrEmpty(memberShop.getAddress())){
                        shop.setAddress("地址信息缺失，请去恒康系统维护！");
                    }else {
                        shop.setAddress(memberShop.getAddress());
                    }
                }else {
                    log.error("not find member shop by outer id:{} company id:{}",shop.getOuterId(),shopExtraInfo.getCompanyId());
                }

            }catch (JsonResponseException e){
                log.error("find shop by code:{}, type:{},companyId:{} fail,error:{}",shop.getOuterId(),1,shopExtraInfo.getCompanyId(),Throwables.getStackTraceAsString(e));
            }

            shopPag.setShopExtraInfo(shopExtraInfo);
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

        //判断是否已创建
        Shop recoverShop = checkOuterId(shop.getOuterId(),shop.getCompanyId());

        // TODO 判断对应仓库是否存在，目前使用OUTCODE，后面调整
        checkWarehouse(shop.getOuterId(), shop.getCompanyId());

        //创建门店
        shop.setBusinessId(shop.getCompanyId());//这里把公司id塞入到businessId字段中
        ShopExtraInfo shopExtraInfo = new ShopExtraInfo();
        shopExtraInfo.setCompanyId(shop.getCompanyId());
        shopExtraInfo.setShopInnerCode(shop.getStoreId());
        shopExtraInfo.setCompanyName(shop.getCompanyName());
        shopExtraInfo.setEmail(shop.getEmail());
        //门店营业状态、营业时间等营业信息默认值
        ShopBusinessTime shopBusinessTime = new ShopBusinessTime();
        setInitBusinessInfo(shopBusinessTime);
        shopExtraInfo.setShopBusinessTime(shopBusinessTime);

        Map<String,String> extraMap = Maps.newHashMap();
        shop.setExtra(ShopExtraInfo.putExtraInfo(extraMap,shopExtraInfo));





        Long id;
        if(Arguments.isNull(recoverShop)){
            //创建门店用户
            String userNmae = shop.getCompanyId()+"-"+shop.getOuterId();
            Response<UcUserInfo> userInfoRes = ucUserOperationLogic.createUcUserForShop(userNmae,password);
            if(!userInfoRes.isSuccess()){
                log.error("create user(name:{}) fail,error:{}",shop.getOuterId(),userInfoRes.getError());
                throw new JsonResponseException(userInfoRes.getError());
            }
            //创建门店信息
            id = createShop(shop, userInfoRes.getResult().getUserId(), userNmae);

            CreateShopEvent addressEvent = new CreateShopEvent(id,shop.getCompanyId(),shop.getOuterId(),shop.getOuterId());
            eventBus.post(addressEvent);
        }else {
            //更新门店用户
            String userNmae = shop.getCompanyId()+"-"+shop.getOuterId();
            Response<UcUserInfo> userInfoRes = ucUserOperationLogic.updateUcUser(recoverShop.getUserId(),userNmae,password);
            if(!userInfoRes.isSuccess()){
                log.error("update user(name:{}) fail,error:{}",recoverShop.getOuterId(),userInfoRes.getError());
                throw new JsonResponseException(userInfoRes.getError());
            }
            //解冻
            RespHelper.or500(paranaUserOperationLogic.updateUserStatus(1,recoverShop.getUserId()));
            //更新门店信息
            id = updateShop(recoverShop.getId());

            // TODO 设置对应仓库标签， 现在先用outCode，后面改造，方法已提供，直接换
            warehouseClient.markMposOrNotWithOutCode(shop.getOuterId(), String.valueOf(shop.getCompanyId()), true);

            UpdateShopEvent updateShopEvent = new UpdateShopEvent(id,shop.getCompanyId(),recoverShop.getOuterId());
            eventBus.post(updateShopEvent);

            //同步电商
            syncParanaShop(recoverShop);
        }

        return Collections.singletonMap("id", id);
    }

    @ApiOperation("补偿门店信息")
    @RequestMapping(value = "/create/open/shop", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean createOpenShop(Long id){
        Response<Shop> resp=shopReadService.findById(id);
        if(!resp.isSuccess()){
            throw new JsonResponseException(resp.getError());
        }
        Shop shop =resp.getResult();
        if (!StringUtils.isEmpty(shop.getExtra().get(OPEN_SHOP_ID))) {
            throw new JsonResponseException("open.shop.is.exist");
        }
        CreateShopEvent addressEvent = new CreateShopEvent(id,shop.getBusinessId(),shop.getOuterId(),shop.getOuterId());
        eventBus.post(addressEvent);
        return true;
    }

    private Shop checkOuterId(String outerId,Long companyId){
        if(Strings.isNullOrEmpty(outerId)){
            log.error("shop outer id is null");
            throw new JsonResponseException("shop.outer.in.invalid");
        }
        Response<Optional<Shop>> shopRes = psShopReadService.findByOuterIdAndBusinessId(outerId,companyId);
        if(!shopRes.isSuccess()){
            log.error("find shop by outer id:{} and business id:{} fail,error:{}",outerId,companyId,shopRes.getError());
            throw new JsonResponseException(shopRes.getError());
        }else {
            Optional<Shop> shopOptional = shopRes.getResult();
            if(shopOptional.isPresent()){
                Shop shop = shopOptional.get();
                log.warn("shop(id:{}) ,status:{} exist outer id:{}",shop.getId(),shop.getStatus(),outerId);
                if(Objects.equal(shop.getStatus(),-1)){
                    log.warn("shop(id:{}),status:{} exist outer id:{} ,so to recover status 1",shop.getId(),shop.getStatus(),outerId);
                    return shop;
                }
                throw new JsonResponseException("duplicate.shop.outer.id");
            }
        }

        return null;

    }

    private void checkWarehouse(String outerId,Long companyId){
        if(Strings.isNullOrEmpty(outerId)){
            log.error("shop outer id is null");
            throw new JsonResponseException("shop.outer.in.invalid");
        }
        Response<WarehouseDTO> warehouseRes = warehouseClient.findByOutCodeBizId(outerId,String.valueOf(companyId));
        if(!warehouseRes.isSuccess() || null == warehouseRes.getResult()){
            log.error("no warehouse found by outer id:{} and business id:{} fail,error:{}",outerId,companyId, warehouseRes);
            throw new JsonResponseException("warehouse.not.found");
        }
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
        Shop toCreate = new Shop();
        toCreate.setName(shop.getName());
        toCreate.setBusinessId(shop.getBusinessId());
        toCreate.setImageUrl(shop.getImageUrl());
        toCreate.setType(MoreObjects.firstNonNull(shop.getType(),1));
        toCreate.setStatus(ShopOpeningStatus.OPENING.value());

        toCreate.setUserId(userId);
        toCreate.setOuterId(shop.getOuterId());
        toCreate.setUserName(userName);
        toCreate.setPhone(shop.getPhone());
        toCreate.setAddress(shop.getAddress());
        toCreate.setExtra(shop.getExtra());
        toCreate.setZoneId(shop.getZoneId());
        toCreate.setZoneName(shop.getZoneName());

        Response<Long> rShop = shopWriteService.create(toCreate);
        if (!rShop.isSuccess()) {
            log.warn("shop create failed, error={}", rShop.getError());
            throw new JsonResponseException(rShop.getError());
        }

        // TODO 设置对应仓库标签， 现在先用outCode，后面改造，方法已提供，直接换
        warehouseClient.markMposOrNotWithOutCode(toCreate.getOuterId(), String.valueOf(toCreate.getBusinessId()), true);

        //同步电商
        syncParanaShop(toCreate);

        //刷新open shop缓存
        shopChannelGroupCacher.refreshShopChannelGroupCache();

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
        toUpdate.setZoneId(shop.getZoneId());
        toUpdate.setZoneName(shop.getZoneName());
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
        LoginTokenInfo token = ucUserOperationLogic.getUserToken(exist.getUserName(),password);
        if(token.getError() == null){
            log.error("new password can not same as old password");
            throw new JsonResponseException("modify.password.fail");
        }
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
    @OperationLogType("更新门店安全库存信息")
    public void updateShopSafeStock(@PathVariable @OperationLogParam Long shopId, @RequestParam Integer safeStock) {
        log.info("UPDATE-SAFE-STOCK shop:{} safe stock to:{}",shopId,safeStock);
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

        //刷新缓存
        shopCacher.refreshShopById(shopId);
        middleShopCacher.refreshByOuterIdAndBusinessId(exist.getOuterId(),exist.getBusinessId());
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
                Response<Boolean> syncParanaExpressRes = syncParanaShopService.syncShopExpress(exist.getOuterId(),exist.getBusinessId(),shopExpresssCompany.getCode(),shopExpresssCompany.getName());
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
    @ApiOperation("更新单个门店服务信息")
    @RequestMapping(value = "/{shopId}/server/info", method = RequestMethod.PUT)
    public void updateShopServerInfo(@PathVariable Long shopId, @RequestBody ShopServerInfo shopServerInfo) {

        val rExist = shopReadService.findById(shopId);
        if (!rExist.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}",shopId,rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        Shop exist = rExist.getResult();

        ShopExtraInfo existShopExtraInfo = ShopExtraInfo.fromJson(exist.getExtra());

        Response<OpenShop> openShopRes = openShopReadService.findById(existShopExtraInfo.getOpenShopId());
        if(!openShopRes.isSuccess()){
            log.error("find open shop by id:{} fail,error:{}",existShopExtraInfo.getOpenShopId(),openShopRes.getError());
            throw new JsonResponseException(openShopRes.getError());
        }
        OpenShop openShop = openShopRes.getResult();
        if(Arguments.isNull(openShop)){
            log.error("not find open shop by id:{}",existShopExtraInfo.getOpenShopId());
            throw new JsonResponseException("open.shop.not.exist");
        }
        Map<String,String> openExtra = openShop.getExtra();
        if(CollectionUtils.isEmpty(openExtra)){
            openExtra = Maps.newHashMap();
        }

        ShopServerInfo toUpdateServerInfo = existShopExtraInfo.getShopServerInfo();
        if(Arguments.isNull(toUpdateServerInfo)){
            toUpdateServerInfo = new ShopServerInfo();
        }
        toUpdateServerInfo.setReturnWarehouseCode(shopServerInfo.getReturnWarehouseCode());
        toUpdateServerInfo.setReturnWarehouseId(shopServerInfo.getReturnWarehouseId());
        toUpdateServerInfo.setReturnWarehouseName(shopServerInfo.getReturnWarehouseName());

        openExtra.put(TradeConstants.DEFAULT_REFUND_WAREHOUSE_ID,shopServerInfo.getReturnWarehouseId()!=null?shopServerInfo.getReturnWarehouseId().toString():null);
        openExtra.put(TradeConstants.DEFAULT_REFUND_OUT_WAREHOUSE_CODE,shopServerInfo.getReturnWarehouseCode());
        openExtra.put(TradeConstants.DEFAULT_REFUND_WAREHOUSE_NAME,shopServerInfo.getReturnWarehouseName());

        toUpdateServerInfo.setCompanyId(shopServerInfo.getCompanyId());
        toUpdateServerInfo.setVirtualShopName(shopServerInfo.getVirtualShopName());
        toUpdateServerInfo.setVirtualShopCode(shopServerInfo.getVirtualShopCode());
        toUpdateServerInfo.setVirtualShopInnerCode(shopServerInfo.getVirtualShopInnerCode());
        toUpdateServerInfo.setCompanyId(shopServerInfo.getCompanyId());

        openExtra.put(TradeConstants.HK_PERFORMANCE_SHOP_CODE,shopServerInfo.getVirtualShopInnerCode());
        openExtra.put(TradeConstants.HK_PERFORMANCE_SHOP_NAME,shopServerInfo.getVirtualShopName());
        openExtra.put(TradeConstants.HK_PERFORMANCE_SHOP_OUT_CODE,shopServerInfo.getVirtualShopCode());
        openExtra.put(TradeConstants.HK_COMPANY_CODE,shopServerInfo.getCompanyId());


        Shop toUpdate = new Shop();
        toUpdate.setId(shopId);
        existShopExtraInfo.setShopServerInfo(toUpdateServerInfo);
        toUpdate.setExtra(ShopExtraInfo.putExtraInfo(exist.getExtra(),existShopExtraInfo));
        Response<Boolean> resp = shopWriteService.update(toUpdate);
        if (!resp.isSuccess()) {
            log.error("update shop extra:{} failed, shopId={}, error={}",existShopExtraInfo, shopId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
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
        //同步电商
        syncParanaFrozenShop(exist.getOuterId(),exist.getBusinessId());
        RespHelper.or500(paranaUserOperationLogic.updateUserStatus(-2,exist.getUserId()));

        String shopInfo = Joiner.on("_").join(Lists.newArrayList(exist.getOuterId(),exist.getBusinessId()));
        WarehouseDTO warehouse = warehouseCacher.findByShopInfo(shopInfo);
        if (warehouse != null) {
            RespHelper.or500(warehouseClient.updateStatus(warehouse.getId(), -2));
            warehouseCacher.refreshById(warehouse.getId());
        }

        RespHelper.or500(adminShopWriteService.frozen(shopId));

        shopCacher.refreshShopById(shopId);
        middleShopCacher.refreshByOuterIdAndBusinessId(exist.getOuterId(),exist.getBusinessId());
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
        //同步电商
        syncParanaUnFrozenShop(exist.getOuterId(),exist.getBusinessId());
        RespHelper.or500(paranaUserOperationLogic.updateUserStatus(1,exist.getUserId()));
        String shopInfo = Joiner.on("_").join(Lists.newArrayList(exist.getOuterId(),exist.getBusinessId()));
        WarehouseDTO warehouse = warehouseCacher.findByShopInfo(shopInfo);
        if (warehouse != null) {
            RespHelper.or500(warehouseClient.updateStatus(warehouse.getId(), 1));
            warehouseCacher.refreshById(warehouse.getId());
        }
        RespHelper.or500(adminShopWriteService.unfrozen(shopId));
        shopCacher.refreshShopById(shopId);
        middleShopCacher.refreshByOuterIdAndBusinessId(exist.getOuterId(),exist.getBusinessId());


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
        //同步电商
        syncParanaCloseShop(exist.getOuterId(),exist.getBusinessId());
        RespHelper.or500(paranaUserOperationLogic.updateUserStatus(-2,exist.getUserId()));
        // TODO 取消对应仓库标签， 现在先用outCode，后面改造，方法已提供，直接换
        warehouseClient.markMposOrNotWithOutCode(exist.getOuterId(), String.valueOf(exist.getBusinessId()), false);
        RespHelper.or500(adminShopWriteService.close(shopId));
        shopCacher.refreshShopById(shopId);
        middleShopCacher.refreshByOuterIdAndBusinessId(exist.getOuterId(),exist.getBusinessId());
    }

    @ApiOperation("设置邮箱和手机号")
    @RequestMapping(value = "/{shopId}/communication",method = RequestMethod.PUT)
    public void setCommunication(@PathVariable Long shopId,@RequestParam String email,@RequestParam String phone){
        val rExist = shopReadService.findById(shopId);
        if (!rExist.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}",shopId,rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        Shop exist = rExist.getResult();
        ShopExtraInfo shopExtraInfo = ShopExtraInfo.fromJson(exist.getExtra());
        shopExtraInfo.setEmail(email);
        Shop update = new Shop();
        update.setId(exist.getId());
        update.setExtra(ShopExtraInfo.putExtraInfo(exist.getExtra(),shopExtraInfo));
        update.setPhone(phone);
        Response<Boolean> response = shopWriteService.update(update);
        if(!response.isSuccess()){
            log.error("update shop(id:{}) failed,cause:{}",shopId,response.getError());
            throw new JsonResponseException(response.getError());
        }
        log.info("shop(name:{}) set email:{} success!",exist.getName(),email);
        if (!Objects.equal(exist.getPhone(), phone)) {
            syncParanaShopPhone(exist.getOuterId(), exist.getBusinessId(), phone);
        }
    }


    @RequestMapping(value = "/{outerId}/cache/{businessId}", method = RequestMethod.GET)
    public Shop testCache(@PathVariable String outerId,@PathVariable Long businessId ) {
        return middleShopCacher.findByOuterIdAndBusinessId(outerId,businessId);
    }

    @RequestMapping(value = "/{outerId}/cache/{businessId}/clear", method = RequestMethod.GET)
    public void testClearCache(@PathVariable String outerId,@PathVariable Long businessId ) {
        middleShopCacher.refreshByOuterIdAndBusinessId(outerId,businessId);
    }


    @RequestMapping(value = "/{id}/fix/open/shop", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public void fixOpenShop(@PathVariable("id") Long shopId) {
        Response<Shop> rShop = shopReadService.findById(shopId);
        if (!rShop.isSuccess()) {
            throw new JsonResponseException(rShop.getError());
        }
        Shop shop = rShop.getResult();
        CreateShopEvent createShopEvent = new CreateShopEvent(shopId,shop.getBusinessId(),shop.getOuterId(),shop.getOuterId());

        createOpenShopRelationListener.createOpenShopRelation(createShopEvent);
    }

    @ApiOperation("获取区部经理和负责人邮箱")
    @RequestMapping(value = "/zone/email", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> findEmailList(@RequestParam String outerId, @RequestParam Long businessId) {
        ArrayList<String> emails = Lists.newArrayList();
        Response<Optional<Shop>> response = psShopReadService.findByOuterIdAndBusinessId(outerId, businessId);
        if (!response.isSuccess()) {
            log.error("find shop by outer id:{} business id:{},cause:{}",outerId,businessId,response.getError());
        }
        Optional<Shop> shopOptional = response.getResult();
        if (shopOptional.isPresent()) {
            Shop shop = shopOptional.get();
            if (org.apache.commons.lang3.StringUtils.isNotBlank(shop.getZoneId())) {
                Response<List<ZoneContract>> listResponse = zoneContractReadService.findByZoneId(shop.getZoneId());
                if (!response.isSuccess()) {
                    log.error("zoneContractReadService findByZoneId  fail,zoneId={}", shop.getZoneId());
                }
                listResponse.getResult().stream().forEach(item -> emails.add(item.getEmail()));
            }
        }
        if (!CollectionUtils.isEmpty(Arrays.asList(mposEmailGroup))) {
            emails.addAll(Arrays.asList(mposEmailGroup));
        }
        return emails;
    }
    /**
     * @Description 查询门店营业信息
     * @Date 2018/5/9
     * @param shopId
     * @return com.pousheng.middle.shop.dto.ShopPaging
     */
    @ApiOperation("查询门店营业信息(门店类型、门店接单时间、门店接单量等)")
    @RequestMapping(value = "/{shopId}/get/shop/business/info", method = RequestMethod.GET)
    public ShopBusinessInfo getShopBusinessInfo(@PathVariable Long shopId){

        ShopBusinessInfo shopBusinessInfo = new ShopBusinessInfo();
        try {

            Response<Shop> rExist = shopReadService.findById(shopId);
            if (!rExist.isSuccess()) {
                log.error("find shop by id:{} fail,error:{}", shopId, rExist.getError());
                throw new JsonResponseException(rExist.getError());
            }
            Shop shop = rExist.getResult();
            ShopExtraInfo existShopExtraInfo = ShopExtraInfo.fromJson(shop.getExtra());

            shopBusinessInfo.setId(shop.getId());
            shopBusinessInfo.setType(shop.getType());
            shopBusinessInfo.setOutId(shop.getOuterId());
            shopBusinessInfo.setBusinessId(shop.getBusinessId());
            shopBusinessInfo.setShopBusinessTime(existShopExtraInfo.getShopBusinessTime());

            return shopBusinessInfo;
        } catch (Exception e){
            throw new JsonResponseException(e.getMessage());
        }
    }

    /**
     * @Description 查询MPOS门店营业信息
     * @Date 2018/5/9
     * @param outerId
     * @param businessId
     * @return com.pousheng.middle.shop.dto.ShopPaging
     */
    @ApiOperation("查询MPOS门店营业信息")
    @RequestMapping(value = "/get/mpos/shop/business/info/by/outerid/and/businessid", method = RequestMethod.GET)
    public ShopBusinessInfo getShopBusinessInfoByOuterIdAndBusinessId(@RequestParam String outerId, @RequestParam Long businessId){
        try {
            ShopBusinessInfo shopBusinessInfo = new ShopBusinessInfo();
            val rExist = psShopReadService.findByOuterIdAndBusinessId(outerId, businessId);
            if (!rExist.isSuccess()) {
                log.error("find shop by outerId({}) and businessId({}) fail,error:{}", outerId, businessId, rExist.getError());
                throw new JsonResponseException(rExist.getError());
            }
            Shop shop = rExist.getResult().get();
            ShopExtraInfo existShopExtraInfo = ShopExtraInfo.fromJson(shop.getExtra());
            shopBusinessInfo.setId(shop.getId());
            shopBusinessInfo.setType(shop.getType());
            shopBusinessInfo.setOutId(shop.getOuterId());
            shopBusinessInfo.setBusinessId(shop.getBusinessId());
            shopBusinessInfo.setShopBusinessTime(existShopExtraInfo.getShopBusinessTime());
            return shopBusinessInfo;
        } catch (Exception e){
            throw new JsonResponseException(e.getMessage());
        }
    }

    /**
     * @Description 查询门店订单到期时间
     * @Date 2018/5/11
     * @param outerId
     * @param businessId
     * @param orderDateTime 订单下单日期
     * @return com.pousheng.middle.web.shop.dto.OrderExpireInfo
     */
    @ApiOperation("查询门店订单到期时间")
    @RequestMapping(value = "/get/mpos/shop/expire/time/by/orderdatetime", method = RequestMethod.GET)
    public OrderExpireInfo getShopOrderExpireTime(@RequestParam String outerId, @RequestParam Long businessId,@RequestParam LocalDateTime orderDateTime){
        try{

            OrderExpireInfo orderExpireInfo = new OrderExpireInfo();

            //查询通过outerId,businessId获取门店信息
            val rExist = psShopReadService.findByOuterIdAndBusinessId(outerId, businessId);
            if (!rExist.isSuccess()) {
                log.error("find shop by outerId({}) and businessId({}) fail,error:{}", outerId, businessId, rExist.getError());
                throw new JsonResponseException(rExist.getError());
            }
            Shop shop = rExist.getResult().get();
            ShopExtraInfo existShopExtraInfo = ShopExtraInfo.fromJson(shop.getExtra());
            ShopBusinessTime shopBusinessTime = existShopExtraInfo.getShopBusinessTime();

            //订单超时时间和订单发送邮件超时时间
            int orderTimeoutSeting = shopBusinessTime.getOrderTimeout();
            int orderEmailTimeoutSeting = shopBusinessTime.getOrderEmailTimeout();

            //门店营业时间map
            Map<DayOfWeek,String[]> weekTimeMap =  shopBusinessLogic.getWeekMap(shopBusinessTime);
            LocalTime orderTime = orderDateTime.toLocalTime();
            LocalDate orderDate = orderDateTime.toLocalDate();

            //校验门店是否可以处理此订单，避免shopBusinessLogic回归调用死循环
            if(!shopBusinessLogic.validShopOrderCapacity(weekTimeMap)){
                throw new InvalidException(500, "shop.order.capacity.valid.illegal");
            }

            //赋值订单过期时间，订单邮件过期时间
            LocalDateTime orderEexcpireDateTime = shopBusinessLogic.getExcpireDateTime(weekTimeMap,orderTime,orderDate,orderTimeoutSeting);
            LocalDateTime emailExcpireDateTime = shopBusinessLogic.getExcpireDateTime(weekTimeMap,orderTime,orderDate,orderEmailTimeoutSeting);
            orderExpireInfo.setOrderExpireTime(orderEexcpireDateTime);
            orderExpireInfo.setEmailExpireTime(emailExcpireDateTime);

            return orderExpireInfo;

        } catch (Exception e) {
            log.error("get Expire time of order fail, outerId({}), businessId({}) and orderDateTime({}),error:{}",
                    outerId, businessId,orderDateTime,e.getMessage());
            throw new JsonResponseException(e.getMessage());
        }
    }


    /**
     * @Description 更新门店营业信息
     * @Date 2018/5/9
     * @param shopId
     * @param type 门店类型
     * @param shopBusiness 营业时间
     * @return io.terminus.common.model.Response<java.lang.Boolean>
     */
    @ApiOperation("更新门店营业信息(门店类型、门店接单时间、门店接单量等)")
    @RequestMapping(value = "/{shopId}/update/shop/business/info", method = RequestMethod.PUT)
    public Response<Boolean> updateShopBusinessInfo(@PathVariable Long shopId,
                                                    @RequestBody ShopBusinessInfo shopBusinessInfo) {

        //请求参数校验 营业时间校验
        List valideResultList = new ArrayList();
        Integer type = shopBusinessInfo.getType();
        ShopBusinessTime shopBusinessTime = shopBusinessInfo.getShopBusinessTime();
        if (shopBusinessTime != null) {
            Integer openingStatus = shopBusinessTime.getOpeningStatus();
            if (openingStatus != null && openingStatus.equals(ShopOpeningStatus.OPENING.value())) {
                if (!shopBusinessLogic.valideBusinessTime(shopBusinessTime.getOpeningStatusMon(),
                        shopBusinessTime.getOpeningStartTimeMon(),
                        shopBusinessTime.getOpeningEndTimeMon()
                )) {
                    valideResultList.add("Mon");
                }
                if (!shopBusinessLogic.valideBusinessTime(shopBusinessTime.getOpeningStatusTue(),
                        shopBusinessTime.getOpeningStartTimeTue(),
                        shopBusinessTime.getOpeningEndTimeTue()
                )) {
                    valideResultList.add("Tue");
                }
                if (!shopBusinessLogic.valideBusinessTime(shopBusinessTime.getOpeningStatusWed(),
                        shopBusinessTime.getOpeningStartTimeWed(),
                        shopBusinessTime.getOpeningEndTimeWed()
                )) {
                    valideResultList.add("Wed");
                }
                if (!shopBusinessLogic.valideBusinessTime(shopBusinessTime.getOpeningStatusThu(),
                        shopBusinessTime.getOpeningStartTimeThu(),
                        shopBusinessTime.getOpeningEndTimeThu()
                )) {
                    valideResultList.add("Thu");
                }
                if (!shopBusinessLogic.valideBusinessTime(shopBusinessTime.getOpeningStatusFri(),
                        shopBusinessTime.getOpeningStartTimeFri(),
                        shopBusinessTime.getOpeningEndTimeFri()
                )) {
                    valideResultList.add("Fri");
                }
                if (!shopBusinessLogic.valideBusinessTime(shopBusinessTime.getOpeningStatusSat(),
                        shopBusinessTime.getOpeningStartTimeSat(),
                        shopBusinessTime.getOpeningEndTimeSat()
                )) {
                    valideResultList.add("Sat");
                }
                if (!shopBusinessLogic.valideBusinessTime(shopBusinessTime.getOpeningStatusSun(),
                        shopBusinessTime.getOpeningStartTimeSun(),
                        shopBusinessTime.getOpeningEndTimeSun()
                )) {
                    valideResultList.add("Sun");
                }
            }

            if (!valideResultList.isEmpty()) {
                log.error("valid shop(id:{}) business time error fail,week:{}", shopId, valideResultList.toString());
                throw new InvalidException(500, "(outCode={0})shop.business.time.valid.illegal",
                        valideResultList.toString());
            }
        }
        //店铺是否存在
        Response<Shop> rExist = shopReadService.findById(shopId);
        if (!rExist.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}", shopId, rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }

        Shop toUpdate = rExist.getResult();
        //类型赋值
        toUpdate.setType(type);

        //扩展字段赋值
        if (shopBusinessTime != null) {
            ShopExtraInfo shopExtraInfo = ShopExtraInfo.fromJson(toUpdate.getExtra());
            if (Arguments.isNull(shopExtraInfo)) {
                log.error("not find shop(id:{}) extra info by shop extra info json:{} ",
                        toUpdate.getId(), toUpdate.getExtra());
                throw new ServiceException("shop.extra.info.invalid");
            }
            shopExtraInfo.setShopBusinessTime(shopBusinessTime);
            toUpdate.setExtra(ShopExtraInfo.putExtraInfo(toUpdate.getExtra(), shopExtraInfo));
        }

        Response<Boolean> resp = shopWriteService.update(toUpdate);
        if (!resp.isSuccess()) {
            log.error("update shop(shopId={}) business info failed, error={}", shopId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        //刷新缓存
        shopCacher.refreshShopById(shopId);
        middleShopCacher.refreshByOuterIdAndBusinessId(toUpdate.getOuterId(),toUpdate.getBusinessId());

        //更新缓存中的超过最大接单量的标志位
        if (shopBusinessTime != null) {
            shopMaxOrderLogic.changeMaxOrderAcceptQtyFlag(shopBusinessTime.getOrderAcceptQtyMax(), toUpdate);
        }
        return resp;

    }

    /**
     * @Description 更新MPOS门店营业状态
     * @Date 2018/5/10
     * @param shopBusinessInfo 门店营业信息
     * @return io.terminus.common.model.Response<java.lang.Boolean>
     */
    @ApiOperation("更新MPOS门店营业状态")
    @RequestMapping(value = "/update/mpos/shop/business/info", method = RequestMethod.PUT)
    public Response<Boolean> updateShopBusinessInfo(@RequestBody ShopBusinessInfo shopBusinessInfo) {
        try {
            Long shopId = null;
            String outerId = shopBusinessInfo.getOutId();
            Long businessId = shopBusinessInfo.getBusinessId();

            val rExist = psShopReadService.findByOuterIdAndBusinessId(outerId, businessId);
            if (!rExist.isSuccess()) {
                log.error("find shop by outerId({}) and businessId({}) fail,error:{}",
                        outerId, businessId, rExist.getError());
                throw new JsonResponseException(rExist.getError());
            }
            Shop toUpdate = rExist.getResult().get();
            shopId = toUpdate.getId();
            ShopExtraInfo shopExtraInfo = ShopExtraInfo.fromJson(toUpdate.getExtra());
            int openingStatus = shopBusinessInfo.getShopBusinessTime().getOpeningStatus();
            shopExtraInfo.getShopBusinessTime().setOpeningStatus(openingStatus);
            toUpdate.setExtra(ShopExtraInfo.putExtraInfo(toUpdate.getExtra(), shopExtraInfo));
            Response<Boolean> resp = shopWriteService.update(toUpdate);
            if (!resp.isSuccess()) {
                log.error("update shop by outerId({}) and businessId({}) fail,error:{}",
                        outerId, businessId, rExist.getError());
                throw new JsonResponseException(500, resp.getError());
            }
            //刷新缓存
            shopCacher.refreshShopById(shopId);
            middleShopCacher.refreshByOuterIdAndBusinessId(toUpdate.getOuterId(),toUpdate.getBusinessId());
            return resp;

        } catch (Exception e){
            throw new JsonResponseException(500, e.getMessage());
        }
    }


    @ApiOperation("修改必须接单门店")
    @RequestMapping(value = "/{shopId}/no/reject", method = RequestMethod.PUT)
    public void NoRejectSeller(@PathVariable Long shopId, Boolean canNotReject) {
        val rExist = shopReadService.findById(shopId);
        if (!rExist.isSuccess()) {
            log.error("find shop by id:{} fail,error:{}", shopId, rExist.getError());
            throw new JsonResponseException(rExist.getError());
        }
        Shop exist = rExist.getResult();
        Map<String, Object> params = new HashMap<>(4);
        params.put("canNotReject", canNotReject);
        params.put("outerId", exist.getOuterId());
        params.put("businessId", exist.getBusinessId());
        String responseBody = paranaClient.systemPost("sync.shop.no.reject.api", params);
        //同步电商
        ParanaCallResult res = mapper.fromJson(responseBody, ParanaCallResult.class);
        log.info("sync to parana ,res is {}", res);
        if (!res.getSuccess()) {
            throw new JsonResponseException(res.getErrorMessage());
        }
        Map<String, String> extra = exist.getExtra();
        extra.put("canNotReject", canNotReject.toString());
        exist.setExtra(extra);
        Response<Boolean> result = shopWriteService.update(exist);
        if (!result.isSuccess()) {
            throw new JsonResponseException(result.getError());
        }
        shopCacher.refreshShopById(shopId);
        middleShopCacher.refreshByOuterIdAndBusinessId(exist.getOuterId(), exist.getBusinessId());
    }


    /**
     * @Description 查询MPOS门店当前营业状
     * @Date   2018/5/16
     * @param  outerId
     * @param  businessId
     * @return boolean
     */
    @ApiOperation("查询MPOS门店当前营业状")
    @RequestMapping(value = "/get/mpos/shop/current/opening/status", method = RequestMethod.GET)
    public boolean getShopCurrentStatus(@RequestParam String outerId, @RequestParam Long businessId) {
        try{

            //查询通过outerId,businessId获取门店信息
            val rExist = psShopReadService.findByOuterIdAndBusinessId(outerId, businessId);
            if (!rExist.isSuccess()) {
                log.error("find shop by outerId({}) and businessId({}) fail,error:{}", outerId, businessId, rExist.getError());
                throw new JsonResponseException(rExist.getError());
            }
            Shop shop = rExist.getResult().get();
            ShopExtraInfo existShopExtraInfo = ShopExtraInfo.fromJson(shop.getExtra());
            ShopBusinessTime shopBusinessTime = existShopExtraInfo.getShopBusinessTime();

            //未设置营业相关字段则返回false
            if (shopBusinessTime == null){
                return false;
            }

            //如果营业状态为歇业返回 false
            if (shopBusinessTime.getOpeningStatus() == null
                    || !shopBusinessTime.getOpeningStatus().equals(ShopOpeningStatus.OPENING.value())){
                return false;
            }

            LocalDate localDate = LocalDate.now();
            DayOfWeek curDayOfWeek = localDate.getDayOfWeek();
            Map<DayOfWeek,String[]> weekTimeMap =  shopBusinessLogic.getWeekMap(shopBusinessTime);
            String[] strArr = weekTimeMap.get(curDayOfWeek);
            //当日营业状态为不营业 false
            if (strArr[0] == null || !strArr[0].equals(String.valueOf(ShopOpeningStatus.OPENING.value()))){
                return false;
            }

            //校验开始时间和结束时间是否正确
            if (!shopBusinessLogic.validTime(strArr[1]) || !shopBusinessLogic.validTime(strArr[2])){
                return false;
            }
            ShopExtraInfo shopExtraInfo = ShopExtraInfo.fromJson(shop.getExtra());
            if(Arguments.isNull(shopExtraInfo)){
                log.error("not find shop(id:{}) extra info by shop extra info json:{} ",shop.getId(),shop.getExtra());
                throw new ServiceException("shop.extra.info.invalid");
            }
            //校验是否到达最大接单量
            if(!Arguments.isNull(shopBusinessTime.getOrderAcceptQtyMax())){
                Response<Integer> countResp = orderShipmentReadService.countByShopId(shopExtraInfo.getOpenShopId());
                if (!countResp.isSuccess()) {
                    throw new JsonResponseException(countResp.getError());
                }
                if (shopBusinessTime.getOrderAcceptQtyMax() <= countResp.getResult()){
                    return false;
                }
            }
            //校验当前时间是否在开始时间和结束时间之间
            String[] startTimeStr = strArr[1].split(":");
            String[] endTimeStr = strArr[2].split(":");
            LocalTime startTime = LocalTime.of(Integer.parseInt(startTimeStr[0]),Integer.parseInt(startTimeStr[1]));
            LocalTime endTime = LocalTime.of(Integer.parseInt(endTimeStr[0]),Integer.parseInt(endTimeStr[1]));
            LocalTime curTime = LocalTime.now();
            if (curTime.compareTo(startTime) >= 0 && curTime.compareTo(endTime) <= 0){
                return true;
            }

            return false;

        } catch (Exception e) {
            log.error("get Expire time of order fail, outerId({}), businessId({}),error:{}",
                    outerId, businessId,e.getMessage());
            throw new JsonResponseException(e.getMessage());
        }
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


    private void syncParanaFrozenShop(String outerId,Long businessId){
        Response<Boolean> response =  syncParanaShopService.frozenShop(outerId,businessId);
        if(!response.isSuccess()){
            log.error("sync parana frozen shop(outerId:{}) fail,error:{}",outerId,response.getError());
            throw new JsonResponseException(response.getError());
        }
    }

    private void syncParanaUnFrozenShop(String outerId,Long businessId){
        Response<Boolean> response =  syncParanaShopService.unfrozenShop(outerId,businessId);
        if(!response.isSuccess()){
            log.error("sync parana unfrozen shop(outerId:{}) fail,error:{}",outerId,response.getError());
            throw new JsonResponseException(response.getError());
        }
    }

    private void syncParanaCloseShop(String outerId,Long businessId){
        Response<Boolean> response =  syncParanaShopService.closeShop(outerId,businessId);
        if(!response.isSuccess()){
            log.error("sync parana close shop(outerId:{}) fail,error:{}",outerId,response.getError());
            throw new JsonResponseException(response.getError());
        }
    }

    private void syncParanaShopPhone(String outerId,Long busienssId,String phone){
        Response<Boolean> response =  syncParanaShopService.syncShopPhone(outerId,busienssId,phone);
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

        private String zoneId;

        private String zoneName;

        //店铺内码
        private String storeId;

        //店铺邮箱
        private String email;
    }

    /**
     * @Description 初始化门店营业信息
     * @Date        2018/7/12
     * @param
     * @return
     */
    @ApiOperation("初始化门店营业信息")
    @RequestMapping(value = "/shop/business/info/init", method = RequestMethod.POST)
    public Response<Boolean> initBusinessInf(@RequestBody(required = false) List<Long> shopIds) {
        try {
            int pageNo =1;
            int size = 10000;
            List<Shop>  shopList = new ArrayList<>();

            if(shopIds!=null&&!shopIds.isEmpty()){
                shopList = shopReadService.findByIds(shopIds).getResult();
            }else {
                Response<Paging<Shop>> pagingRes = shopReadService.pagination(null, null, null, null, pageNo, size);
                shopList = pagingRes.getResult().getData();
            }
            shopList.forEach(shop -> {
                ShopExtraInfo shopExtraInfo = ShopExtraInfo.fromJson(shop.getExtra());
                if (!Arguments.isNull(shopExtraInfo)) {
                    ShopBusinessTime shopBusinessTime = shopExtraInfo.getShopBusinessTime();
                    if (shopBusinessTime == null) {
                        shopBusinessTime = new ShopBusinessTime();
                        setInitBusinessInfo(shopBusinessTime);
                        shopExtraInfo.setShopBusinessTime(shopBusinessTime);
                        shop.setExtra(ShopExtraInfo.putExtraInfo(shop.getExtra(), shopExtraInfo));
                        Response<Boolean> resp = shopWriteService.update(shop);
                        if (!resp.isSuccess()) {
                            log.error("update shop(shopId={}) business info failed, error={}", shop.getId(), resp.getError());
                            throw new JsonResponseException(500, resp.getError());
                        }
                        //刷新缓存
                        shopCacher.refreshShopById(shop.getId());
                        middleShopCacher.refreshByOuterIdAndBusinessId(shop.getOuterId(), shop.getBusinessId());
                    }
                }
            });
        } catch (Exception e) {
            log.error("update shop(shopId={}) business info failed, error={}",Throwables.getStackTraceAsString(e));
            return Response.fail(e.getMessage());
        }
        return Response.ok(true);
    }

    private void setInitBusinessInfo(ShopBusinessTime shopBusinessTime){
            shopBusinessTime.setOpeningStatus(ShopOpeningStatus.OPENING.value());
            shopBusinessTime.setOpeningStatusMon(ShopOpeningStatus.OPENING.value());
            shopBusinessTime.setOpeningStatusTue(ShopOpeningStatus.OPENING.value());
            shopBusinessTime.setOpeningStatusWed(ShopOpeningStatus.OPENING.value());
            shopBusinessTime.setOpeningStatusThu(ShopOpeningStatus.OPENING.value());
            shopBusinessTime.setOpeningStatusFri(ShopOpeningStatus.OPENING.value());
            shopBusinessTime.setOpeningStatusSat(ShopOpeningStatus.OPENING.value());
            shopBusinessTime.setOpeningStatusSun(ShopOpeningStatus.OPENING.value());
            shopBusinessTime.setOpeningStartTimeMon("00:00:00");
            shopBusinessTime.setOpeningEndTimeMon("23:59:59");
            shopBusinessTime.setOpeningStartTimeTue("00:00:00");
            shopBusinessTime.setOpeningEndTimeTue("23:59:59");
            shopBusinessTime.setOpeningStartTimeWed("00:00:00");
            shopBusinessTime.setOpeningEndTimeWed("23:59:59");
            shopBusinessTime.setOpeningStartTimeThu("00:00:00");
            shopBusinessTime.setOpeningEndTimeThu("23:59:59");
            shopBusinessTime.setOpeningStartTimeFri("00:00:00");
            shopBusinessTime.setOpeningEndTimeFri("23:59:59");
            shopBusinessTime.setOpeningStartTimeSat("00:00:00");
            shopBusinessTime.setOpeningEndTimeSat("23:59:59");
            shopBusinessTime.setOpeningStartTimeSun("00:00:00");
            shopBusinessTime.setOpeningEndTimeSun("23:59:59");
            shopBusinessTime.setOrderAcceptQtyMax(1000);
            shopBusinessTime.setOrderTimeout(90);
            shopBusinessTime.setOrderTimeout(120);
    }

    /**
     * 手动修复最大接单库存同步
     *
     * @param shopId
     * @return
     */
    @RequestMapping(value = "/max/order/fix/{shopId}")
    public Response<String> fixShopMaxOrderAsync(@PathVariable Long shopId) {
        try {
            //店铺是否存在
            Response<Shop> response = shopReadService.findById(shopId);
            if (!response.isSuccess()) {
                log.error("find shop by id:{} fail,error:{}", shopId, response.getError());
                throw new JsonResponseException(response.getError());
            }
            ShopExtraInfo shopExtraInfo = ShopExtraInfo.fromJson(response.getResult().getExtra());
            ShopBusinessTime shopBusinessTime = shopExtraInfo.getShopBusinessTime();
            //更新缓存中的超过最大接单量的标志位
            if (shopBusinessTime != null) {
                shopMaxOrderLogic.changeMaxOrderAcceptQtyFlag(shopBusinessTime.getOrderAcceptQtyMax(),
                    response.getResult());
            }
            return Response.ok("ok");

        } catch (Exception e) {
            log.error("failed to fix shop max order check", e);
            return Response.fail(e.getMessage());
        }
    }

}
