package com.pousheng.middle.web.utils.export;

import lombok.Data;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sunbo@terminus.io on 2017/7/20.
 */

@Data
public class ExportContext {

    public ExportContext(List<?> data) {
        this.data = data;
    }

    public ExportContext(String filename, List<?> data) {
        this.filename = filename;
        this.data = data;
    }

    public ExportContext(String filename, String path, List<?> data) {
        this.filename = filename;
        this.path = path;
        this.data = data;
    }

    private String filename;

    private String path;

    private List<?> data;

    private ResultType resultType = ResultType.BYTE_ARRAY;

    private File resultFile;
    private byte[] resultByteArray;

    private List<ExportTitleContext> titleContexts;

    public ExportContext addTitle(ExportTitleContext titleContext) {
        if (null == titleContexts)
            titleContexts = new ArrayList<>();
        titleContexts.add(titleContext);
        return this;
    }


    public enum ResultType {
        FILE, BYTE_ARRAY;
    }

}
