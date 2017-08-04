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
 `fenqile_code`varchar(64) NOT NULL COMMENT '分期乐代码',
 `hk_code` varchar(64) NOT NULL COMMENT '恒康快递代码',
 `created_at` datetime NOT NULL,
 `updated_at` datetime NOT NULL,
  PRIMARY KEY(`id`),
  KEY `idx_express_code_name` (`name`)
)COMMENT='快递商代码';

drop table if exists `pousheng_operation_logs`;
CREATE TABLE `pousheng_operation_logs`
(
 `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
 `type` tinyint(4) NOT NULL COMMENT '类型',
 `operator_name` varchar(10) NOT NULL COMMENT '操作人名',
 `operate_id` varchar(45) NOT NULL COMMENT '操作实体的ID',
 `content` varchar(500) NOT NULL COMMENT '操作内容',
 `created_at` datetime NOT NULL,
 `updated_at` datetime NOT NULL,
  PRIMARY KEY(`id`),
  KEY `index_operator_name` (`operator_name`),
  KEY `index_operate_entity_id` (`operate_id`)
)COMMENT='操作日志表';