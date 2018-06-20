package com.pousheng.middle.open.yunding;

import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.open.client.common.OpenClientException;
import io.terminus.open.client.common.mappings.model.ItemMapping;
import io.terminus.open.client.common.mappings.service.MappingReadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Author:  <a href="mailto:zhaoxiaotao@terminus.io">tony</a>
 * Date: 2018/5/17
 * pousheng-middle
 */
@Component
@Slf4j
public class JdYunDingSyncStockLogic {
    @Value("${jd.yunding.app.key}")
    private String appKey;
    @Value("${jd.yunding.secret}")
    private String secret;
    @Value("${jd.yunding.gateway}")
    private String gateway;
    @RpcConsumer
    private MappingReadService mappingReadService;

    public Response<Boolean> syncJdYundingStock(Long shopId,String skuCode, Integer stock) {
        try {
            //查找映射关系
            Response<Optional<ItemMapping>> findItemMapping = mappingReadService.findBySkuCodeAndOpenShopId(skuCode, shopId);
            if (!findItemMapping.isSuccess()) {
                log.error("fail to find item mapping by skuCode={},openShopId={},cause:{}",
                        skuCode, shopId, findItemMapping.getError());
                return Response.fail(findItemMapping.getError());
            }
            Optional<ItemMapping> itemMappingOptional = findItemMapping.getResult();

            if (!itemMappingOptional.isPresent()) {
                log.error("item mapping not found by skuCode={},openShopId={},cause:{}",
                        skuCode, shopId, findItemMapping.getError());
                return Response.fail("item.mapping.not.found");
            }

            ItemMapping itemMapping = itemMappingOptional.get();
            if (!StringUtils.hasText(itemMapping.getChannelItemId()) ||
                    !StringUtils.hasText(itemMapping.getChannelSkuId())) {
                log.error("open item id or open sku id not sync,and skip to update sku(code={}) stock to {} for shop(id={})",
                        skuCode, stock, shopId);
                return Response.fail("open.item.id.or.sku.id.not.sync");
            }
            //推送库存
            HashMap params = Maps.newHashMap();
            params.put("shopId", shopId);
            params.put("openItemId", itemMapping.getChannelItemId());
            params.put("skuCode", skuCode);
            params.put("openSkuId", itemMapping.getChannelSkuId());
            params.put("stock", stock);
            String result = this.post(shopId, "jd.stock.push.api", params);
            if (!result.contains("success")) {
                log.error("order by ship by params:{} fail, request result:{}", params, result);
                return Response.fail(result);
            }
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("push jd order failed, caused by {}", Throwables.getStackTraceAsString(e));
            return Response.fail("push.jd.order.failed");
        }
    }

    /**
     * 更新京东云鼎订单金额
     * @param shopId 店铺id
     * @param outerOrderId 京东订单号
     * @return
     */
    public Response<Boolean> syncUpdateJdOrderAmount(Long shopId,String outerOrderId){
        try{
            HashMap params = Maps.newHashMap();
            params.put("shopId", shopId);
            params.put("outOrderId", outerOrderId);
            String result = this.post(shopId, "jd.yunding.order.amount.push.api", params);
            if (!result.contains("success")) {
                log.error("sync update jd order amount  by params:{} fail, request result:{}", params, result);
                return Response.fail(result);
            }
            return Response.ok(Boolean.TRUE);
        }catch (Exception e){
            log.error("sync update jd order amount failed,caused by {}",Throwables.getStackTraceAsString(e));
            return Response.fail("sync update jd order amount failed");
        }
    }

    /**
     * @param shopId
     * @param method
     * @param requestParams
     * @return
     */
    public String post(Long shopId, String method, Map<String, Object> requestParams) {

        Map<String, Object> params = this.handleRequestParams(appKey, secret, method, requestParams);
        return this.post(gateway, params);
    }

    public String post(String url, Map<String, Object> params) {
        HttpRequest request = HttpRequest.post(url).connectTimeout(1000000).readTimeout(1000000).form(params);
        if (!request.ok()) {
            log.error("post request url:{} params:{} fail,response result:{}", new Object[]{url, params, request.body()});
            throw new ServiceException("request.fail");
        } else {
            String result = request.body();
            log.debug("post request url:{} result:{}", url, result);
            return result;
        }
    }

    private Map<String, Object> handleRequestParams(String appKey, String secret, String method, Map<String, Object> requestParams) {
        Map<String, Object> params = Maps.newTreeMap();
        params.put("appKey", appKey);
        params.put("pampasCall", method);
        params.putAll(requestParams);
        String sign = this.sign(secret, params);
        params.put("sign", sign);
        return params;
    }

    private String sign(String secret, Map<String, Object> params) {
        try {
            String toVerify = Joiner.on('&').withKeyValueSeparator("=").join(params);
            return Hashing.md5().newHasher().putString(toVerify, Charsets.UTF_8).putString(secret, Charsets.UTF_8).hash().toString();
        } catch (Exception var4) {
            log.error("call parana open api sign fail, params:{},cause:{}", params, Throwables.getStackTraceAsString(var4));
            throw new OpenClientException(500, "parana.sign.fail");
        }
    }

}
