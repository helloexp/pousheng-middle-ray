package com.pousheng.middle.web.user;

import com.google.common.collect.Lists;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
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
    @RequestMapping(value = "", method = RequestMethod.POST)
    public Long createRole(@RequestBody OperatorRole role) {
        role.setStatus(1);
        return or500(operatorRoleWriteService.createRole(role));
    }

    /**
     * 更新运营角色
     *
     * @param id   角色 ID
     * @param role 角色授权内容
     * @return 是否更新成功
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT)
    public Boolean updateRole(@PathVariable Long id, @RequestBody OperatorRole role) {
        OperatorRole existRole = RespHelper.orServEx(operatorRoleReadService.findById(id));
        if (existRole == null) {
            throw new JsonResponseException(500, "operator.role.not.exist");
        }
        role.setId(id);
        role.setStatus(null); // prevent update
        return or500(operatorRoleWriteService.updateRole(role));
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public Boolean deleteRole(@PathVariable Long id) {
        OperatorRole toUpdate = new OperatorRole();
        toUpdate.setId(id);
        toUpdate.setStatus(-1);
        return or500(operatorRoleWriteService.updateRole(toUpdate));
    }

    /**
     * 拿所有合法角色
     *
     * @return 角色列表
     */
    @RequestMapping(value = "/all", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<OperatorRole> findAllRoles() {
        return RespHelper.or500(operatorRoleReadService.findByStatus(1));
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<OperatorRole> findById(@PathVariable Long id) {
        return operatorRoleReadService.findById(id);
    }

    @RequestMapping(value = "/find-for-edit", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<OperatorRole> findForEdit(@RequestParam(required = false) Long id) {
        if (id == null) {
            OperatorRole role = new OperatorRole();
            return Response.ok(role);
        }
        return operatorRoleReadService.findById(id);
    }

    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Paging<OperatorRole>> pagination(@RequestParam(required = false) Long id,
                                                     @RequestParam(required = false) Integer status,
                                                     @RequestParam(required = false) Integer pageNo,
                                                     @RequestParam(required = false) Integer pageSize) {
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
        return operatorRoleReadService.pagination(status, pageNo, pageSize);
    }

}
