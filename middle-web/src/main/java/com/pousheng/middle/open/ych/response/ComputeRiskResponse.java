package com.pousheng.middle.open.ych.response;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by cp on 9/15/17.
 */
@Data
public class ComputeRiskResponse extends YchResponse implements Serializable {

    private static final long serialVersionUID = -1029493068845871908L;

    private double risk;

    private String riskType;

    private String riskDescription;

}
