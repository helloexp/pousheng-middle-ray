package com.pousheng.middle.web.excel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-18 15:48<br/>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnifiedEvent implements Serializable {
    private static final long serialVersionUID = 6510732658642620164L;

    private String eventTag;
    private String payload;
}
