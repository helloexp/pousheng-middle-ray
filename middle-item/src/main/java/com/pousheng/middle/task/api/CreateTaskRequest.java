package com.pousheng.middle.task.api;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-09 16:51<br/>
 */
@Data
public class CreateTaskRequest implements Serializable {
    private static final long serialVersionUID = -2203911494956097305L;

    private Map<String, Object> detail;
    private Map<String, Object> content;
    private String status;
    private String type;
}
