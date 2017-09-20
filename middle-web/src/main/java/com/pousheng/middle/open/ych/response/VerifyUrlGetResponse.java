package com.pousheng.middle.open.ych.response;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by cp on 9/15/17.
 */
@Data
public class VerifyUrlGetResponse extends YchResponse implements Serializable {

    private static final long serialVersionUID = -1779599452888512443L;

    private String verifyUrl;

}
