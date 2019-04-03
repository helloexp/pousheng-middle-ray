package com.pousheng.middle.task.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-09 15:36<br/>
 */
@Data
public class TaskDTO implements Serializable {
    private static final long serialVersionUID = 730606362627550925L;

    private Long id;
    private Map<String, Object> content;
    private String status;
    private String type;
    private Date createdAt;
    private Date updatedAt;
}
