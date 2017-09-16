package com.pousheng.middle.open.ych.token;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;

/**
 * 淘宝token
 * Created by cp on 9/13/17.
 */
@ConfigurationProperties(prefix = "taobao.token")
@Data
public class TaobaoToken implements Serializable {

    private static final long serialVersionUID = 1175258157993328052L;

    private String appKey;

    private String appName;
}