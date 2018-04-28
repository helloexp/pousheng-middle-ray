package com.pousheng.middle.web.utils;

import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * map操作工具类
 * Created with IntelliJ IDEA
 * Author: songrenfei
 * Date: 15/4/22
 * Time: 上午11:58
 */
public class MapFilter {


    /**
     * 过滤map中key为null或empty的元素
     * @param criteria map
     * @return 过滤后得map
     */
    public static Map<String, String> filterNullOrEmpty(Map<String, String> criteria) {
        return Maps.filterEntries(criteria, new Predicate<Map.Entry<String, String>>() {
            @Override
            public boolean apply(Map.Entry<String, String> entry) {
                String v = entry.getValue();
                return !Strings.isNullOrEmpty(v);
            }
        });
    }
}
