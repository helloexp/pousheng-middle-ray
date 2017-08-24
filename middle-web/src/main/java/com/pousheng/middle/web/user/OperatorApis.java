package com.pousheng.middle.web.user;

import com.google.common.base.CharMatcher;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.pousheng.auth.dto.UcUserInfo;
import com.pousheng.auth.model.MiddleUser;
import com.pousheng.auth.service.MiddleOperatorReadService;
import com.pousheng.auth.service.UserReadService;
import com.pousheng.auth.service.UserWriteService;
import com.pousheng.middle.constants.Constants;
import com.pousheng.middle.web.user.component.UcUserOperationLogic;
import com.pousheng.middle.web.user.component.UserManageShopReader;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.common.utils.JsonMapper;
import io.terminus.common.utils.Params;
import io.terminus.open.client.common.shop.dto.OpenClientShop;
import io.terminus.parana.auth.model.Operator;
import io.terminus.parana.auth.service.OperatorReadService;
import io.terminus.parana.auth.service.OperatorWriteService;
import io.terminus.parana.common.enums.UserRole;
import io.terminus.parana.common.enums.UserType;
import io.terminus.parana.common.utils.RespHelper;
import io.terminus.parana.common.utils.UserUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Effet
 */
@Slf4j
@RestController
@RequestMapping("/api/operator")
public class OperatorApis {

    @Autowired
    private UserWriteService userWriteService;
    @Autowired
    private UserReadService userReadService;
    @RpcConsumer
    private OperatorReadService operatorReadService;
    @RpcConsumer
    private OperatorWriteService operatorWriteService;
    @Autowired
    private UcUserOperationLogic ucUserOperationLogic;
    @Autowired
    private UserManageShopReader userManageShopReader;
    @Autowired
    private MiddleOperatorReadService middleOperatorReadService;


    /**
     * ADMIN 创建运营
     *
     * @param operator 运营信息
     * @return 运营用户 ID
     */
    @RequestMapping(value = "", method = RequestMethod.POST)
    public Long createOperator(@RequestBody OperatorPost operator) {
        String un = Params.trimToNull(operator.getUsername());
        if (un == null) {
            log.warn("create operator failed, no username specified");
            throw new JsonResponseException("operator.username.can.not.be.null");
        }

        if (!isBindingUser(operator)) {
            String pw = Params.trimToNull(operator.getPassword());
            if (pw == null) {
                log.warn("create operator failed, no password specified");
                throw new JsonResponseException("operator.password.can.not.be.null");
            }
            judgePassword(operator.getPassword());
        }
        judgeUsername(operator.getUsername());

        checkUserExist(un);

        Operator toCreateOperator = new Operator();
        toCreateOperator.setUserName(operator.getUsername());
        toCreateOperator.setRoleId(operator.getRoleId());
        Map<String, String> extraMap = Maps.newHashMap();
        extraMap.put(Constants.MANAGE_SHOP_IDS, JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(handleManageShopIds(operator.getManageShopIds())));
        toCreateOperator.setExtra(extraMap);

        Long outUserId;
        if (isBindingUser(operator)) {
            outUserId = operator.getUserId();
            checkUserExist(outUserId);
        } else {
            //调用用户中心创建用户
            Response<UcUserInfo> ucUserInfoRes = ucUserOperationLogic.createUcUser(operator.getUsername(), operator.getPassword());
            if (!ucUserInfoRes.isSuccess()) {
                log.error("create middleUser center middleUser(name:{}) fail,error:{}", operator.getUsername(), ucUserInfoRes.getError());
                throw new JsonResponseException(ucUserInfoRes.getError());
            }
            UcUserInfo ucUserInfo = ucUserInfoRes.getResult();
            outUserId = ucUserInfo.getUserId();
        }


        // 创建 middle middleUser
        MiddleUser middleUser = new MiddleUser();
        middleUser.setName(un);
        middleUser.setOutId(outUserId);
        middleUser.setType(UserType.OPERATOR.value());
        middleUser.setRoles(Lists.newArrayList(UserRole.OPERATOR.name()));
        Response<Long> userCreateResp = userWriteService.create(middleUser);
        if (!userCreateResp.isSuccess()) {
            log.error("failed to create operator middleUser = {}, cause: {}", middleUser, userCreateResp.getError());
            throw new JsonResponseException(userCreateResp.getError());
        }

        // 创建operator
        Long userId = userCreateResp.getResult();
        toCreateOperator.setUserId(userId);

        return RespHelper.or500(operatorWriteService.create(toCreateOperator));
    }

    @RequestMapping(value = "/{userId}", method = RequestMethod.PUT)
    public Boolean updateOperator(@PathVariable Long userId, @RequestBody OperatorPost operator) {

        Response<MiddleUser> userRes = userReadService.findById(userId);
        if (!userRes.isSuccess()) {
            log.error("find user(id:{}) fail,error:{}", userId, userRes.getError());
            throw new JsonResponseException(userRes.getError());
        }
        MiddleUser existMiddleUser = userRes.getResult();


        Response<Operator> operatorResp = operatorReadService.findByUserId(userId);
        if (!operatorResp.isSuccess()) {
            log.warn("operator find fail, userId={}, error={}", userId, operatorResp.getError());
            throw new JsonResponseException(operatorResp.getError());
        }
        Operator existOp = operatorResp.getResult();

        MiddleUser toUpdateMiddleUser = new MiddleUser();
        toUpdateMiddleUser.setId(userId);
        String username = Params.trimToNull(operator.getUsername());

        if (username != null) {
            judgeUsername(username);
            toUpdateMiddleUser.setName(username);
        }

        String password = Params.trimToNull(operator.getPassword());
        if (password != null) {
            judgePassword(password);
        }

        //更新用户中心用户信息
        Response<UcUserInfo> ucUserInfoRes = ucUserOperationLogic.updateUcUser(existMiddleUser.getOutId(), operator.getUsername(), operator.getPassword());
        if (!ucUserInfoRes.isSuccess()) {
            log.error("update user center user(id:{}) fail,error:{}", existMiddleUser.getOutId(), ucUserInfoRes.getError());
            throw new JsonResponseException(ucUserInfoRes.getError());
        }

        Response<Boolean> userResp = userWriteService.update(toUpdateMiddleUser);
        if (!userResp.isSuccess()) {
            log.warn("user update failed, cause:{}", userResp.getError());
            throw new JsonResponseException(userResp.getError());
        }

        Operator toUpdateOperator = new Operator();
        toUpdateOperator.setId(existOp.getId());
        toUpdateOperator.setUserName(toUpdateMiddleUser.getName());
        toUpdateOperator.setRoleId(operator.getRoleId());
        Map<String, String> extraMap = existOp.getExtra();//这里就不判断extra是否为空了，创建时会塞入管理店铺id，所以这里一定不会为空
        extraMap.put(Constants.MANAGE_SHOP_IDS, JsonMapper.JSON_NON_EMPTY_MAPPER.toJson(handleManageShopIds(operator.getManageShopIds())));
        toUpdateOperator.setExtra(extraMap);

        Response<Boolean> opUpdateResp = operatorWriteService.update(toUpdateOperator);
        if (!opUpdateResp.isSuccess()) {
            log.warn("operator update failed, error={}", opUpdateResp.getError());
            throw new JsonResponseException(opUpdateResp.getError());
        }
        return Boolean.TRUE;
    }

    private void judgePassword(String password) {
        if (!password.matches("[\\s\\S]{6,16}")) {
            throw new JsonResponseException(400, "user.password.invalid");
        }
    }

    private void judgeUsername(String username) {
        if (CharMatcher.digit().matchesAllOf(username)
                || CharMatcher.is('@').matchesAnyOf(username)) {
            throw new JsonResponseException(400, "user.username.invalid");
        }
    }

    private List<Long> handleManageShopIds(List<Long> manageShopIds) {
        if (CollectionUtils.isEmpty(manageShopIds)) {
            manageShopIds = Lists.newArrayList();
        }
        return manageShopIds;
    }

    private Boolean isBindingUser(OperatorPost operatorPost) {
        return !Arguments.isNull(operatorPost.getUserId());
    }

    @RequestMapping(value = "/{userId}/frozen", method = RequestMethod.PUT)
    public Boolean frozenOperator(@PathVariable Long userId) {
        Response<Operator> opResp = operatorReadService.findByUserId(userId);
        if (!opResp.isSuccess()) {
            log.warn("operator find failed, userId={}, error={}", userId, opResp.getError());
            throw new JsonResponseException(opResp.getError());
        }
        Operator op = opResp.getResult();
        if (!op.isActive()) {
            return Boolean.FALSE;
        }

        Operator toUpdate = new Operator();
        toUpdate.setId(op.getId());
        toUpdate.setStatus(0);
        Response<Boolean> updateResp = operatorWriteService.update(toUpdate);
        if (!updateResp.isSuccess()) {
            log.warn("frozen operator failed, userId={}, error={}", userId, updateResp.getError());
            throw new JsonResponseException(updateResp.getError());
        }
        return Boolean.TRUE;
    }

    @RequestMapping(value = "/{userId}/unfrozen", method = RequestMethod.PUT)
    public Boolean unfrozenOperator(@PathVariable Long userId) {
        Response<Operator> opResp = operatorReadService.findByUserId(userId);
        if (!opResp.isSuccess()) {
            log.warn("operator find failed, userId={}, error={}", userId, opResp.getError());
            throw new JsonResponseException(opResp.getError());
        }
        Operator op = opResp.getResult();
        if (!Objects.equals(op.getStatus(), 0)) {
            return Boolean.FALSE;
        }

        Operator toUpdate = new Operator();
        toUpdate.setId(op.getId());
        toUpdate.setStatus(1);
        Response<Boolean> updateResp = operatorWriteService.update(toUpdate);
        if (!updateResp.isSuccess()) {
            log.warn("frozen operator failed, userId={}, error={}", userId, updateResp.getError());
            throw new JsonResponseException(updateResp.getError());
        }
        return Boolean.TRUE;
    }

    @RequestMapping(value = "/paging", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public Response<Paging<Operator>> pagingOperator(@RequestParam(required = false) Long roleId,
                                                     @RequestParam(required = false) Long userId,
                                                     @RequestParam(required = false) String roleName,
                                                     @RequestParam(required = false) String userName,
                                                     @RequestParam(required = false) Integer pageNo,
                                                     @RequestParam(required = false) Integer pageSize) {
        return middleOperatorReadService.pagination(roleId, userId, userName, roleName, null, pageNo, pageSize);
    }

    @RequestMapping(value = "/manage/shops", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<OpenClientShop> findManageShops() {
        return userManageShopReader.findManageShops(UserUtil.getCurrentUser());
    }

    @Data
    public static class OperatorPost {

        private String username;

        private String password;

        private Long roleId;
        //管理店铺ids
        private List<Long> manageShopIds;

        //用户中心用户id（绑定已有账户时）
        private Long userId;
    }


    private void checkUserExist(String name) {

        Response<MiddleUser> middleUserRes = userReadService.findByName(name);
        if (!middleUserRes.isSuccess()) {
            log.error("find middle user by name:{} fail ,error:{}", name, middleUserRes.getError());
            throw new JsonResponseException(middleUserRes.getError());
        }

        if (!Arguments.isNull(middleUserRes.getResult())) {
            log.error("user name:{} is exist");
            throw new JsonResponseException("user.name.already.exist");
        }


    }

    private void checkUserExist(Long outId) {

        Response<Optional<MiddleUser>> middleUserRes = userReadService.findByOutId(outId);
        if (!middleUserRes.isSuccess()) {
            log.error("find middle user by outId:{} fail ,error:{}", outId, middleUserRes.getError());
            throw new JsonResponseException(middleUserRes.getError());
        }

        if (middleUserRes.getResult().isPresent()) {
            log.error("user name:{} is exist");
            throw new JsonResponseException("user.already.exist");
        }


    }

}