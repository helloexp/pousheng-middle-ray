package com.pousheng.erp.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-05-23
 */
@Data
public class SkuGroupRule implements Serializable {
    private static final long serialVersionUID = -8199523841469053571L;

    /**
     * 自增主键
     */
    private Long id;

    /**
     * 品牌id
     */
    private String cardId;

    /**
     * 规则适用的类目id
     */
    private String kindId;


    /**
     * 规则类型, 1为按照分割符来区分色号, 2为按照末尾xx为来区分色号, 3为优先分隔符, 次为index
     */
    private Integer ruleType;

    /**
     * 分隔符
     */
    private Character splitChar;


    /**
     * 色号起始位(从末尾开始数)
     */
    private Integer lastStart;

    /**
     * 针对第4种规则进行拼接,例如：7-1-10-9（如果第7位含有1则取前10位,如果没有1则取前9位）
     */
    private String ruleDetail;

    /**
     * 创建时间
     */
    private Date createdAt;


    /**
     * 更新时间
     */
    private Date updatedAt;


}
