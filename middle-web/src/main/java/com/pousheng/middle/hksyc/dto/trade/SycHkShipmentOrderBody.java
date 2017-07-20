package com.pousheng.middle.hksyc.dto.trade;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Created by songrenfei on 2017/7/19
 */
@Data
public class SycHkShipmentOrderBody implements Serializable{


    private static final long serialVersionUID = 7410531388126216790L;

    private List<SycHkShipmentOrderDto> orders;
}
