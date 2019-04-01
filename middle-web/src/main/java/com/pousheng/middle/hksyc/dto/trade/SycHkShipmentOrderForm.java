package com.pousheng.middle.hksyc.dto.trade;

import com.pousheng.middle.hksyc.dto.HkRequestHead;
import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/7/19
 */
@Data
public class SycHkShipmentOrderForm implements Serializable{

    private static final long serialVersionUID = -8971663297124108197L;

    private HkRequestHead head;

    private SycHkShipmentOrderBody body;


}
