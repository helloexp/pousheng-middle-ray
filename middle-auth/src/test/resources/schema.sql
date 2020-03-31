drop table if exists `pusheng_users`;

CREATE TABLE `pousheng_users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键' ,
  `out_id` bigint(20) NOT NULL COMMENT '外部用户 id',
  `name` varchar(32) NOT NULL COMMENT '用户名',
  `mobile`   VARCHAR(16)     NULL  COMMENT '手机号码',
  `type`              SMALLINT        NOT NULL    COMMENT '用户类型',
  `roles_json`        VARCHAR(512)    NULL        COMMENT '用户角色信息',
  `extra_json`        VARCHAR(1024)   NULL        COMMENT '用户额外信息,建议json字符串',
  `created_at`        DATETIME        NOT NULL,
  `updated_at`        DATETIME        NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_psu_out_id` (out_id)
) COMMENT='用户基本信息表' ENGINE=InnoDB DEFAULT CHARSET=utf8;
CREATE INDEX idx_pousheng_users_out_id ON `pousheng_users` (`out_id`);






-- 运营角色表
CREATE TABLE `parana_operator_roles` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT,
  `name`       VARCHAR(32)   NULL COMMENT '角色名',
  `desc`       VARCHAR(256)  NULL COMMENT '角色描述',
  `status`     TINYINT       NOT NULL COMMENT '0. 未生效(冻结), 1. 生效, -1. 删除',
  `allow_json` VARCHAR(2048) NULL COMMENT '角色对应选中权限树节点列表',
  `extra_json` VARCHAR(4096) NULL COMMENT '用户额外信息,建议json字符串',
  `created_at` DATETIME      NOT NULL,
  `updated_at` DATETIME      NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT = '运营角色表';

-- 用户运营表
CREATE TABLE `parana_user_operators` (
  `id`         BIGINT NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT          NOT NULL COMMENT '用户 ID',
  `user_name`  VARCHAR(64)     NULL COMMENT '用户名 (登录名, 冗余)',
  `role_id`    BIGINT          NULL COMMENT '运营角色 ID',
  `role_name`  VARCHAR(32)     NULL COMMENT '角色名 (冗余)',
  `status`     TINYINT         NOT NULL COMMENT '运营状态 0.已冻结,1.正常',
  `extra_json` VARCHAR(4096)   NULL COMMENT '运营额外信息, 建议json字符串',
  `created_at` DATETIME        NOT NULL,
  `updated_at` DATETIME        NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT = '用户运营表';
CREATE UNIQUE INDEX idx_user_operator_user_id ON `parana_user_operators` (`user_id`);
CREATE INDEX idx_user_operator_role_id ON `parana_user_operators` (`role_id`);