package com.pousheng.middle.web.excel.supplyRule.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-11 14:34<br/>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessDetailDTO implements Serializable {
    private static final long serialVersionUID = 5755703102812445010L;

    private String fileName;
    private String filePath;
}
