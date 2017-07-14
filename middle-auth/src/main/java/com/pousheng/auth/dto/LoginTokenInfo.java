/*
 * Copyright (c) 2017. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * @author : songrenfei
 */
@Data
public class LoginTokenInfo implements Serializable{
    private static final long serialVersionUID = 5838793848752900770L;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private String expiresIn;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("error")
    private String error;

    @JsonProperty("error_description")
    private String errorDescription;



}
