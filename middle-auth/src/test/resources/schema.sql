drop table if exists `pusheng_users`;

CREATE TABLE `pusheng_users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键' ,
  `out_id` bigint(20) NOT NULL COMMENT '外部用户 id',
  `name` varchar(32) NOT NULL COMMENT '用户名',
  `mobile`   VARCHAR(16)     NULL  COMMENT '手机号码',
  `extra_json`        VARCHAR(1024)   NULL        COMMENT '用户额外信息,建议json字符串',
  `created_at`        DATETIME        NOT NULL,
  `updated_at`        DATETIME        NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_psu_out_id` (out_id)
) COMMENT='用户基本信息表' ENGINE=InnoDB DEFAULT CHARSET=utf8;