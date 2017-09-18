package com.pousheng.middle.open.ych.token;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

/**
 * 御城河token
 * Created by cp on 9/13/17.
 */
@ConfigurationProperties(prefix = "ych.token")
@Data
public class YchToken implements Serializable {

    private static final long serialVersionUID = -6898689415453356041L;

    private String appKey;

    private String secret;

    private String gateway;

    private String gatewayOfLog;

    private String clientIp;

}