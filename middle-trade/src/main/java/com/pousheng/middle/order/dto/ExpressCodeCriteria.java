package com.pousheng.middle.order.dto;

import io.terminus.parana.common.model.PagingCriteria;
import lombok.Data;
import org.joda.time.DateTime;

import java.util.Date;

/**
 * Created by tony on 2017/6/28.
 */
@Data
public class ExpressCodeCriteria extends PagingCriteria implements java.io.Serializable{

    private static final long serialVersionUID = -2968622568681923595L;
    /**
     * 快递名称
     */
    private String name;



}
