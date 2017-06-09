/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package com.pousheng.middle.web.utils;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;

/**
 * 将富文本根据白名单过滤
 *
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-01-31
 */
public abstract class RichTextCleaner {
    public static final Whitelist whiteList = Whitelist.relaxed()
            // for supporting relative protocols '//url/img.jpg'
            .removeProtocols("img", "src", "http", "https")
            .addAttributes(":all","class")
            .addAttributes(":all","style");

    public static String safe(String richText){
        return Jsoup.clean(richText,"", whiteList);
    }
}
