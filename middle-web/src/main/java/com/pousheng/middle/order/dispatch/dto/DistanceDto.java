package com.pousheng.middle.order.dispatch.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/12/25
 */
@Data
public class DistanceDto implements Serializable{

    private static final long serialVersionUID = -3419795086729377986L;

    private Long id;

    private double distance;
}
