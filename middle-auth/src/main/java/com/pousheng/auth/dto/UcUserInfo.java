package com.pousheng.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 用户中心用户数据
 */
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Data
public class UcUserInfo implements Serializable {


    private static final long serialVersionUID = 7246661666497597610L;
    @JsonProperty("user_id")
    private Long userId;

    private String name;

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("family_name")
    private String familyName;

    @JsonProperty("middle_name")
    private String middleName;

    private String nickname;

    private String username;

    private String profile;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    private String website;

    private String email;

    @JsonProperty("email_verified")
    private boolean emailVerified;

    private String gender;

    @JsonProperty("birthdate")
    private String birthDate;

    private String zoneinfo;

    private String locale;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("phone_number_verified")
    private boolean phoneNumberVerified;

    private Map<String, String> address;

    @JsonProperty("created_at")
    private Date createdAt;

    @JsonProperty("updated_at")
    private Date updatedAt;

    private Map<String, Object> metadata;
}