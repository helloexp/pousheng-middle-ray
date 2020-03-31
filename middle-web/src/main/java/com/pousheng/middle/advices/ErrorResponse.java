package com.pousheng.middle.advices;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.google.common.base.MoreObjects;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/11/27
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse implements Serializable{

    private static final long serialVersionUID = 2988012073163003532L;


    @Getter
    @Setter
    private boolean success;
    @Getter
    private String error;
    @Getter
    private String message;   //如果success = false,则通过errorMessage 查看具体文本信息




    public void setError(String error) {
        this.success = false;
        this.error = error;
    }

    private void setError(String error, String errorMessage) {
        this.success = false;
        this.error = error;
        this.message = errorMessage;
    }




    public static ErrorResponse fail(String error) {
        ErrorResponse resp = new ErrorResponse();
        resp.setError(error);
        return resp;
    }

    public static ErrorResponse  fail(String error, String errorMessage) {
        ErrorResponse resp = new ErrorResponse();
        resp.setError(error, errorMessage);
        return resp;
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("success", success)
                .add("error", error)
                .add("message", message)
                .omitNullValues()
                .toString();
    }
}
