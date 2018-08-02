package com.pousheng.middle.web.shop;

import com.pousheng.middle.enums.OpenShopEnum;
import com.pousheng.middle.shop.dto.MemberShop;
import com.pousheng.middle.web.shop.component.MemberShopOperationLogic;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.open.client.common.shop.model.OpenShop;
import io.terminus.open.client.common.shop.service.OpenShopReadService;
import io.terminus.open.client.common.shop.service.OpenShopWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        if (Arguments.isNull(status)) {
            status = OpenShopEnum.enable_open_shop_enum.getIndex();
        }
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
        Response<Boolean> response = openShopWriteService.update(openShop);
        if (!response.isSuccess()) {
            log.error("update open shop failed, openShopId={}, error={}", openShopId, response.getError());
            throw new JsonResponseException(500, response.getError());
        }
        return response.getResult();
    }

    @ApiOperation("新增外部平台店铺")
    @RequestMapping(value = "/create", method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public Long createOpenShop(@RequestBody OpenShop openShop) {
        openShop.setStatus(OpenShopEnum.enable_open_shop_enum.getIndex());
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


    private OpenShop checkOpenShopNameIfDuplicated(String channel, String updatedOpenShopName) {
        Response<OpenShop> findShop = openShopReadService.findByChannelAndName(channel, updatedOpenShopName);
        if (!findShop.isSuccess()) {
            log.error("fail to check open shop if existed by channel:{}, name:{},cause:{}",
                    channel, updatedOpenShopName, findShop.getError());
            throw new JsonResponseException(findShop.getError());
        }
        return findShop.getResult();
    }
}
