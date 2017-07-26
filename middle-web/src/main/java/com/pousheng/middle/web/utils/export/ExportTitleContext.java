package com.pousheng.middle.web.utils.export;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.ConstructorArgs;

/**
 * Created by sunbo@terminus.io on 2017/7/21.
 */
@Data

public class ExportTitleContext {

    public ExportTitleContext(String title) {
        this.title = title;
    }

    public ExportTitleContext(String title, String fieldName) {
        this.title = title;
        this.fieldName = fieldName;
    }

    private String title;

    private String fieldName;


}
