package com.pousheng.middle.consume.index.processor.impl;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-23 17:45<br/>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkDocument implements Serializable {
    private static final long serialVersionUID = 29112184431443153L;

    private String indexName;
    private String indexType;

    private Object id;
    private String source;
}
