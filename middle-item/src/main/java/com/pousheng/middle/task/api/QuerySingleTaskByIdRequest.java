package com.pousheng.middle.task.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-12 11:23<br/>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuerySingleTaskByIdRequest implements Serializable {
    private static final long serialVersionUID = -8591597443747817032L;

    private Long taskId;
}
