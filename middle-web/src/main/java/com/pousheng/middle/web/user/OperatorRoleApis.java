package com.pousheng.middle.web.user;

import com.google.common.collect.Lists;
import io.swagger.annotations.ApiOperation;
import io.terminus.applog.annotation.LogMe;
import io.terminus.applog.annotation.LogMeContext;
import io.terminus.applog.annotation.LogMeId;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.JsonMapper;
import io.terminus.parana.auth.model.OperatorRole;
import io.terminus.parana.auth.service.OperatorRoleReadService;
import io.terminus.parana.auth.service.OperatorRoleWriteService;
import io.terminus.parana.common.utils.RespHelper;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

import static io.terminus.parana.common.utils.RespHelper.or500;

/**
 * @author Effet
 */
@Slf4j
@RestController
@RequestMapping("/api/operator/role")
public class OperatorRoleApis {

    @RpcConsumer
    private OperatorRoleReadService operatorRoleReadService;

    @RpcConsumer
    private OperatorRoleWriteService operatorRoleWriteService;

    /**
     * 创建运营角色
     *
     * @param role 运营角色
     * @return 角色主键 ID
     */
    @ApiOperation("创建运营角")
    @LogMe(description = "创建运营角", compareTo = "operatorRoleDao#findById")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public Long createRole(@RequestBody @LogMeContext  OperatorRole role) {
        String roleStr = JsonMapper.nonEmptyMapper().toJson(role);
        if(log.isDebugEnabled()){
            log.debug("API-OPERATOR-CREATEROLE-START param: role [{}]",roleStr);
        }
        role.setStatus(1);
        Response<Long> resp = operatorRoleWriteService.createRole(role);
        if(log.isDebugEnabled()){
            log.debug("API-OPERATOR-CREATEROLE-END param: role [{}] ,resp: [{}]",roleStr,resp.getResult());
        }
        return or500(resp);
    }

    /**
     * 更新运营角色
     *
     * @param id   角色 ID
     * @param role 角色授权内容
     * @return 是否更新成功
     */
    @ApiOperation("更新运营角色")
    @LogMe(description = "更新运营角色", compareTo = "operatorRoleDao#findById")
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public Boolean updateRole(@PathVariable Long id, @RequestBody OperatorRole role) {
        String roleStr = JsonMapper.nonEmptyMapper().toJson(role);
        if(log.isDebugEnabled()){
            log.debug("API-OPERATOR-UPDATEROLE-START param: id [{}] role [{}]",id,roleStr);
        }
        OperatorRole existRole = RespHelper.orServEx(operatorRoleReadService.findById(id));
        if (existRole == null) {
            throw new JsonResponseException(500, "operator.role.not.exist");
        }
        role.setId(id);
        role.setStatus(null); // prevent update
        Response<Boolean> resp = operatorRoleWriteService.updateRole(role);
        if(log.isDebugEnabled()){
            log.debug("API-OPERATOR-UPDATEROLE-END param: id [{}] role [{}] ,resp: [{}]",id,roleStr,resp.getResult());
        }
        return or500(resp);
    }

    @LogMe(description = "删除运营角", deleting = "operatorRoleDao#findById")
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
        public Boolean deleteRole(@PathVariable @LogMeId Long id) {
        if(log.isDebugEnabled()){
            log.debug("API-OPERATOR-DELETEROLE-START param: id [{}]",id);
        }
        OperatorRole toUpdate = new OperatorRole();
        toUpdate.setId(id);
        toUpdate.setStatus(-1);
        Response<Boolean> resp= operatorRoleWriteService.updateRole(toUpdate);
        if(log.isDebugEnabled()){
            log.debug("API-OPERATOR-DELETEROLE-END param: id [{}] ,resp: [{}]",id,resp.getResult());
        }
        return or500(resp);
    }

    /**
     * 拿所有合法角色
     *
     * @return 角色列表
     */
    @RequestMapping(value = "/all", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<OperatorRole> findAllRoles() {
        if(log.isDebugEnabled()){
            log.debug("API-OPERATOR-FINDALLROLES-START noparam: ");
        }
        Response<List<OperatorRole>> resp= operatorRoleReadService.findByStatus(1);
        if(log.isDebugEnabled()){
            log.debug("API-OPERATOR-FINDALLROLES-END noparam: ,resp: [{}]",JsonMapper.nonEmptyMapper().toJson(resp.getResult()));
        }
        return RespHelper.or500(resp);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<OperatorRole> findById(@PathVariable Long id) {
        if(log.isDebugEnabled()){
            log.debug("API-OPERATOR-FINDBYID-START param: id [{}]",id);
        }
        Response<OperatorRole> resp= operatorRoleReadService.findById(id);
        if(log.isDebugEnabled()){
            log.debug("API-OPERATOR-FINDBYID-END param: id [{}] ,resp: [{}]",id,JsonMapper.nonEmptyMapper().toJson(resp.getResult()));
        }
        return resp;
    }

    @RequestMapping(value = "/find-for-edit", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<OperatorRole> findForEdit(@RequestParam(required = false) Long id) {
        if(log.isDebugEnabled()){
            log.debug("API-OPERATOR-FINDFOREDIT-START param: id [{}]",id);
        }
        if (id == null) {
            OperatorRole role = new OperatorRole();
            return Response.ok(role);
        }
        Response<OperatorRole> resp = operatorRoleReadService.findById(id);
        if(log.isDebugEnabled()){
            log.debug("API-OPERATOR-FINDFOREDIT-END param: id [{}] ,resp: [{}]",id,JsonMapper.nonEmptyMapper().toJson(resp.getResult()));
        }
        return resp;
    }

    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Paging<OperatorRole>> pagination(@RequestParam(required = false) Long id,
                                                     @RequestParam(required = false) Integer status,
                                                     @RequestParam(required = false) Integer pageNo,
                                                     @RequestParam(required = false) Integer pageSize) {
        if(log.isDebugEnabled()){
            log.debug("API-OPERATOR-PAGINATION-START param: id [{}] status [{}] pageNo [{}] pageSize [{}]",id,status,pageNo,pageSize);
        }
        if (id != null) {
            val idResp = operatorRoleReadService.findById(id);
            if (!idResp.isSuccess()) {
                log.warn("find operator role by id={} failed, error={}", id, idResp.getError());
                return Response.fail(idResp.getError());
            }
            OperatorRole role = idResp.getResult();
            if (Objects.equals(role.getStatus(), -1)) {
                return Response.ok(Paging.<OperatorRole>empty());
            }
            return Response.ok(new Paging<>(1L, Lists.newArrayList(role)));
        }
        Response<Paging<OperatorRole>> resp = operatorRoleReadService.pagination(status, pageNo, pageSize);
        if(log.isDebugEnabled()){
            log.debug("API-OPERATOR-PAGINATION-END param: id [{}] status [{}] pageNo [{}] pageSize [{}] ,resp: [{}]",id,status,pageNo,pageSize,JsonMapper.nonEmptyMapper().toJson(resp.getResult()));
        }
        return resp;
    }

}
