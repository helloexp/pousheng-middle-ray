package com.pousheng.middle.web.utils.export;

import lombok.Data;

import java.util.Date;

/**
 * Created by sunbo@terminus.io on 2017/7/26.
 */
@Data
public class FileRecord {

    private String name;
    private String url;
    private Date exportAt;
}
