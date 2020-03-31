package com.pousheng.middle.web.warehouses.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-07-12
 */
@Data
public class Company implements Serializable {
    private static final long serialVersionUID = 5130047977617950411L;

    private String id;

    private String companyName;
}
