CREATE TABLE IF NOT EXISTS `warehouse_addresses` (
  `id` bigint(20) NOT NULL,
  `pid` bigint(20) DEFAULT NULL COMMENT '父级ID',
  `name` varchar(50) DEFAULT NULL COMMENT '名称',
  `level` int(11) DEFAULT NULL COMMENT '级别',
  `pinyin` varchar(100) DEFAULT NULL COMMENT '拼音',
  `english_name` varchar(100) DEFAULT NULL COMMENT '英文名',
  `unicode_code` varchar(200) DEFAULT NULL COMMENT 'ASCII码',
  `order_no` varchar(32) DEFAULT NULL COMMENT '排序号',
  PRIMARY KEY (`id`),
  KEY `idx_warehouse_addresses_pid` (`pid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

drop table if exists `pousheng_warehouses`;

CREATE TABLE `pousheng_warehouses` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NULL COMMENT '仓库编码',
  `name` varchar(64) NOT NULL COMMENT '仓库名称',
  `type` tinyint(4)  NULL COMMENT '仓库类型',
  `status` tinyint(4) NOT NULL COMMENT '仓库状态',
  `address` varchar(128) NULL COMMENT '仓库地址',
  `owner_id` bigint(20)  NULL COMMENT '负责人id',
  `is_default` tinyint(4) NULL COMMENT '是否默认发货仓',
  `extra_json` varchar(2048) NULL COMMENT '附加信息',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_warehouse_code` (`code`)
) COMMENT='仓库';


drop table if exists `pousheng_warehouse_address_rules`;

CREATE TABLE `pousheng_warehouse_address_rules`(
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `address_id` bigint(20) NOT NULL COMMENT '地址id',
  `address_name` varchar(20) NOT NULL COMMENT '地址名称',
  `rule_id` bigint(20)  NOT NULL COMMENT '规则id',
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_warehouse_address_rules_rid` (`rule_id`)
)COMMENT='地址和仓库规则的关联';

CREATE UNIQUE INDEX idx_war_aid ON `pousheng_warehouse_address_rules` (`address_id`);



drop table if exists `pousheng_warehouse_rules`;

CREATE TABLE `pousheng_warehouse_rules` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NULL COMMENT '规则描述, 按照优先级将各仓名称拼接起来',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
)COMMENT='仓库优先级规则概述';


drop table if exists `pousheng_warehouse_rule_items`;

CREATE TABLE `pousheng_warehouse_rule_items` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `rule_id` bigint(20) NOT NULL COMMENT '规则id',
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库id',
  `name` varchar(128) NOT NULL COMMENT '仓库名称',
  `priority` tinyint(4) NOT NULL COMMENT '优先级',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_warehouse_rule_items_rid` (`rule_id`)
)COMMENT='仓库优先级规则项';



drop table if exists `pousheng_warehouse_sku_stocks`;

CREATE TABLE `pousheng_warehouse_sku_stocks` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库id',
  `shop_id` bigint(20)  NULL COMMENT '店铺id',
  `sku_code` varchar(64) NOT NULL COMMENT 'sku标识',
  `base_stock` bigint(20) NOT NULL COMMENT '同步基准库存',
  `avail_stock` bigint(20) NOT NULL COMMENT '当前可用库存',
  `locked_stock` bigint(20) NOT NULL COMMENT '当前锁定库存',
  `sync_at` datetime NULL COMMENT '上次同步校准时间',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_pousheng_warehouse_skus_code` (`sku_code`),
  KEY `idx_pousheng_wss_shop_id` (`shop_id`)
)COMMENT='sku在仓库的库存情况';


drop table if exists `pousheng_trade_express_code`;
CREATE TABLE `pousheng_trade_express_code`
(
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL COMMENT '快递商名称',
  `offical_code` varchar(64) NOT NULL COMMENT '快递官方代码',
  `pousheng_code` varchar(64) NOT NULL COMMENT '宝胜官网快递代码',
  `jd_code` varchar(64) NOT NULL COMMENT '京东快递代码',
  `taobao_code` varchar(64) NOT NULL COMMENT '淘宝快递代码',
  `suning_code` varchar(64) NOT NULL COMMENT '苏宁快递代码',
  `fenqile_code` varchar(64) NOT NULL COMMENT '分期乐代码',
  `mpos_code` varchar(64) DEFAULT NULL COMMENT 'mpos快递代码',
  `hk_code` varchar(64) NOT NULL COMMENT '恒康快递代码',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY(`id`),
  KEY `idx_express_code_name` (`name`)
)COMMENT='快递商代码';



-- 库存任务分片表
DROP TABLE IF EXISTS `pousheng_temp_sku_stock_push_partitioner`;
CREATE TABLE `pousheng_temp_sku_stock_push_partitioner` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT,
  `start`       BIGINT      NOT NULL COMMENT '开始id',
  `end`         BIGINT      NOT NULL COMMENT '截止id',
  `status`      VARCHAR(16) NOT NULL COMMENT '状态: INIT/PROCESSING/DONE',
  `machine`     VARCHAR(16) NOT NULL ,
  `created_at`  DATETIME    NOT NULL,
  `updated_at`  DATETIME    NOT NULL,
  PRIMARY KEY (`id`)
);

-- 库存任务推送表
DROP TABLE IF EXISTS `pousheng_temp_sku_stock_push_id`;
CREATE TABLE `pousheng_temp_sku_stock_push_id` (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT,
  `sku_code`        VARCHAR(40) NOT NULL COMMENT 'sku编码',
  `push_finish_at`  DATETIME    NULL COMMENT '完成推送时间',
  PRIMARY KEY (`id`)
);

