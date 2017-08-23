package com.pousheng.middle.open.erp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

/**
 * Created by cp on 8/23/17.
 */
@Data
@ConfigurationProperties(prefix = "erp.token")
public class ErpOpenApiToken implements Serializable {

    private static final long serialVersionUID = 4518664062015857508L;

    private String appKey;

    private String secret;

    private String gateway;

}
