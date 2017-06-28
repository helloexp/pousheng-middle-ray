package com.pousheng.middle.web.warehouses.gateway;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.dto.PoushengWarehouse;
import com.pousheng.middle.warehouse.model.StockBill;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-26
 */
@Component
@Slf4j
public class StockClient {

    public static final ObjectMapper mapper = JsonMapper.nonEmptyMapper().getMapper();
    public static final DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss");
    public static final TypeReference<List<StockBill>> LIST_OF_STOCKBILL = new TypeReference<List<StockBill>>() {
    };
    public static final TypeReference<PoushengWarehouse> LIST_OF_WAREHOUSE = new TypeReference<PoushengWarehouse>() {
    };

    private final String host;

    private final String accessKey;

    @Autowired
    public StockClient(@Value("${gateway.stock.host}") String host,
                       @Value("${gateway.stock.accessKey}")String accessKey) {
        this.host = host;
        this.accessKey = accessKey;
    }

    public List<StockBill> stockBills(String path,
                                      DateTime start,
                                      DateTime end,
                                      Integer pageNo,
                                      Integer pageSize,
                                      Map<String, String> params){
        params.put("start_datetime", formatter.print(start));
        params.put("end_datetime", formatter.print(end));
        params.put("current_page", MoreObjects.firstNonNull(pageNo, 1).toString());
        params.put("page_size", MoreObjects.firstNonNull(pageSize,20).toString());
        HttpRequest r = HttpRequest.get(host+"/"+path, params, true)
                .header("access-key", accessKey)
                .acceptJson()
                .acceptCharset(HttpRequest.CHARSET_UTF8);
        if (r.ok()) {
            String result =  handleResponse(path, params, r.body());
            try {
                return mapper.readValue(result, LIST_OF_STOCKBILL);
            } catch (Exception e) {
                log.error("failed to deserialize json to StockBill list:{}, cause:{}",
                        result, Throwables.getStackTraceAsString(e));
                throw new ServiceException("stock.bill.synchronize.fail", e);
            }
        } else {
            int code = r.code();
            String body = r.body();
            log.error("failed to get (path={}, params:{}), http code:{}, message:{}",
                    path, params, code, body);
            throw new ServiceException("stock.bill.synchronize.fail");
        }
    }


    public List<PoushengWarehouse>  warehouses(DateTime start,
                                               DateTime end,
                                               Integer pageNo,
                                               Integer pageSize
                                               ){
        Map<String, String> params = Maps.newHashMap();
        if(start!=null){
            params.put("start_datetime", formatter.print(start));
        }
        if(end!=null){
            params.put("end_datetime", formatter.print(end));
        }
        if(pageNo!=null){
            params.put("current_page", pageNo.toString());
        }
        if(pageSize!=null){
            params.put("page_size", pageSize.toString());
        }
        HttpRequest r = HttpRequest.get(host+"/e-commerce-api/v1/hk-get-stocks", params, true)
                .header("access-key", accessKey)
                .acceptJson()
                .acceptCharset(HttpRequest.CHARSET_UTF8);
        if (r.ok()) {
            String result =  handleResponse(null,params, r.body());
            try {
                return mapper.readValue(result, LIST_OF_WAREHOUSE);
            } catch (Exception e) {
                log.error("failed to deserialize json to PoushengWarehouse list:{}, cause:{}",
                        result, Throwables.getStackTraceAsString(e));
                throw new ServiceException("warehouse.synchronize.fail", e);
            }
        } else {
            int code = r.code();
            String body = r.body();
            log.error("failed to get (params:{}), http code:{}, message:{}",
                    params, code, body);
            throw new ServiceException("warehouse.synchronize.fail");
        }
    }

    private String handleResponse(String path, Map<String, String> params, String body) {
        try {
            JsonNode root = mapper.readTree(body);
            boolean success = root.findPath("retCode").asInt() == 0;
            if (!success) {
                String errorCode = root.findPath("retMessage").textValue();
                log.error(errorCode);
                throw new ServiceException(errorCode);
            }
            return root.findPath("list").toString();
/*            return mapper.readValue(root.findPath("list").toString(),
                    LIST_OF_STOCKBILL);*/

        } catch (IOException e) {
            log.error("failed to get stock bills from (path={}, params:{}), cause:{}",
                    path, params, Throwables.getStackTraceAsString(e));
            throw new ServiceException(e);
        }
    }
}
