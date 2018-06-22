package com.pousheng.middle.web.express;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.cache.ExpressCompanyCacher;
import io.terminus.parana.express.model.ExpressCompany;
import io.terminus.parana.express.service.ExpressCompanyReadService;
import io.terminus.parana.express.service.ExpressCompanyWriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 *
 * Author:宋仁飞
 * Created on 5/27/16.
 */

@Api(description="快递公司API")
@RestController
@Slf4j
@RequestMapping("/api/express-companies")
public class ExpressCompanies {

    @RpcConsumer
    private ExpressCompanyReadService expressCompanyReadService;

    @RpcConsumer
    private ExpressCompanyWriteService expressCompanyWriteService;

    @Autowired
    private ExpressCompanyCacher expressCompanyCacher;

    @ApiOperation("列出全部快递公司信息")
    @RequestMapping(value = "/list", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ExpressCompany> listActiveCompanies() {
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-COMPANIES-LIST-START noparam: ");
        }
        List<ExpressCompany> companies = expressCompanyCacher.listAllActiveExpressCompanies();
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-COMPANIES-LIST-END noparam: ,resp: [{}]",JsonMapper.nonEmptyMapper().toJson(companies));
        }
        return companies;
    }

    @ApiOperation("根据快递公司名称搜索")
    @RequestMapping(value = "/suggest", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ExpressCompany> findCompanyByFuzzyName(@RequestParam(value = "fuzzyName") String fuzzyName) {
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-COMPANIES-SUGGEST-START param: fuzzyName [{}]",fuzzyName);
        }
        Response<List<ExpressCompany>> response = expressCompanyReadService.findExpressCompanyByFuzzyName(fuzzyName);
        if (!response.isSuccess()) {
            log.error("fail to find express company by fuzzy name:{} cause:{}", fuzzyName, response.getError());
            throw new JsonResponseException(500, response.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-COMPANIES-SUGGEST-END param: fuzzyName [{}] ,resp: [{}]",fuzzyName,JsonMapper.nonEmptyMapper().toJson(response.getResult()));
        }
        return response.getResult();
    }

    /**
     *
     *
     * @param express    快递公司信息
     * @return 成功返回记录的id
     */
    @ApiOperation("创建快递公司")
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long create(@RequestBody ExpressCompany express) throws JsonResponseException {
        String expressStr = JsonMapper.nonEmptyMapper().toJson(express);
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-COMPANIES-CREATE-START param: express [{}]",expressStr);
        }
        if (!StringUtils.hasText(express.getCode()) || !StringUtils.hasText(express.getName())) {
            log.error("express company code or name cannot be blank, got:{}", express);
            throw new JsonResponseException("express.info.uncompleted");
        }

        Response<Long> tryCreate = expressCompanyWriteService.create(express);
        if (!tryCreate.isSuccess()) {
            log.error("fail to create express company:{}, cause:{}", express, tryCreate.getError());
            throw new JsonResponseException(tryCreate.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-COMPANIES-CREATE-END param: express [{}] ,resp:[{}]",expressStr,tryCreate.getResult());
        }
        return tryCreate.getResult();
    }

    /**
     * 更新快递公司
     *
     * @param express    快递公司信息
     * @return 成功返回记录的id
     */
    @ApiOperation("更新快递公司")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long update(@PathVariable Long id, @RequestBody ExpressCompany express) {
        String expressStr = JsonMapper.nonEmptyMapper().toJson(express);
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-COMPANIES-UPDATE-START param: express [{}]",expressStr);
        }
        if (!StringUtils.hasText(express.getCode()) || !StringUtils.hasText(express.getName())) {
            log.error("express company code or name cannot be blank, got:{}", express);
            throw new JsonResponseException("express.info.uncompleted");
        }

        express.setId(id);
        Long update =  doUpdate(express);
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-COMPANIES-UPDATE-END param: express [{}] ,resp: [{}]",expressStr,update);
        }
        return update;
    }

    /**
     * 删除快递公司
     *
     * @param id    快递公司记录的id
     * @return 成功返回记录的id
     */
    @ApiOperation("删除快递公司")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long delete(@PathVariable Long id) {
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-COMPANIES-DELETE-START param: id [{}]",id);
        }
        Response<Long> tryDelete = expressCompanyWriteService.delete(id);
        if (!tryDelete.isSuccess()) {
            log.error("fail to delete express company by id:{}, cause:{}", id, tryDelete.getError());
            throw new JsonResponseException(tryDelete.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-COMPANIES-DELETE-END param: id [{}] ,resp: [{}]",id,tryDelete.getResult());
        }
        return tryDelete.getResult();
    }

    /**
     * 启用快递公司
     *
     * @param id    快递公司记录的id
     * @return 成功返回记录的id
     */
    @ApiOperation("启用快递公司")
    @RequestMapping(value = "/{id}/enable", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long enable(@PathVariable Long id) {
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-COMPANIES-ENABLE-START param: id [{}]",id);
        }
        ExpressCompany update = new ExpressCompany();
        update.setId(id);
        update.setStatus(ExpressCompany.Status.ACTIVE.value());
        Long num =  doUpdate(update);
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-COMPANIES-ENABLE-END param: id [{}] ,resp: [{}]",id,num);
        }
        return num;
    }

    /**
     * 禁用快递公司
     *
     * @param id    快递公司记录的id
     * @return 成功返回记录的id
     */
    @ApiOperation("禁用快递公司")
    @RequestMapping(value = "/{id}/disable", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    public Long disable(@PathVariable Long id) {
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-COMPANIES-DISABLE-START param: id [{}]",id);
        }
        ExpressCompany update = new ExpressCompany();
        update.setId(id);
        update.setStatus(ExpressCompany.Status.STOP.value());
        Long num =doUpdate(update);
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-COMPANIES-DISABLE-END param: id [{}] ,resp: [{}]",id,num);
        }
        return num;
    }

    /**
     * 更新快递公司
     *
     * @param update    快递公司信息
     * @return 成功返回记录id
     */
    private Long doUpdate(ExpressCompany update) {
        Response<Long> tryUpdate = expressCompanyWriteService.update(update);
        if (!tryUpdate.isSuccess()) {
            log.error("fail to update express company:{}, cause:{}", update, tryUpdate.getError());
            throw new JsonResponseException(tryUpdate.getError());
        }

        return tryUpdate.getResult();
    }
}
