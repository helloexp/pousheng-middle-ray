package com.pousheng.middle.web.express;

import com.pousheng.middle.order.dto.ExpressCodeCriteria;
import com.pousheng.middle.order.model.ExpressCode;
import com.pousheng.middle.order.service.ExpressCodeReadService;
import com.pousheng.middle.order.service.ExpressCodeWriteService;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created by tony on 2017/6/28.
 */
@RestController
@RequestMapping("/api/express")
@Slf4j
public class ExpressCodes {
    @RpcConsumer
    private ExpressCodeWriteService expressCodeWriteService;
    @RpcConsumer
    private ExpressCodeReadService expressCodeReadService;

    @RequestMapping(method = RequestMethod.POST,produces = MediaType.APPLICATION_JSON_VALUE)
    public Long create(@RequestBody ExpressCode expressCode)
    {
      String expressStr = JsonMapper.nonEmptyMapper().toJson(expressCode);
      if(log.isDebugEnabled()){
         log.debug("API-EXPRESS-CREATE-START param: expressCode [{}]",expressStr);
      }
      Response<Long> r =  expressCodeWriteService.create(expressCode);
        if (!r.isSuccess()) {
            log.error("failed to batchCreate {}, error code:{}", expressCode, r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-CREATE-END param: expressCode [{}] ,resp: [{}]",expressStr,r.getResult());
        }
      return r.getResult();
    }
    @RequestMapping(method = RequestMethod.PUT,produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean update(@RequestBody ExpressCode expressCode)
    {
        String expressStr = JsonMapper.nonEmptyMapper().toJson(expressCode);
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-UPDATE-START param: expressCode [{}]",expressStr);
        }
        Response<Boolean> r = expressCodeWriteService.update(expressCode);
        if (!r.isSuccess()){
            log.error("failed to update {}, error code:{}", expressCode, r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-UPDATE-END param: expressCode [{}] ,resp: [{}]",expressStr,r.getResult());
        }
        return r.getResult();
    }

    @RequestMapping(value = "/{id}",method = RequestMethod.DELETE,produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean delete(@PathVariable("id")Long id){
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-DELETE-START param: id [{}]",id);
        }
        Response<Boolean> r = expressCodeWriteService.deleteById(id);
        if (!r.isSuccess()) {
            log.error("failed to delete expressCode(id={}), error code:{} ",
                    id, r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-DELETE-END param: id [{}] ,resp: [{}]",id,r.getResult());
        }
        return r.getResult();
    }
    @RequestMapping(value = "/paging",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public Paging<ExpressCode> pagination(@RequestParam(required = false, value = "pageNo") Integer pageNo,
                                                                          @RequestParam(required = false, value = "pageSize") Integer pageSize,
                                                                          @RequestParam(required = false, value = "name") String name
                                                                          )
    {
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-PAGING-START param: pageNo [{}] pageSize [{}] name [{}]",pageNo,pageSize,name);
        }
        ExpressCodeCriteria criteria = new ExpressCodeCriteria();
        if(StringUtils.isNoneEmpty(name))
        {
            criteria.setName(name);
        }
        if (pageNo!=null){
            criteria.setPageNo(pageNo);
        }
        if (pageSize!=null){
            criteria.setPageSize(pageSize);
        }
        Response<Paging<ExpressCode>> r = expressCodeReadService.pagingExpressCode(criteria);

        if(!r.isSuccess()){
            log.error("failed to pagination expressCode with criteria:{}, error code:{}", criteria, r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-PAGING-END param: pageNo [{}] pageSize [{}] name [{}] ,resp: [{}]",pageNo,pageSize,name,JsonMapper.nonEmptyMapper().toJson(r.getResult()));
        }
        return r.getResult();
    }
    @RequestMapping(value = "/{id}",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public ExpressCode findById(@PathVariable("id") Long id)
    {
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-FINDBYID-START param: id [{}]",id);
        }
        Response<ExpressCode> r = expressCodeReadService.findById(id);
        if (!r.isSuccess()) {
            log.error("failed to delete expressCode(id={}), error code:{} ",
                    id, r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-FINDBYID-END param: id [{}] ,resp: [{}]",id,JsonMapper.nonEmptyMapper().toJson(r.getResult()));
        }
        return r.getResult();
    }

    @RequestMapping(value = "all",method = RequestMethod.GET,produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ExpressCode> findAll(@RequestParam(required = false) String name)
    {
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-ALL-START param: name [{}]",name);
        }
        if (StringUtils.isEmpty(name)){
            name="";
        }
        Response<List<ExpressCode>> r = expressCodeReadService.findAllByName(name);
        if (!r.isSuccess()) {
            log.error("find all express code failed, error code:{} "
                    , r.getError());
            throw new JsonResponseException(r.getError());
        }
        if(log.isDebugEnabled()){
            log.debug("API-EXPRESS-ALL-END param: name [{}] ,resp: [{}]",name,JsonMapper.nonEmptyMapper().toJson(r.getResult()));
        }
        return r.getResult();
    }
}
