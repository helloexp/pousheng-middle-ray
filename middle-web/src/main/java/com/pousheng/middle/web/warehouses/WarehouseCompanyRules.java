package com.pousheng.middle.web.warehouses;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.pousheng.middle.warehouse.cache.WarehouseCacher;
import com.pousheng.middle.warehouse.model.Warehouse;
import com.pousheng.middle.warehouse.model.WarehouseCompanyRule;
import com.pousheng.middle.warehouse.service.WarehouseCompanyRuleReadService;
import com.pousheng.middle.warehouse.service.WarehouseCompanyRuleWriteService;
import com.pousheng.middle.web.warehouses.dto.Company;
import com.pousheng.middle.web.warehouses.dto.ErpShop;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-21
 */
@RestController
@RequestMapping("/api/warehouse/company-rule")
@Slf4j
public class WarehouseCompanyRules {

    public static final ObjectMapper mapper = JsonMapper.nonEmptyMapper().getMapper();

    @Value("${gateway.member.host}")
    private String memberHost;

    @RpcConsumer
    private WarehouseCompanyRuleReadService warehouseCompanyRuleReadService;

    @RpcConsumer
    private WarehouseCompanyRuleWriteService warehouseCompanyRuleWriteService;

    @Autowired
    private WarehouseCacher warehouseCacher;


    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long create(@RequestBody WarehouseCompanyRule warehouseCompanyRule){
        Long warehouseId = warehouseCompanyRule.getWarehouseId();
        Warehouse warehouse = warehouseCacher.findById(warehouseId);
        if(!Objects.equal(warehouse.getCompanyCode(), warehouseCompanyRule.getCompanyCode())){
            log.error("company code mismatch, expect: {}, actual:{}",warehouseCompanyRule.getCompanyCode(),
                    warehouse.getCompanyCode() );
            throw new JsonResponseException("company.code.mismatch");
        }
        Response<Long> r = warehouseCompanyRuleWriteService.create(warehouseCompanyRule);
        if(!r.isSuccess()){
            log.error("failed to create {}, error code:{}", warehouseCompanyRule, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    @RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean update(@RequestBody WarehouseCompanyRule warehouseCompanyRule){
        Long warehouseId = warehouseCompanyRule.getWarehouseId();
        Warehouse warehouse = warehouseCacher.findById(warehouseId);
        if(!Objects.equal(warehouse.getCompanyCode(), warehouseCompanyRule.getCompanyCode())){
            log.error("company code mismatch, expect: {}, actual:{}",warehouseCompanyRule.getCompanyCode(),
                    warehouse.getCompanyCode() );
            throw new JsonResponseException("company.code.mismatch");
        }
        Response<Boolean> r = warehouseCompanyRuleWriteService.update(warehouseCompanyRule);
        if(!r.isSuccess()){
            log.error("failed to update {}, error code:{}", warehouseCompanyRule, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean delete(@PathVariable Long id){
        Response<Boolean> r = warehouseCompanyRuleWriteService.deleteById(id);
        if(!r.isSuccess()){
            log.error("failed to delete WarehouseCompanyRule(id={}), error code:{}", id, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public WarehouseCompanyRule findById(@PathVariable Long id){
        Response<WarehouseCompanyRule> r = warehouseCompanyRuleReadService.findById(id);
        if(!r.isSuccess()){
            log.error("failed to find WarehouseCompanyRule(id={}), error code:{}", id, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    @RequestMapping(value = "/paging",method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<WarehouseCompanyRule> pagination(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                                   @RequestParam(required = false, value = "pageSize") Integer pageSize,
                                                   @RequestParam(required = false, value="companyCode")String companyCode){
        Map<String, Object> params = Maps.newHashMapWithExpectedSize(3);
        if(StringUtils.hasText(companyCode)){
            params.put("companyCode", companyCode);
        }
        Response<Paging<WarehouseCompanyRule>> r = warehouseCompanyRuleReadService.pagination(pageNo, pageSize, params);
        if(!r.isSuccess()){
            log.error("failed to pagination WarehouseCompanyRule, params:{}, error code:{}", params, r.getError());
            throw new JsonResponseException(r.getError());
        }
        return r.getResult();
    }

    /**
     * 列出当前未设置规则的公司
     *
     * @return 当前未设置规则的公司列表
     */
    @RequestMapping(value = "/company-candidate", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Company> companyCandidate(){
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
        params.put("storeType", 1);
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


}
