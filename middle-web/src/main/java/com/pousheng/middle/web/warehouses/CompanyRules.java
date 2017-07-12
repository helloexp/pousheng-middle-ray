package com.pousheng.middle.web.warehouses;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.model.WarehouseCompanyRule;
import com.pousheng.middle.warehouse.service.WarehouseCompanyRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseCompanyRuleWriteService;
import com.pousheng.middle.web.warehouses.dto.Company;
import com.pousheng.middle.web.warehouses.dto.ErpShop;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-12
 */
@RestController
@RequestMapping("/api/warehouse/company")
@Slf4j
public class CompanyRules {

    public static final ObjectMapper mapper = JsonMapper.nonEmptyMapper().getMapper();

    @Value("pousheng.member.host")
    private String memberHost;

    @RpcConsumer
    private WarehouseCompanyRuleReadService warehouseCompanyRuleReadService;

    @RpcConsumer
    private WarehouseCompanyRuleWriteService warehouseCompanyRuleWriteService;

    /**
     * 列出当前未设置规则的公司
     *
     * @return 当前未设置规则的公司列表
     */
    @RequestMapping(value = "/todo", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Company> todo(){
        Map<String, Integer> params = ImmutableMap.of("pageNo",1, "pageSize", Integer.MAX_VALUE);
        HttpRequest r = HttpRequest.get(memberHost+"/api/member/pousheng/company/list", params, true)
                .acceptJson()
                .acceptCharset(HttpRequest.CHARSET_UTF8);
        if (r.ok()) {
            return todoCompanies(r.body());

        } else {
            int code = r.code();
            String body = r.body();
            log.error("failed to get company list (params:{}), http code:{}, message:{}",
                     params, code, body);
            throw new JsonResponseException("member.company.request.fail");
        }

    }

    @RequestMapping(value = "/erpShops", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ErpShop> erpShops(@RequestParam(value = "prefix", required = false) String namePrefix){
        Map<String, Object> params = Maps.newHashMap();
        params.put("pageNo", 1);
        params.put("pageSize", 10);
        if(StringUtils.hasText(namePrefix)){
            params.put("storeName", namePrefix);
        }
        HttpRequest r = HttpRequest.get(memberHost+"/api/member/pousheng/shop/list", params, true)
                .acceptJson()
                .acceptCharset(HttpRequest.CHARSET_UTF8);
        if (r.ok()) {
            String body = r.body();
            try {
                JsonNode root = mapper.readTree(body);
                return mapper.readValue(root.findPath("data").toString(), new TypeReference<List<ErpShop>>() {
                });
            }catch (Exception e){
                log.error("failed to deserialize shop list from member center, body:{}, cause:{} ",
                        body,  Throwables.getStackTraceAsString(e));
                throw new JsonResponseException("member.shop.request.fail");
            }

        } else {
            int code = r.code();
            String body = r.body();
            log.error("failed to get shop list (params:{}), http code:{}, message:{}",
                    params, code, body);
            throw new JsonResponseException("member.shop.request.fail");
        }
    }

    private List<Company> todoCompanies(String body) {
        try {
            JsonNode root = mapper.readTree(body);
            List<Company> all = mapper.readValue(root.findPath("data").toString(), new TypeReference<List<Company>>() {
            });

            //过滤掉已设置规则的公司
            Response<List<String>> rCompanyCodes = warehouseCompanyRuleReadService.findCompanyCodes();
            if(!rCompanyCodes.isSuccess()){
                log.error("failed to find company codes where rule set");
                throw new JsonResponseException("company.rule.request.fail");
            }
            Set<String> doneCompanyCodes = Sets.newHashSet(rCompanyCodes.getResult());
            List<Company> todoCompanies = Lists.newArrayList();
            for (Company company : all) {
                if(!doneCompanyCodes.contains(company.getCompanyId())){
                    todoCompanies.add(company);
                }
            }
            return todoCompanies;
        } catch (Exception e) {
            log.error("failed to find companies to set rules for {}, cause:{}", body, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("company.rule.request.fail");
        }
    }

    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long create(@RequestBody  WarehouseCompanyRule companyRule){
        Response<Long> r = warehouseCompanyRuleWriteService.create(companyRule);
        if(!r.isSuccess()){
            log.error("failed to create {}, error code:{}", companyRule, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }
}
