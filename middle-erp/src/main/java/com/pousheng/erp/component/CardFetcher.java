package com.pousheng.erp.component;

import com.pousheng.erp.model.PoushengCard;

import java.util.Date;
import java.util.List;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2017-06-28
 */
public interface CardFetcher {
    /**
     *
     * @param pageNo 起始页码
     * @param pageSize 每页返回数量
     * @param start 开始时间
     * @param end 结束时间(可空)
     * @return 对象
     */
    List<PoushengCard> fetch(int pageNo, int pageSize, Date start, Date end);
}
