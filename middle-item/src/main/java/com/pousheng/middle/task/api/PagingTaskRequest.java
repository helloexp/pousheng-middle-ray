package com.pousheng.middle.task.api;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-09 16:50<br/>
 */
@Data
public class PagingTaskRequest implements Serializable {
    private static final long serialVersionUID = 9088363545181436740L;

    private String type;
    private String status;
    private List<Long> exclude;
    private Integer pageNo;
    private Integer pageSize;
}
