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
