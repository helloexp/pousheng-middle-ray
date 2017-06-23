ALTER TABLE `parana_shops` change  `user_id` `user_id` bigint(20)  NULL COMMENT '商家id';
ALTER TABLE `parana_shops` change  `user_name` `user_name` VARCHAR(32)  NULL COMMENT '商家名称';

drop table if exists `pousheng_warehouse_shop_returns`;
CREATE TABLE `pousheng_warehouse_shop_returns` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `shop_name` varchar(128) NULL COMMENT '店铺名称',
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库id',
  `warehouse_name` varchar(128)  NULL COMMENT '仓库名称',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_wsr_shop_id` (`shop_id`)
)COMMENT='店铺的退货仓库';

drop table if exists `pusheng_spu_materials`;

CREATE TABLE `pusheng_spu_materials` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键' ,
  `spu_id` bigint(20) NOT NULL COMMENT 'spu id',
  `material_id` varchar(32) NOT NULL COMMENT '货品id',
  `material_code` varchar(32) NOT NULL COMMENT '货品编码',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_psm_spu_id` (spu_id),
  KEY `idx_psm_material_id` (material_id)
) COMMENT='spu与material_id的关联' ENGINE=InnoDB DEFAULT CHARSET=utf8;
