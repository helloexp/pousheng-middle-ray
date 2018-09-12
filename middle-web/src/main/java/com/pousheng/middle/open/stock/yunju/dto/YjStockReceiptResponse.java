package com.pousheng.middle.open.stock.yunju.dto;

import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Description: TODO
 * User:        liangyj
 * Date:        2018/7/7
 */
@Data
@ToString
public class YjStockReceiptResponse implements Serializable {

    private static final long serialVersionUID = -5549610860427212149L;
    private boolean success;
    private String error;
    private String message;   //如果success = false,则通过errorMessage 查看具体文本信息
}
