package com.pousheng.middle.schedule.jobs;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Description: 批次配置
 * Author: xiao
 * Date: 2018/05/29
 */
@ConfigurationProperties("pousheng.middle")
public class BatchJobProperties {

    private JobConfig stockFullDump = new JobConfig();

    public JobConfig getStockFullDump() {
        return stockFullDump;
    }

    public void setStockFullDump(JobConfig stockFullDump) {
        this.stockFullDump = stockFullDump;
    }

    public static class JobConfig {
        private Integer chunkSize = 200;
        private Integer pageSize = 200;
        private String cron;
        private Integer throttleLimit = 4;
        private Integer gridSize = 3;

        public Integer getGridSize() {
            return gridSize;
        }

        public void setGridSize(Integer gridSize) {
            this.gridSize = gridSize;
        }

        public Integer getThrottleLimit() {
            return throttleLimit;
        }

        public void setThrottleLimit(Integer throttleLimit) {
            this.throttleLimit = throttleLimit;
        }

        public Integer getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(Integer chunkSize) {
            this.chunkSize = chunkSize;
        }

        public Integer getPageSize() {
            return pageSize;
        }

        public void setPageSize(Integer pageSize) {
            this.pageSize = pageSize;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }

}
