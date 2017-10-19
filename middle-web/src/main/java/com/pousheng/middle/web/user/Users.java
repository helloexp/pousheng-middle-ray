package com.pousheng.middle.web.user;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.pousheng.auth.dto.LoginTokenInfo;
import com.pousheng.auth.dto.UcUserInfo;
import com.pousheng.auth.model.MiddleUser;
import com.pousheng.auth.service.PsUserReadService;
import com.pousheng.middle.utils.ParanaUserMaker;
import com.pousheng.middle.web.events.user.LoginEvent;
import com.pousheng.middle.web.user.component.UcUserOperationLogic;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.parana.auth.api.Role;
import io.terminus.parana.auth.api.RoleContent;
import io.terminus.parana.auth.api.UserRoleLoader;
import io.terminus.parana.auth.model.Operator;
import io.terminus.parana.auth.service.OperatorReadService;
import io.terminus.parana.common.enums.UserType;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.RespHelper;
import io.terminus.parana.common.utils.UserUtil;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.print.attribute.standard.JobSheets;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * @author songrenfei
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
public class Users {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private PsUserReadService userReadService;
    @Autowired
    private UcUserOperationLogic operationLogic;
    @Autowired
    private PsUserReadService psUserReadService;
    @Autowired
    private UserRoleLoader userRoleLoader;
    @RpcConsumer
    private OperatorReadService operatorReadService;


    private static final DateTimeFormatter DTF = DateTimeFormat.forPattern("yyyy-MM-dd");



    @RequestMapping(value = "/{userId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<ParanaUser> findUserById(@PathVariable Long userId) {
        val userResp = userReadService.findById(userId);
        if (!userResp.isSuccess()) {
            log.warn("find user by id={} failed, error={}", userId, userResp.getError());
            return Response.fail(userResp.getError());
        }
        return Response.ok(buildParanaUser(userResp.getResult()));
    }


    /**
     * @return ParanaUser ，id为会员中心的id（特殊处理过）
     */
    @RequestMapping(value = "/login", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ParanaUser login(@RequestParam String username,
                            @RequestParam String password,
                            HttpServletRequest request,
                            HttpServletResponse response) {


        //获取登录token
        LoginTokenInfo tokenInfo =  operationLogic.getUserToken(username,password);

        if(Arguments.isNull(tokenInfo)){
            log.error("get user center token fail");
            throw new JsonResponseException("get.user.center.token.fail");
        }

        if(!Strings.isNullOrEmpty(tokenInfo.getError())){
            log.error("user login by user name:{} password:{} fail,error:{}",username,password,tokenInfo.getErrorDescription());
            throw new JsonResponseException(tokenInfo.getErrorDescription());
        }

        UcUserInfo ucUserInfo = operationLogic.authGetUserInfo(tokenInfo.getAccessToken());

        log.info("[MIDDLE] get uc user info:{}",ucUserInfo);


        val userResp = userReadService.findByOutId(ucUserInfo.getUserId());
        log.info("end find middle user by outer id:{}",ucUserInfo.getUserId());

        if (!userResp.isSuccess()) {
            log.warn("find  user failed, outId={}, error={}", ucUserInfo.getUserId(), userResp.getError());
            throw new JsonResponseException(userResp.getError());
        }
        Optional<MiddleUser> userOptional = userResp.getResult();
        if(!userOptional.isPresent()){
            log.error("user(name:{}) not belong to current system",ucUserInfo.getUsername());
            throw new JsonResponseException("authorize.fail");
        }

        log.info("[MIDDLE] find pousheng user by outer id:{}",ucUserInfo.getUserId());


        ParanaUser paranaUser = buildParanaUser(userOptional.get());
        log.info("LOGIN SUCCESS user name:{}",paranaUser.getName());
        eventBus.post(new LoginEvent(request,response,paranaUser));
        log.info("PUSH LOGIN EVENT SUCCESS");
        return paranaUser;
    }

    private ParanaUser buildParanaUser(MiddleUser middleUser) {
        if (Objects.equal(middleUser.getType(), UserType.OPERATOR.value())) {
            Operator operator = RespHelper.or500(operatorReadService.findByUserId(middleUser.getId()));
            if (operator == null) {
                throw new JsonResponseException("operator.not.exist");
            }
            if (Objects.equal(operator.getStatus(), 0)) {
                throw new JsonResponseException("user.status.locked");
            }
        }

        middleUser.setId(middleUser.getOutId());
        return ParanaUserMaker.from(middleUser);
    }

    /**
     * 查询userid 对应的role
     * @param userId out user id
     * @return RoleContent
     */
    @RequestMapping(value = "/{userId}/roles", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<RoleContent> getUserRolesByUserId(@PathVariable Long userId) {
        MiddleUser middleUser = findMiddleUserByOutId(userId);
        return userRoleLoader.hardLoadRoles(middleUser.getId());
    }

    /**
     * 查询userid 对应的role
     * @param userId out user id
     * @return Role
     */
    @GetMapping("/{userId}/role-names")
    public List<String> getRoleNamesOfUserId(@PathVariable Long userId) {
        MiddleUser middleUser = findMiddleUserByOutId(userId);
        RoleContent content = RespHelper.or500(userRoleLoader.hardLoadRoles(middleUser.getId()));
        List<String> result = new ArrayList<>();
        if (content != null) {
            for (Role role : content.getRoles()) {
                result.add(role.getBase());
            }
            for (Role role : content.getDynamicRoles()) {
                result.addAll(role.getNames());
            }
        }
        return result;
    }


    @RequestMapping(value = "/current", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ParanaUser findCurrentUser() {
        return UserUtil.getCurrentUser();
    }


    @RequestMapping(value = "/uc/query", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<UcUserInfo> findUserCenterUser(@RequestParam String userName) {
        Response<List<UcUserInfo>> listRes = operationLogic.queryUserByName(userName);
        if(!listRes.isSuccess()){
            log.error("find user center user by name:{} fail,error:{}",userName,listRes.getError());
            throw new JsonResponseException(listRes.getError());
        }

        return listRes.getResult();
    }

    private MiddleUser findMiddleUserByOutId(Long outId){
        Response<Optional<MiddleUser>>  response =  psUserReadService.findByOutId(outId);
        if(!response.isSuccess()){
            log.error("find middle user by out id:{} fail,error:{}",outId,response.getError());
            throw new JsonResponseException(response.getError());
        }
        if(!response.getResult().isPresent()){
            log.error("not find middle user by out id:{}");
            throw new JsonResponseException("user.not.exist");
        }

        return response.getResult().get();
    }

}
