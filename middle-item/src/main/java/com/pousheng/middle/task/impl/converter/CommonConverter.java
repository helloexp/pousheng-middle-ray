package com.pousheng.middle.task.impl.converter;

import io.terminus.common.model.Paging;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:d@terminus.io">张成栋</a> at 2019
 * @date 2019-04-09 17:16<br/>
 */
public class CommonConverter {
    public static <S, T> Paging<T> batchConvert(Paging<S> source, Function<S, T> converter) {
        if (source == null) {
            return null;
        }

        if (source.isEmpty()) {
            return Paging.empty();
        }

        Paging<T> target = new Paging<>();
        target.setTotal(source.getTotal());
        target.setData(batchConvert(source.getData(), converter));
        return target;
    }

    public static <S, T> List<T> batchConvert(List<S> source, Function<S, T> converter) {
        if (source == null) {
            return null;
        }
        return source.stream().map(converter).collect(Collectors.toList());
    }
}
