package com.pousheng.middle.web.events.item;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by songrenfei on 2017/12/7
 */
@Data
public class SkuTemplateUpdateEvent implements Serializable{

    private Long skuTemplateId;
}
