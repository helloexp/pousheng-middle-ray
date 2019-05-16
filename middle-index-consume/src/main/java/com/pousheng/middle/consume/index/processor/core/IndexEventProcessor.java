package com.pousheng.middle.consume.index.processor.core;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-05-16 15:25<br/>
 */
public interface IndexEventProcessor {
    void process(IndexEvent event);
}
