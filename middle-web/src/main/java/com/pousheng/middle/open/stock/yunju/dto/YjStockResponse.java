package com.pousheng.middle.open.stock.yunju.dto;

import lombok.Data;
import lombok.ToString;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Description: TODO
 * User:        liangyj
 * Date:        2018/6/26
 */
@Component
@Data
@ToString
public class YjStockResponse implements Serializable{

    private static final long serialVersionUID = -507659993675143080L;
    private String error;
    private String error_info;
    private String[] data;
}
