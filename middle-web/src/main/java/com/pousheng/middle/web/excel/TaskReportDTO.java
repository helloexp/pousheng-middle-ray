package com.pousheng.middle.web.excel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-10 15:35<br/>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskReportDTO implements Serializable {
    private static final long serialVersionUID = -386675480023722453L;

    private Map current;
    private Map[] tasks;
    private Long executedCount;
}
