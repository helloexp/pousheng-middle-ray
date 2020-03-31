package com.pousheng.middle.web.task;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/6/7
 */
@Data
public class SyncErrorData implements Serializable {

    private static final long serialVersionUID = 3329360945088887826L;

    private Long id;

    private String name;

    private String error;
}
