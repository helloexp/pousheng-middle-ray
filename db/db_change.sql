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


alter table pousheng_users add column `roles_json` varchar(512) DEFAULT NULL COMMENT '用户角色信息' AFTER `mobile`;
alter table pousheng_users add column `type`  SMALLINT  NOT NULL    COMMENT '用户类型' AFTER `mobile`;
drop table if exists `pousheng_spu_materials`;

CREATE TABLE `pousheng_spu_materials` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键' ,
  `spu_id` bigint(20) NOT NULL COMMENT 'spu id',
  `material_id` varchar(32) NOT NULL COMMENT '货品id',
  `material_code` varchar(32) NOT NULL COMMENT '货品编码',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_psm_spu_id` (spu_id),
  KEY `idx_psm_material_id` (material_id)
) COMMENT='spu与material_id的关联' ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- 修改字段长度
ALTER TABLE `parana_shipments` CHANGE `extra_json` `extra_json` VARCHAR(8192);
ALTER TABLE `parana_refunds` CHANGE `extra_json` `extra_json` VARCHAR(4096);


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

drop index idx_spus_spu_code on `parana_spus`;
CREATE INDEX idx_spus_spu_code ON `parana_spus` (`spu_code`);

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
  KEY `idx_warehouse_address_rules_rid` (`rule_id`),
  KEY `idx_warehouse_address_rules_aid` (`address_id`),
  KEY `idx_warehouse_address_rules_sid` (`shop_id`)
)COMMENT='地址和仓库规则的关联';

drop table if exists `pousheng_stock_bills`;


-- 删除spu spu_code 唯一索引
drop index idx_spus_spu_code on parana_spus ;
-- 创建spu spu_code 索引
create index idx_spus_spu_code on parana_spus (spu_code) ;




ALTER TABLE `pousheng_warehouse_company_rules` DROP COLUMN `shop_id`;
ALTER TABLE `pousheng_warehouse_company_rules` DROP COLUMN `shop_name`;
-- 买家名称允许为空
ALTER TABLE `parana_shop_orders` change  `buyer_name` `buyer_name` VARCHAR(64)  NULL COMMENT '买家名称';

-- 发票信息中user_id允许为空
ALTER TABLE `parana_invoices` change  `user_id` `user_id` bigint(20)  NULL COMMENT '用户id';
-- spu归组规则新增字段rule_detail(规则详情)
ALTER TABLE `pousheng_sku_group_rules` ADD COLUMN `rule_detail` VARCHAR(20) COMMENT '规则详情' AFTER `last_start`;


drop table if exists `pousheng_stock_push_logs`;
CREATE TABLE `pousheng_stock_push_logs`
(
  `id` BIGINT(20) unsigned NOT NULL AUTO_INCREMENT,
  `shop_id`  BIGINT(20) NOT NULL  COMMENT '店铺id',
  `shop_name` VARCHAR(64) NOT NULL COMMENT '店铺名称',
  `sku_code` VARCHAR(40) NULL COMMENT 'SKU 编码 (标准库存单位编码)',
  `quantity`  BIGINT(20) NOT NULL COMMENT 'sku数量',
  `status` INT(1) NOT NULL COMMENT '1:推送成功,2:推送失败',
  `cause` VARCHAR(512) COMMENT '失败原因',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY(`id`),
  KEY `index_stock_push_shop_name` (`shop_name`),
  KEY `index_stock_push_shop_id` (`shop_id`),
  KEY `index_stock_push_sku_code` (`sku_code`)
)COMMENT='宝胜库存推送日志';
drop table if exists `pousheng_settlement_pos`;
CREATE TABLE `pousheng_settlement_pos` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pos_type` tinyint(4) NOT NULL COMMENT 'pos单类型:1.正常销售,2.售后订单',
  `ship_type` tinyint(4) NOT NULL COMMENT '发货类型:1.销售发货单,2.换货发货单,3.售后',
  `order_id` bigint(20) NOT NULL COMMENT '订单号或者售后单号',
  `shipment_id` bigint(20) DEFAULT NULL COMMENT '发货单号',
  `pos_serial_no` varchar(60) NOT NULL COMMENT 'pos单号',
  `pos_amt` bigint(20) NOT NULL COMMENT 'pos单金额',
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `shop_name` varchar(64) NOT NULL COMMENT '店铺名称',
  `pos_created_at` datetime NOT NULL COMMENT 'POS单创建时间',
  `pos_done_at` datetime NOT NULL COMMENT 'POS单完成时间',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `index_settlement_pos_serial_no` (`pos_serial_no`),
  KEY `index_settlement_order_id` (`order_id`)
) COMMENT='宝胜结算管理pos单';
CREATE TABLE `pousheng_settlement_pos`
(
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pos_type` tinyint(4) NOT NULL COMMENT 'pos单类型:1.正常销售,2.售后订单',
  `ship_type`tinyint(4) NOT NULL COMMENT '发货类型:1.销售发货单,2.换货发货单,3.售后',
  `order_id` bigint(20) NOT NULL COMMENT '发货单或售后单号',
  `pos_serial_no` VARCHAR(60)  NOT NULL COMMENT 'pos单号',
  `pos_amt` bigint(20) NOT NULL  COMMENT 'pos单金额',
  `shop_id`  BIGINT(20) NOT NULL  COMMENT '店铺id',
  `shop_name` VARCHAR(64) NOT NULL COMMENT '店铺名称',
  `pos_created_at`datetime NOT NULL COMMENT 'POS单创建时间',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY(`id`),
  KEY `index_settlement_pos_serial_no` (`pos_serial_no`),
  KEY `index_settlement_order_id` (`order_id`)
)COMMENT ='宝胜结算管理pos单';
drop table if exists `pousheng_gift_activity`;
create table `pousheng_gift_activity`
(
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL COMMENT '活动名称',
  `order_rule` tinyint(4) NOT NULL COMMENT '类型1.订单满足多少钱不限定活动商品,2.订单满足多少钱限定活动商品,3.订单满足多少件不限定活动商品,4.订单满足多少件限定活动商品',
  `order_fee` BIGINT(20)   COMMENT '满足赠品的订单金额',
  `order_quantity`  BIGINT(20) COMMENT '满足赠品的订单数量',
  `total_price`  BIGINT(20) COMMENT '赠品总的金额',
  `status` SMALLINT NOT NULL COMMENT '状态:0.未发布，1.未开始,2.进行中,3.已结束，4.已结束',
  `quantity_rule` tinyint(4) NOT NULL COMMENT '类型:1.不限制前多少人参与活动,2.限制前多少位参与活动',
  `already_activity_quantity` BIGINT(20)  COMMENT '已经有多少人参与活动',
  `activity_quantity` BIGINT(20)  COMMENT '前多少位可以参与活动,为空则不限制(前端输入0，但是后端控制输入0时不会记录)',
  `extra_json` varchar(1024) COMMENT '赠品商品信息',
  `activity_start_at` datetime NOT NULL,
  `activity_end_at` datetime NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY(`id`),
  KEY `index_middle_gift_name` (`name`)
)COMMENT='宝胜中台赠品活动表';

-- 库存表添加mpos标识,公司代码，公司名称。
alter table `pousheng_warehouses` add `is_mpos` tinyint default 0 after `is_default`;
alter table `pousheng_warehouses` add `company_id` varchar(64) after `address`;
alter table `pousheng_warehouses` add `company_name` varchar(64) after `company_id`;
alter table `pousheng_warehouses` add index `index_middle_warehouse_company` (`company_id`);

-- 增大shop表extra字段长度
alter table `parana_shops` modify column `extra_json` varchar(2048);

drop table if exists `pousheng_auto_compensation`;
CREATE TABLE `pousheng_auto_compensation` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `type` tinyint(4) NOT NULL COMMENT '任务类型 1:同步无法派单商品至mpos',
  `extra_json` varchar(2048) NOT NULL COMMENT '额外信息,json表示',
  `status` tinyint(4) NOT NULL COMMENT '0:待处理，1:已处理',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8 COMMENT='自动补偿失败任务表';


-- 库存表存储退货仓信息
alter table `pousheng_warehouses` add `tags_json` VARCHAR(2048) NULL COMMENT 'tag信息, json表示' after `extra_json`;

drop table if exists `open_push_order_task`;
CREATE TABLE `open_push_order_task` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `source_order_id` VARCHAR(200) NOT NULL COMMENT '来源单号',
  `channel` VARCHAR(50) NOT NULL COMMENT '渠道',
  `extra_json` mediumtext NOT NULL COMMENT '额外信息,json表示',
  `status` tinyint(4) NOT NULL COMMENT '0:待处理，1:已处理',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
)  COMMENT='外部订单处理失败补偿任务表';

-- 添加mpos快递码
alter table `pousheng_trade_express_code` add `mpos_code` VARCHAR(64) NULL COMMENT 'mpos快递代码' after `fenqile_code`;


drop table if exists `shipment_amount`;
CREATE TABLE `shipment_amount` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(50) COMMENT '中台发货单号',
  `buyer_nick` VARCHAR(50) COMMENT '买家昵称',
  `order_mon` VARCHAR(50) COMMENT '订单总金额',
  `fee_mon` VARCHAR(50) COMMENT '订单总运费',
  `real_mon`VARCHAR(50) COMMENT '买家应付金额',
  `shop_id` VARCHAR(50) COMMENT '下单店铺代码',
  `performance_shop_id` VARCHAR(50) COMMENT '绩效店铺代码',
  `stock_id` VARCHAR(50) COMMENT '仓库编码',
  `online_type` VARCHAR(50) COMMENT '订单来源',
  `online_order_no` VARCHAR(50) COMMENT '来源单号',
  `order_sub_no` VARCHAR(50) COMMENT '中台子订单号',
  `bar_code` VARCHAR(50) COMMENT '中台skuCode',
  `num` VARCHAR(50) COMMENT '数量',
  `preferential_mon` VARCHAR(50) COMMENT '中台折扣',
  `sale_price` VARCHAR(50) COMMENT '销售单价',
  `total_price` VARCHAR(50) COMMENT '总价',
  `hk_order_no` VARCHAR(50) COMMENT '恒康单号',
  `pos_no` VARCHAR(50) COMMENT 'pos单号',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
)  COMMENT='发货单同步恒康数据表';


drop table if exists `refund_amount`;
CREATE TABLE `refund_amount` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `refund_no`VARCHAR(50) COMMENT '中台退货单号',
  `order_no` VARCHAR(50) COMMENT '中台发货单号',
  `shop_id` VARCHAR(50) COMMENT '下单店铺代码',
  `performance_shop_id` VARCHAR(50) COMMENT '绩效店铺代码',
  `stock_id` VARCHAR(50) COMMENT '仓库编码',
  `refund_order_amount` VARCHAR(50) COMMENT '页面上的退款金额',
  `type` VARCHAR(50) COMMENT '售后类型',
  `total_refund` VARCHAR(50) COMMENT '总的退款金额',
  `online_order_no` VARCHAR(50) COMMENT '来源单号',
  `hk_order_no` VARCHAR(50) COMMENT '恒康单号',
  `pos_no` VARCHAR(50) COMMENT 'pos单号',
  `refund_sub_no` VARCHAR(50) COMMENT '中台退货子订单号',
  `order_sub_no` VARCHAR(50) COMMENT '原销售子订单号',
  `bar_code` VARCHAR(50) COMMENT '货品条码',
  `item_num` VARCHAR(50) COMMENT '售后数量',
  `sale_price` VARCHAR(50) COMMENT '销售价格',
  `refund_amount` VARCHAR(50) COMMENT '商品总进价',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='售后单同步恒康数据表';

--添加未处理原因筛选
alter table parana_shop_orders add handle_status tinyint(1) after buyer_note;

--添加无头件查询条件
alter table parana_order_shipments add spu_codes varchar(512) default null comment '货号' after order_type, add province_id bigint(20) default null comment '省份id' after spu_codes, add city_id bigint(20) default null comment '市id' after province_id, add region_id bigint(20) default null comment '区id' after city_id;

--添加订单前缀
alter table parana_shop_orders add order_code varchar(25) after id;
alter table parana_shipments add shipment_code varchar(25) after id;
alter table parana_order_shipments add shipment_code varchar(25) after shipment_id;
alter table parana_order_shipments add after_sale_order_code varchar(25) after after_sale_order_id;
alter table parana_order_shipments add order_code varchar(25) after order_id;
alter table parana_refunds add refund_code varchar(25) after id;
alter table parana_refunds add rele_order_code varchar(25) after after_sale_id;
alter table parana_order_refunds add refund_code varchar(25) after refund_id;
alter table parana_order_refunds add order_code varchar(25) after order_id;






