package com.pousheng.middle.web.shop;

import com.pousheng.middle.enums.GateWayEnum;
import com.pousheng.middle.enums.OpenShopEnum;
import com.pousheng.middle.order.constant.TradeConstants;
import com.pousheng.middle.order.enums.MiddleChannel;
import com.pousheng.middle.shop.dto.MemberShop;
import com.pousheng.middle.web.shop.component.MemberShopOperationLogic;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.open.client.center.shop.OpenShopCacher;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.common.shop.service.OpenShopWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @Description: TODO
 * @author: yjc
 * @date: 2018/8/2下午2:50
 */
@Api(description = "外部门店API")
@RestController
@Slf4j
@RequestMapping("/api/openShop")
public class AdminOpenShops {

    @RpcConsumer
    private OpenShopReadService openShopReadService;
    @RpcConsumer
    private OpenShopWriteService openShopWriteService;
    @Autowired
    private MemberShopOperationLogic memberShopOperationLogic;
    @Autowired
    private OpenShopCacher openShopCacher;

    /**
     *
     * @param shopName 外部平台店铺名称
     * @param channel  渠道名称
     * @param status   状态
     * @param pageNo   页码
     * @param pageSize 页大小
     * @return         外部店铺信息
     */
    @ApiOperation("分页查询外部平台店铺信息")
    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<OpenShop> paging(@RequestParam(required = false) String shopName,
                                   @RequestParam(required = false) String channel,
                                   @RequestParam(required = false) Integer status,
                                   @RequestParam(required = false) Integer pageNo,
                                   @RequestParam(required = false) Integer pageSize) {

        Response<Paging<OpenShop>> resp = openShopReadService.pagination(shopName, channel, status, pageNo, pageSize);
        if (!resp.isSuccess()) {
            throw new JsonResponseException(resp.getError());
        }
        return resp.getResult();
    }


    @ApiOperation("根据ID查询平台店铺信息")
    @RequestMapping(value = "", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public OpenShop findOpenShop(@RequestParam(required = false) Long openShopId) {
        if (Arguments.isNull(openShopId)) {
            return null;
        }
        Response<OpenShop> rOpenShop = openShopReadService.findById(openShopId);
        if (!rOpenShop.isSuccess()) {
            log.error("find open shop by openShopId:{} fail,error:{}",openShopId,rOpenShop.getError());
            throw new JsonResponseException(rOpenShop.getError());
        }
        return rOpenShop.getResult();
    }

    @ApiOperation("更新外部平台店铺信息")
    @RequestMapping(value = "/update/{openShopId}", method = RequestMethod.PUT)
    public Boolean updateOpenShop(@PathVariable Long openShopId, @RequestBody OpenShop openShop) {
        Response<OpenShop> openShopResponse = openShopReadService.findById(openShopId);
        if (!openShopResponse.isSuccess()) {
            log.error("fail to find open shop, shop id:{},cause:{}", openShopId, openShopResponse.getError());
            throw new JsonResponseException(openShopResponse.getError());
        }
        openShop.setId(openShopId);
        // 变更json信息,不覆盖旧数据
        editExtraInfo(openShop, openShopResponse.getResult().getExtra());
        Response<Boolean> response = openShopWriteService.update(openShop);
        if (!response.isSuccess()) {
            log.error("update open shop failed, openShopId={}, error={}", openShopId, response.getError());
            throw new JsonResponseException(500, response.getError());
        }
        // 刷新缓存
        openShopCacher.refreshById(openShopId);
        return response.getResult();
    }

    @ApiOperation("新增外部平台店铺")
    @RequestMapping(value = "/create", method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Long createOpenShop(@RequestBody OpenShop openShop) {
        if (!openShop.getChannel().startsWith("yunju")) {
            openShop.setStatus(OpenShopEnum.enable_open_shop_enum.getIndex());
        }
        // 渠道对应的json信息特殊处理下
        addExtraInfo(openShop);
        Response<Long> response = openShopWriteService.create(openShop);
        if (!response.isSuccess()) {
            log.error("add open shop failed, error:{}",response.getError());
            throw new JsonResponseException("add.open.shop.fail");
        }
        return response.getResult();
    }

    @ApiOperation("删除外部平台店铺")
    @RequestMapping(value = "/delete/{openShopId}", method = RequestMethod.DELETE)
    public Boolean deleteOpenShop(@PathVariable Long openShopId) {
        Response<OpenShop> rOpenShop = openShopReadService.findById(openShopId);
        if (!rOpenShop.isSuccess()) {
            log.error("find open shop by openShopId:{} fail,error:{}",openShopId,rOpenShop.getError());
            throw new JsonResponseException(rOpenShop.getError());
        }
        OpenShop openShop = rOpenShop.getResult();
        // 状态改为禁用
        openShop.setStatus(OpenShopEnum.disable_open_shop_enum.getIndex());
        Response<Boolean> resp = openShopWriteService.update(openShop);
        if (!resp.isSuccess()) {
            log.error("delete open shop failed, openShopId:{}, error:{}", openShopId, resp.getError());
            throw new JsonResponseException(500, resp.getError());
        }
        // 刷新缓存
        openShopCacher.refreshById(openShopId);
        return resp.getResult();
    }

    @ApiOperation("校验外部平台店铺名称重复")
    @RequestMapping(value = "/checkShopName", method = RequestMethod.GET)
    public Boolean checkOpenShopName(@RequestParam(required = false) String channel,
                                     @RequestParam(required = false) String shopName) {
        OpenShop openShop = checkOpenShopNameIfDuplicated(channel, shopName);
        if (Arguments.isNull(openShop)) {
            return true;
        }
        log.info("open shop name has exist");
        return false;
    }

    @ApiOperation("绩效店铺外码查询信息")
    @RequestMapping(value = "/shopCode/{code}", method = RequestMethod.GET)
    public List<MemberShop> findByShopCode(@PathVariable String code) {
        List<MemberShop> memberShopList = memberShopOperationLogic.findShops(code);
        return memberShopList;
    }

    @ApiOperation("默认渠道gateway")
    @RequestMapping(value = "/shopCode/gateway/{channel}", method = RequestMethod.GET)
    public String findShopGateWay(@PathVariable String channel) {
        return GateWayEnum.get(channel).getGateway();
    }


    private OpenShop checkOpenShopNameIfDuplicated(String channel, String updatedOpenShopName) {
        Response<OpenShop> findShop = openShopReadService.findByChannelAndName(channel, updatedOpenShopName);
        if (!findShop.isSuccess()) {
            log.error("fail to check open shop if existed by channel:{}, name:{},cause:{}",
                    channel, updatedOpenShopName, findShop.getError());
            throw new JsonResponseException(findShop.getError());
        }
        return findShop.getResult();
    }


    private void addExtraInfo (OpenShop openShop) {
        Map<String, String> jsonMap = openShop.getExtra();
        // 云聚类型订单
        if(Objects.equals(MiddleChannel.YUNJUBBC.getValue(), openShop.getChannel())) {
            jsonMap.put("isCareStock","0");
            jsonMap.put("isOrderInsertMiddle","true");
        }
        // 天猫渠道淘宝C店
        if(Objects.equals(MiddleChannel.TAOBAO.getValue(), openShop.getChannel())) {
            if (jsonMap.containsKey(TradeConstants.IS_TAOBAO_SHOP)) {
                // 拉取映射关系
                jsonMap.put(TradeConstants.IS_TAOBAO_SHOP, jsonMap.get(TradeConstants.IS_TAOBAO_SHOP));
            }
            if (jsonMap.containsKey(TradeConstants.EXCHANGE_PULL)) {
                // 换货拉取
                jsonMap.put(TradeConstants.EXCHANGE_PULL, jsonMap.get(TradeConstants.EXCHANGE_PULL));
            }
        }
        openShop.setExtra(jsonMap);
    }

    /**
     * 修改json信息
     * @param openShop 前端传递修改后的信息
     * @param map 修改前json信息
     */
    private void editExtraInfo (OpenShop openShop, Map<String, String> map) {
        Map<String, String> jsonMap = openShop.getExtra();
        if (jsonMap.containsKey(TradeConstants.HK_PERFORMANCE_SHOP_OUT_CODE)) {
            // hk绩效店铺外码
            map.put(TradeConstants.HK_PERFORMANCE_SHOP_OUT_CODE, jsonMap.get(TradeConstants.HK_PERFORMANCE_SHOP_OUT_CODE));
        }
        if (jsonMap.containsKey(TradeConstants.HK_PERFORMANCE_SHOP_CODE)) {
            // hk绩效店铺代码
            map.put(TradeConstants.HK_PERFORMANCE_SHOP_CODE, jsonMap.get(TradeConstants.HK_PERFORMANCE_SHOP_CODE));
        }
        if (jsonMap.containsKey(TradeConstants.HK_PERFORMANCE_SHOP_NAME)) {
            // hk绩效店铺名称
            map.put(TradeConstants.HK_PERFORMANCE_SHOP_NAME, jsonMap.get(TradeConstants.HK_PERFORMANCE_SHOP_NAME));
        }
        if (jsonMap.containsKey(TradeConstants.HK_COMPANY_CODE)) {
            // 公司代码(账套)
            map.put(TradeConstants.HK_COMPANY_CODE, jsonMap.get(TradeConstants.HK_COMPANY_CODE));
        }
        if (jsonMap.containsKey(TradeConstants.ERP_SYNC_TYPE)) {
            // erp
            map.put(TradeConstants.ERP_SYNC_TYPE, jsonMap.get(TradeConstants.ERP_SYNC_TYPE));
        }
        if (jsonMap.containsKey(TradeConstants.ITEM_MAPPING_STOCK)) {
            // 拉取映射关系
            map.put(TradeConstants.ITEM_MAPPING_STOCK, jsonMap.get(TradeConstants.ITEM_MAPPING_STOCK));
        }
        if(Objects.equals(MiddleChannel.TAOBAO.getValue(), openShop.getChannel())) {
            if (jsonMap.containsKey(TradeConstants.IS_TAOBAO_SHOP)) {
                // 拉取映射关系
                map.put(TradeConstants.IS_TAOBAO_SHOP, jsonMap.get(TradeConstants.IS_TAOBAO_SHOP));
            }
        }
        if (jsonMap.containsKey(TradeConstants.EXCHANGE_PULL)) {
            //第三方换货拉取标记
            map.put(TradeConstants.EXCHANGE_PULL, jsonMap.get(TradeConstants.EXCHANGE_PULL));
        }
        if (jsonMap.containsKey(TradeConstants.IS_NEW_DISPATCH_ORDER_LOGIC)) {
            //派单规则：同公司账套优先/优先发货优先
            map.put(TradeConstants.IS_NEW_DISPATCH_ORDER_LOGIC, jsonMap.get(TradeConstants.IS_NEW_DISPATCH_ORDER_LOGIC));
        }
        if (jsonMap.containsKey(TradeConstants.MANUAL_SHIPMENT_CHECK_WAREHOUSE_FLAG)) {
            //手动派单只限默认发货仓：是/否
            map.put(TradeConstants.MANUAL_SHIPMENT_CHECK_WAREHOUSE_FLAG, jsonMap.get(TradeConstants.MANUAL_SHIPMENT_CHECK_WAREHOUSE_FLAG));
        }
        // 云聚类型订单
        if(Objects.equals(MiddleChannel.YUNJUBBC.getValue(), openShop.getChannel())) {
            jsonMap.put("isCareStock","0");
            jsonMap.put("isOrderInsertMiddle","true");
        }
        if(jsonMap.containsKey(TradeConstants.PULL_REFUND_EXCHANGE_FLAG_KEY)){
            map.put(TradeConstants.PULL_REFUND_EXCHANGE_FLAG_KEY, jsonMap.get(TradeConstants.PULL_REFUND_EXCHANGE_FLAG_KEY));
        }
        openShop.setExtra(map);
    }
}
