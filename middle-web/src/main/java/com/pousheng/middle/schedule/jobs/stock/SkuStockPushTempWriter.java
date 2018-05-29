package com.pousheng.middle.schedule.jobs.stock;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Description: 库存推送写接口
 * Author: xiao
 * Date: 2018/05/29
 */
@Slf4j
public class SkuStockPushTempWriter implements ItemWriter<String> {

    private final SqlSessionTemplate sqlSessionTemplate;

    @Autowired
    public SkuStockPushTempWriter(SqlSessionTemplate sqlSessionTemplate) {
        this.sqlSessionTemplate = sqlSessionTemplate;
    }

    @Override
    public void write(List<? extends String> items) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Batch write {}", items.size());
        }


        for (String item : items) {
            int count = sqlSessionTemplate.update("SkuStockPushTempId.update", item);

            if (count == 0) {
                log.warn("Failed to update SkuStockPushTempId with skuCode {}", item);
            }
        }
    }
}
