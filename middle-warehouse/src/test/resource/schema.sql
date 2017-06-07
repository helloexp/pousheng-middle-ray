drop table if exists `pousheng_warehouses`;

CREATE TABLE `pousheng_warehouses` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NULL COMMENT '仓库编码',
  `name` varchar(64) NOT NULL COMMENT '仓库名称',
  `owner_id` bigint(20)  NULL COMMENT '负责人id',
  `is_default` tinyint(4) NULL COMMENT '是否默认发货仓',
  `extra_json` varchar(2048) NULL COMMENT '附加信息',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='仓库';


drop table if exists `pousheng_warehouse_address_rules`;

CREATE TABLE `pousheng_warehouse_address_rules`(
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `address_id` bigint(20) NOT NULL COMMENT '地址id',
  `address_type` tinyint(4) NOT NULL COMMENT '地址类型, 1: 省, 2: 市'
  `rule_id` bigint(20)  NULL COMMENT '规则id',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_warehouse_rules_wid` (`warehouse_id`),
  KEY `idx_warehouse_rules_rid` (`rule_id`)
)COMMENT='地址和仓库规则的关联';


drop table if exists `pousheng_warehouse_rules`;

CREATE TABLE `pousheng_warehouse_rules` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(128) NOT NULL COMMENT '规则描述, 按照优先级将各仓名称拼接起来',
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



drop table if exists `pousheng_warehouse_skus`;

CREATE TABLE `pousheng_warehouse_skus` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库id',
  `sku_code` varchar(64) NOT NULL COMMENT 'sku标识',
  `base_stock` bigint(20) NOT NULL COMMENT '同步基准库存',
  `avail_stock` bigint(20) NOT NULL COMMENT '当前可用库存',
  `locked_stock` bigint(20) NOT NULL COMMENT '当前锁定库存',
  `sync_at` datetime NULL COMMENT '上次同步校准时间',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_pousheng_warehouse_skus_code` (`sku_code`)
)COMMENT='sku在仓库的库存情况';


