package com.pousheng.middle.schedule.jobs.stock;

import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * Description: Sku第三方推送准备
 * Author: xiao
 * Date: 2018/05/29
 */
@Slf4j
public class SkuStockPushPartitionerPrepareTasklet implements Tasklet {

    private final SqlSessionTemplate sqlSessionTemplate;
    private int gridSize = 3;


    public SkuStockPushPartitionerPrepareTasklet(SqlSessionTemplate sqlSessionTemplate, int gridSize) {
        this.sqlSessionTemplate = sqlSessionTemplate;
        this.gridSize = gridSize;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution,
                                ChunkContext chunkContext) throws Exception {
        sqlSessionTemplate.update("SkuStockPushTempId.teardown");
        sqlSessionTemplate.update("SkuStockPushTempId.create");
        Long count = sqlSessionTemplate.selectOne("SkuStockPushTempId.count");

        if (log.isDebugEnabled()) {
            log.debug("Count {}", count);
        }

        Long batch = count / 3;
        Long offset = 0L;

        sqlSessionTemplate.update("SkuStockPartitioner.teardown");

        for (int i = 0; i < gridSize; i++) {

            Long start = sqlSessionTemplate.selectOne("SkuStockPushTempId.loadId", offset);
            Long end;

            offset = offset + batch;
            if (i == gridSize - 1) {
                end = sqlSessionTemplate.selectOne("SkuStockPushTempId.loadMaxId");
            } else {
                end = sqlSessionTemplate.selectOne("SkuStockPushTempId.loadId", offset);
            }


            sqlSessionTemplate.insert("SkuStockPartitioner.create",
                    ImmutableMap.of("start", start, "end", end, "status", "INIT"));

            offset = offset + 1;
        }


        return RepeatStatus.FINISHED;
    }
}