package com.pousheng.middle.web.user;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.eventbus.EventBus;
import com.pousheng.auth.dto.LoginTokenInfo;
import com.pousheng.auth.dto.UcUserInfo;
import com.pousheng.auth.model.MiddleUser;
import com.pousheng.auth.service.UserReadService;
import com.pousheng.middle.utils.ParanaUserMaker;
import com.pousheng.middle.web.events.user.LoginEvent;
import com.pousheng.middle.web.user.component.UcUserOperationLogic;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.parana.auth.api.Role;
import io.terminus.parana.auth.api.RoleContent;
import io.terminus.parana.auth.api.UserRoleLoader;
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
    private UserReadService userReadService;
    @Autowired
    private UcUserOperationLogic operationLogic;
    @Autowired
    private UserRoleLoader userRoleLoader;


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



    @RequestMapping(value = "/login", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ParanaUser login(@RequestParam String username,
                            @RequestParam String password,
                            HttpServletRequest request,
                            HttpServletResponse response) {


        //获取登录token
        LoginTokenInfo tokenInfo =  operationLogic.getUserToken(username,password);
        if(!Strings.isNullOrEmpty(tokenInfo.getError())){
            log.error("user login by user name:{} password:{} fail,error:{}",username,password,tokenInfo.getErrorDescription());
            throw new JsonResponseException(tokenInfo.getErrorDescription());
        }

        UcUserInfo ucUserInfo = operationLogic.authGetUserInfo(tokenInfo.getAccessToken());


        val userResp = userReadService.findByOutId(ucUserInfo.getUserId());
        if (!userResp.isSuccess()) {
            log.warn("find  user failed, outId={}, error={}", ucUserInfo.getUserId(), userResp.getError());
            throw new JsonResponseException(userResp.getError());
        }
        Optional<MiddleUser> userOptional = userResp.getResult();
        if(!userOptional.isPresent()){
            log.error("user(name:{}) not belong to current system",ucUserInfo.getUsername());
            throw new JsonResponseException("authorize.fail");
        }


        ParanaUser paranaUser= buildParanaUser(userOptional.get());
        eventBus.post(new LoginEvent(request,response,paranaUser));
        return paranaUser;
    }

    private ParanaUser buildParanaUser(MiddleUser middleUser) {
  /*      if (Objects.equal(middleUser.getStatus(), UserStatus.DELETED.value())) {
            throw new JsonResponseException("middleUser.not.found");
        }
        if (Objects.equal(middleUser.getStatus(), UserStatus.FROZEN.value())) {
            throw new JsonResponseException("middleUser.status.frozen");
        }
        if (Objects.equal(middleUser.getStatus(), UserStatus.LOCKED.value())) {
            throw new JsonResponseException("middleUser.status.locked");
        }*/

        return ParanaUserMaker.from(middleUser);
    }

    @RequestMapping(value = "/{userId}/roles", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<RoleContent> getUserRolesByUserId(@PathVariable Long userId) {
        return userRoleLoader.hardLoadRoles(userId);
    }

    @GetMapping("/{userId}/role-names")
    public List<String> getRoleNamesOfUserId(@PathVariable Long userId) {
        RoleContent content = RespHelper.or500(userRoleLoader.hardLoadRoles(userId));
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
}
