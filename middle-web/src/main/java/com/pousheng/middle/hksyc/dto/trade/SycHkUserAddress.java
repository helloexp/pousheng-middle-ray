package com.pousheng.middle.hksyc.dto.trade;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/7/19
 */
@Data
public class SycHkUserAddress implements Serializable{

    private static final long serialVersionUID = 3258882635427743521L;

    private String country = "中国";
    private String city = "广州市";
    private String province = "广东省";
    private String zipCode = "";
    private String addressDetail = "彩频路11号广东软件科学园F301室";
    private String mobile = "13844214754@qq.com";
    private String tel = "13874454214";
    private String contact = "阿汤";
    private String email = "";
    private String district = "萝岗区";


}
