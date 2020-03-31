package com.pousheng.middle.web.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import com.google.common.base.Objects;
import com.google.common.eventbus.EventBus;
import com.google.gson.Gson;
import com.pousheng.auth.model.MiddleUser;
import com.pousheng.auth.service.PsUserReadService;
import com.pousheng.middle.utils.ParanaUserMaker;
import com.pousheng.middle.web.events.user.LoginEvent;
import com.pousheng.middle.web.user.component.UcUserOperationLogic;
import com.yysports.cas.comm.dto.SystemAccountData;
import com.yysports.cas.comm.dto.UserSessionBean;
import com.yysports.cas.comm.utils.CasContextUtils;

import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.BeanMapper;
import io.terminus.parana.auth.model.Operator;
import io.terminus.parana.auth.service.OperatorReadService;
import io.terminus.parana.common.enums.UserType;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.common.utils.RespHelper;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.assertj.core.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping(value = "/cas-user", produces = MediaType.APPLICATION_JSON_VALUE)
public class CasUserApi {

	@Autowired
	private EventBus eventBus;
	@Autowired
	private UcUserOperationLogic operationLogic;
	@Autowired
	private PsUserReadService userReadService;
	@RpcConsumer
	private OperatorReadService operatorReadService;
	
	@Value("${pousheng.system.cas.frontedCasApi}")
	private String frontedCasApi;

	/**
	 * 使用預帳號不帶密碼認證接口
	 * @param target 前台CAS迴向URL
	 * @param request HttpServletRequest
	 * @param response HttpServletResponse
	 * @throws IOException
	 * @throws ServletException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	@GetMapping("/by-name")
	public void loginByName(@RequestParam(required = false) String target,
			HttpServletRequest request, HttpServletResponse response) throws IOException,
			ServletException, IllegalArgumentException, IllegalAccessException {

		// log.info("cookies: " + new Gson().toJson(request.getCookies()));
		// log.info("reaponse: " + new Gson().toJson(response));

		SystemAccountData user = CasContextUtils.getCurrentUser();
		log.info("user: " + user);

		if (user == null) {
			return;
		}
		if (!Strings.isNullOrEmpty(user.getErrMsg())) {
			// TODO 回傳錯誤訊息
			response.setContentType("text/html; charset=utf-8");
			PrintWriter out = response.getWriter();
			out.print("<script>alert('" + user.getErrMsg() + "');</script>");
			out.flush();
			return;
		}
		UserSessionBean bean = new UserSessionBean();
		if (request.getSession() != null) {
			bean.setId(request.getSession().getId());
		} else {
			bean.setId(UUID.randomUUID().toString());
		}

		bean.setStamp(System.currentTimeMillis());
		bean.setUserData(user.getDomainAccount());
		Response<String> result = operationLogic.createSession(bean);
		log.info("loginByName, params: {}, result: {}", bean, result);
		if (!result.isSuccess()) {
			log.error("create user redis session fail, param: {}, return: {}", bean, result.getResult());
			throw new JsonResponseException("會員中心建立SESSION資料錯誤");
		}
		Response<MiddleUser> midUser = userReadService.findByName(user.getAccountName());
		if (!midUser.isSuccess()) {
			log.error("find  user failed, userName={}, error={}", user.getAccountName(),
					midUser.getError());
			throw new JsonResponseException(midUser.getError());
		}

		ParanaUserDto paranaUser = buildParanaUser(midUser.getResult());
		log.debug("LOGIN SUCCESS user name:{}", paranaUser.getName());
		eventBus.post(new LoginEvent(request, response, paranaUser));
		log.debug("PUSH LOGIN EVENT SUCCESS");
		// Client 傳入的URL
		paranaUser.setTargetUrl(target);

		response.addHeader("paranaUser", new Gson().toJson(paranaUser));

		StringBuffer url = new StringBuffer(frontedCasApi + "?id=");
		url.append(paranaUser.getId());
		if (!Strings.isNullOrEmpty(paranaUser.getTargetUrl())) {
			url.append("&target=").append(URLEncoder.encode(paranaUser.getTargetUrl(), "UTF-8"));
		}
		response.setStatus(302);
		response.sendRedirect(url.toString());
		// return paranaUser;
	}

	/**
	 * 登出用接口
	 * @param target
	 * @param request
	 * @param response
	 */
	@GetMapping("/logout")
	public void logoutCas(@RequestParam(required = false) String target,
			HttpServletRequest request, HttpServletResponse response) {
		try {
			loginByName(target, request, response);
			return;
		} catch (Exception ex) {
			log.info("CasUserApi.logoutCas cause exception, ex: {}", ex);
		}
	}

	private ParanaUserDto buildParanaUser(MiddleUser middleUser) {
		if (Objects.equal(middleUser.getType(), UserType.OPERATOR.value())) {
			Operator operator = RespHelper
					.or500(operatorReadService.findByUserId(middleUser.getId()));
			if (operator == null) {
				throw new JsonResponseException("operator.not.exist");
			}
			if (Objects.equal(operator.getStatus(), 0)) {
				throw new JsonResponseException("user.status.locked");
			}
		}

		ParanaUser paranaUser = ParanaUserMaker.from(middleUser);
		paranaUser.setId(middleUser.getOutId());

		ParanaUserDto data = new ParanaUserDto();
		BeanMapper.copy(paranaUser, data);
		return data;
	}

	/**
	 * CAS 接口，回傳中台前端用物件
	 * @author dream
	 *
	 */
	public class ParanaUserDto extends ParanaUser {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Getter
		@Setter
		private String targetUrl;

	}

}
