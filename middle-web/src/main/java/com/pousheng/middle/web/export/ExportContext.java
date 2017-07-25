package com.pousheng.middle.web.export;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by sunbo@terminus.io on 2017/7/20.
 */

public class ExportContext {

    public ExportContext(List<?> data) {
        this.data = data;
    }

    private String filename;

    private List<?> data;


    /**
     * true忽略title格式错误
     * false如果title格式错误抛异常中断导出
     */
    private boolean ingoreTitleFormat = true;

    private List<ExportTitleContext> titleContexts;


    public ExportContext addTitle(ExportTitleContext titleContext) {
        if (null == titleContexts)
            titleContexts = new ArrayList<>();
        titleContexts.add(titleContext);
        return this;
    }


    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public List<?> getData() {
        return data;
    }

    public void setData(List<?> data) {
        this.data = data;
    }

    public List<ExportTitleContext> getTitleContexts() {
        return titleContexts;
    }

    public void setTitleContexts(List<ExportTitleContext> titleContexts) {
        this.titleContexts = titleContexts;
    }

    public boolean isIngoreTitleFormat() {
        return ingoreTitleFormat;
    }

    public void setIngoreTitleFormat(boolean ingoreTitleFormat) {
        this.ingoreTitleFormat = ingoreTitleFormat;
    }
}
