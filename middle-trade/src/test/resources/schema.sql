-- 复用parana部分



-- shopOrder 表: parana_shop_orders

DROP TABLE IF EXISTS `parana_shop_orders`;

CREATE TABLE `parana_shop_orders` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `order_code` varchar(25) DEFAULT NULL,
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `buyer_id` bigint(20) NOT NULL COMMENT '买家id',
  `fee` bigint(20) NOT NULL COMMENT '实付金额',
  `status` smallint(6) NOT NULL COMMENT '状态',
  `type` tinyint(4) DEFAULT NULL COMMENT '订单类型',
  `buyer_name` varchar(64) DEFAULT '' COMMENT '买家名称',
  `out_buyer_id` varchar(64) DEFAULT NULL COMMENT '买家外部id',
  `shop_name` varchar(64) NOT NULL COMMENT '店铺名称',
  `out_shop_id` varchar(64) DEFAULT NULL COMMENT '店铺外部id',
  `company_id` bigint(20) DEFAULT NULL COMMENT '公司id',
  `referer_id` bigint(20) DEFAULT NULL COMMENT '推荐人id',
  `referer_name` varchar(64) DEFAULT NULL COMMENT '推荐人名称',
  `origin_fee` bigint(20) DEFAULT NULL COMMENT '原价',
  `discount` bigint(20) DEFAULT '0',
  `ship_fee` bigint(20) DEFAULT NULL COMMENT '运费',
  `origin_ship_fee` bigint(20) DEFAULT NULL COMMENT '运费原始金额',
  `integral` int(11) DEFAULT NULL COMMENT '积分减免金额',
  `balance` int(11) DEFAULT NULL COMMENT '余额减免金额',
  `shipment_type` smallint(6) DEFAULT NULL COMMENT '配送方式',
  `pay_type` smallint(6) NOT NULL COMMENT '支付类型, 1-在线支付 2-货到付款',
  `channel` smallint(6) NOT NULL COMMENT '订单渠道 1-手机 2-pc',
  `has_refund` tinyint(4) DEFAULT NULL COMMENT '是否申请过逆向流程',
  `commented` tinyint(4) DEFAULT '0' COMMENT '是否已评价',
  `buyer_note` varchar(512) DEFAULT NULL COMMENT '买家备注',
  `handle_status` tinyint(2) DEFAULT NULL,
  `extra_json` varchar(2048) DEFAULT NULL COMMENT '子订单额外信息,json表示',
  `tags_json` varchar(2048) DEFAULT NULL COMMENT '子订单tag信息, json表示',
  `out_id` varchar(64) DEFAULT NULL COMMENT '外部订单id',
  `out_from` varchar(64) DEFAULT NULL COMMENT '外部订单来源',
  `system_auto_expired_at` datetime DEFAULT NULL COMMENT '系统未支付订单自动关闭时间',
  `out_created_at` datetime DEFAULT NULL COMMENT '外部创建时间',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `commission_rate` int(11) DEFAULT '0' COMMENT '电商平台佣金费率, 万分之一',
  `distribution_rate` int(11) DEFAULT '0' COMMENT '分销抽佣费率, 万分之一',
  `diff_fee` int(11) DEFAULT NULL COMMENT '改价金额',
  PRIMARY KEY (`id`)
)  COMMENT='店铺维度订单';
create index idx_shop_orders_shop_id on parana_shop_orders(shop_id);
create index idx_shop_orders_buyer_id on parana_shop_orders(buyer_id);

-- itemOrder 表: parana_sku_orders

DROP TABLE IF EXISTS `parana_sku_orders`;

CREATE TABLE `parana_sku_orders` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `sku_id`  BIGINT(20)  NOT NULL COMMENT 'sku id',
  `quantity`  BIGINT(20) NOT NULL COMMENT 'sku数量',
  `fee`  BIGINT(20) NOT NULL COMMENT '实付金额',
  `status` SMALLINT NOT NULL COMMENT '子订单状态,1.已支付待处理,2.处理中，待全部处理完成,3.待发货,4.待收货,5.已完成,-4.已取消,-5.取消失败,-6.撤销失败',
  `order_id`  BIGINT(20)  NOT NULL COMMENT '订单id',
  `buyer_id`  BIGINT(20)  NOT NULL COMMENT '买家id',
  `out_id`  VARCHAR(64)  NULL COMMENT '外部自订单id',
  `buyer_name`  VARCHAR(32)  NULL COMMENT '买家姓名',
  `out_buyer_id`  VARCHAR(512)  NULL COMMENT '买家外部id',
  `item_id`  BIGINT(20) NOT NULL COMMENT '商品id' ,
  `item_name`  VARCHAR(512) NOT  NULL COMMENT '商品名称',
  `sku_image`  VARCHAR(512)  NULL COMMENT 'sku主图',
  `sku_code` VARCHAR(40) NULL COMMENT 'SKU 编码 (标准库存单位编码)',
  `shop_id`  BIGINT(20) NOT NULL COMMENT '店铺id',
  `shop_name`  VARCHAR(512) NOT  NULL COMMENT '店铺名称',
  `out_shop_id`  VARCHAR(512)  NULL COMMENT '店铺外部id',
  `company_id`  BIGINT(20)  NULL COMMENT '公司id',
  `out_sku_id`  VARCHAR(64)  NULL COMMENT 'sku外部id',
  `sku_attributes`  VARCHAR(512)  NULL COMMENT 'sku属性, json表示',
  `channel`  SMALLINT  NULL COMMENT '订单渠道 1-pc, 2-手机',
  `pay_type`  SMALLINT  NULL COMMENT '支付类型 1-在线支付 2-货到付款',
  `shipment_type`  SMALLINT NULL COMMENT '配送方式',
  `origin_fee`  BIGINT(20)  NULL COMMENT '原价',
  `discount`  BIGINT(20)  NULL COMMENT '折扣',
  `ship_fee`  BIGINT(20)  NULL COMMENT '运费',
  `ship_fee_discount`  BIGINT(20)  NULL COMMENT '运费折扣',
  `integral`  INT NULL COMMENT '积分减免金额',
  `balance` INT NULL COMMENT '余额减免金额',
  `item_snapshot_id`  BIGINT  NULL COMMENT '商品快照id',
  `has_refund`  TINYINT  NULL COMMENT '是否申请过退款',
  `invoiced`  TINYINT  NULL COMMENT '是否已开具过发票',
  `commented` SMALLINT NULL COMMENT '是否已评价',
  `has_apply_after_sale` SMALLINT NULL COMMENT '是否申请过售后',
  `commission_rate` INT default 0 COMMENT '电商平台佣金费率, 万分之一',
  `distribution_rate` INT default 0 COMMENT '分销抽佣费率, 万分之一',
  `diff_fee`  INT NULL COMMENT '改价金额',
  `extra_json` VARCHAR(2048)  NULL COMMENT '子订单额外信息,json表示',
  `tags_json` VARCHAR(2048) NULL COMMENT '子订单tag信息, json表示',
  `created_at`  DATETIME  NULL COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
)  COMMENT='sku维度订单';

CREATE INDEX idx_sku_orders_buyer_id ON `parana_sku_orders` (`buyer_id`);
CREATE INDEX idx_sku_orders_shop_id ON `parana_sku_orders` (`shop_id`);
CREATE INDEX idx_sku_orders_order_id ON `parana_sku_orders` (`order_id`);



-- 发货单表: parana_shipments

DROP TABLE IF EXISTS `parana_shipments`;

CREATE TABLE `parana_shipments` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `shipment_code` varchar(25) DEFAULT NULL,
  `status` smallint(6) NOT NULL COMMENT '0: 待发货, 1:已发货, 2: 已收货, -1: 已删除',
  `type` tinyint(4) DEFAULT NULL COMMENT '发货单类型(1、销售发货 2、换货发货)',
  `ship_way` tinyint(4) DEFAULT NULL COMMENT '发货方式 1仓发 2店发',
  `ship_id` bigint(20) DEFAULT NULL COMMENT '发货仓对应店铺id',
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `shop_name` varchar(64) NOT NULL COMMENT '店铺名称',
  `shipment_serial_no` varchar(32) DEFAULT NULL COMMENT '物流单号',
  `shipment_corp_code` varchar(32) DEFAULT NULL COMMENT '物流公司编号',
  `shipment_corp_name` varchar(32) DEFAULT NULL COMMENT '物流公司名称',
  `sku_info_jsons` varchar(1024) DEFAULT NULL COMMENT '对应的skuId及数量, json表示',
  `receiver_infos` varchar(512) DEFAULT NULL COMMENT '收货人信息',
  `extra_json` varchar(8192) DEFAULT NULL,
  `tags_json` varchar(512) DEFAULT NULL COMMENT '发货单额外信息, 运营使用',
  `confirm_at` datetime DEFAULT NULL COMMENT '确认收货事件',
  `created_at` datetime DEFAULT NULL COMMENT '发货单创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '发货单更新时间',
  PRIMARY KEY (`id`)
) COMMENT='发货单表';
create index idx_shipments_ship_serial_no on parana_shipments(shipment_serial_no);


-- 发货单和(子)订单关联表
DROP TABLE IF EXISTS `parana_order_shipments`;

CREATE TABLE `parana_order_shipments` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `shipment_id` bigint(20) NOT NULL COMMENT '发货单id',
  `shipment_code` varchar(25) DEFAULT NULL,
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `shop_name` varchar(64) NOT NULL COMMENT '店铺名称',
  `after_sale_order_id` bigint(20) DEFAULT NULL COMMENT '(子)售后单id',
  `after_sale_order_code` varchar(25) DEFAULT NULL,
  `order_id` bigint(20) NOT NULL COMMENT '(子)订单id',
  `order_code` varchar(25) DEFAULT NULL,
  `order_type` smallint(6) NOT NULL COMMENT '发货订单类型 1: 店铺订单发货, 2: 子订单发货',
  `ship_way` tinyint(4) DEFAULT NULL COMMENT '发货方式 1仓发 2店发',
  `ship_id` bigint(20) DEFAULT NULL COMMENT '发货店铺ID',
  `spu_codes` varchar(512) DEFAULT NULL COMMENT '货号',
  `province_id` bigint(20) DEFAULT NULL COMMENT '省份id',
  `city_id` bigint(20) DEFAULT NULL COMMENT '市id',
  `region_id` bigint(20) DEFAULT NULL COMMENT '区id',
  `status` smallint(6) NOT NULL COMMENT '0: 待发货, 1: 已发货, -1:已删除',
  `type` tinyint(4) DEFAULT NULL COMMENT '发货单类型(1、销售发货 2、换货发货)',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT='发货单和(子)订单关联表';
create index idx_order_shipment_shipment_id on parana_order_shipments(shipment_id);
create index idx_order_shipment_order_id on parana_order_shipments(order_id);



-- 订单收货信息表

DROP TABLE IF EXISTS `parana_order_receiver_infos`;

CREATE TABLE `parana_order_receiver_infos` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `order_id`  BIGINT(20) NOT NULL COMMENT '(子)订单id',
  `order_type`  SMALLINT NOT  NULL COMMENT '1: 店铺订单, 2: 子订单',
  `receiver_info_json`  VARCHAR(512) NOT NULL COMMENT 'json表示的收货信息',
  `created_at`  DATETIME  NULL  COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
);
create index idx_order_receiver_order_id on parana_order_receiver_infos(order_id);


-- 用户发票表
DROP TABLE IF EXISTS `parana_invoices`;

CREATE TABLE `parana_invoices` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `user_id`  BIGINT(20) NOT NULL COMMENT '用户id',
  `title`  VARCHAR(128) NOT NULL COMMENT '发票title',
  `detail_json`  VARCHAR(512)  NULL COMMENT '发票详细信息',
  `status`  BIGINT(20) NOT NULL COMMENT '发票状态',
  `is_default`  tinyint NOT NULL COMMENT '是否默认',
  `created_at`  DATETIME  NULL  COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT ='用户发票表';
create index idx_invoice_user_id on parana_invoices(user_id);

--  订单发票关联表
DROP TABLE IF EXISTS `parana_order_invoices`;

CREATE TABLE `parana_order_invoices` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `invoice_id`  BIGINT(20) NOT NULL COMMENT '发票id',
  `order_id`  BIGINT(20) NOT NULL COMMENT '(子)订单id',
  `order_type`  SMALLINT NOT  NULL COMMENT '1: 店铺订单, 2: 子订单',
  `status`  BIGINT(20) NOT NULL COMMENT '0: 待开, 1: 已开, -1: 删除作废',
  `created_at`  DATETIME  NULL COMMENT '创建时间',
  `updated_at`  DATETIME  NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT ='订单发票关联表';
create index idx_oi_order_id on parana_order_invoices(order_id);

-- 退款单表
DROP TABLE IF EXISTS `parana_refunds`;
CREATE TABLE `parana_refunds` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `refund_code` varchar(25) DEFAULT NULL,
  `after_sale_id` bigint(20) DEFAULT NULL COMMENT '售后单id',
  `rele_order_code` varchar(25) DEFAULT NULL,
  `refund_type` smallint(6) NOT NULL DEFAULT '0' COMMENT '0: 售中退款, 1: 售后退款',
  `fee` bigint(20) DEFAULT NULL COMMENT '实际退款金额',
  `diff_fee` int(11) DEFAULT NULL COMMENT '改价金额',
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `shop_name` varchar(64) NOT NULL COMMENT '店铺名称',
  `buyer_id` bigint(20) NOT NULL COMMENT '买家id',
  `buyer_name` varchar(64) NOT NULL COMMENT '买家名称',
  `out_id` varchar(512) DEFAULT NULL COMMENT '外部业务id',
  `integral` bigint(20) DEFAULT NULL COMMENT '要退的积分',
  `balance` bigint(20) DEFAULT NULL COMMENT '要退的余额',
  `status` bigint(20) DEFAULT NULL COMMENT '状态',
  `refund_serial_no` varchar(512) DEFAULT NULL COMMENT '退款流水号',
  `payment_id` bigint(20) DEFAULT NULL COMMENT '对应的支付单id',
  `pay_serial_no` varchar(512) DEFAULT NULL COMMENT '对应支付单的交易流水号',
  `refund_account_no` varchar(512) DEFAULT NULL COMMENT '退款到哪个账号',
  `channel` varchar(512) DEFAULT NULL COMMENT '退款渠道',
  `promotion_id` bigint(20) DEFAULT NULL COMMENT '涉及到的平台级优惠id',
  `buyer_note` varchar(512) DEFAULT NULL COMMENT '买家备注',
  `seller_note` varchar(512) DEFAULT NULL COMMENT '商家备注',
  `extra_json` mediumtext,
  `tags_json` varchar(512) DEFAULT NULL COMMENT '标签信息, 运营使用',
  `refund_at` datetime DEFAULT NULL COMMENT '退款成功时间',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `trade_no` varchar(512) DEFAULT NULL COMMENT '对应支付单的电商平台交易流水号',
  PRIMARY KEY (`id`)
) COMMENT ='退款单表';
create index idx_refunds_shop_id on parana_refunds(shop_id);
create index idx_refunds_refund_serial_no on parana_refunds(refund_serial_no);

-- 退款单和订单关联表
DROP TABLE IF EXISTS `parana_order_refunds`;

CREATE TABLE `parana_order_refunds` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `refund_id` bigint(20) NOT NULL COMMENT '退款单id',
  `refund_code` varchar(25) DEFAULT NULL,
  `order_id` bigint(20) NOT NULL COMMENT '(子)订单id',
  `order_code` varchar(25) DEFAULT NULL,
  `order_type` smallint(6) NOT NULL COMMENT '1: 店铺订单, 2: 子订单',
  `status` bigint(20) DEFAULT NULL COMMENT '状态 0:待退款, 1:已退款, -1:删除',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) COMMENT ='退款单和订单关联表';
create index idx_order_refund_rid on parana_order_refunds(refund_id);
create index idx_order_refund_oid on parana_order_refunds(order_id);



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
)COMMENT='快递管理表';

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
  `status` SMALLINT NOT NULL COMMENT '状态:0.未发布，1.未开始,2.进行中,3.已结束，-1.已失效,-2.已删除',
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


drop table if exists `parana_address_gpss`;
CREATE TABLE `parana_address_gpss` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `business_id` bigint(20) NOT NULL COMMENT '业务ID',
  `business_type` tinyint(4) NOT NULL COMMENT '业务类型，1：门店，2：仓库',
  `longitude` varchar(32) NOT NULL COMMENT '经度',
  `latitude` varchar(32) NOT NULL DEFAULT '' COMMENT '纬度',
  `province` varchar(50) NOT NULL COMMENT '省',
  `province_id` bigint(20)  NULL COMMENT '省ID',
  `city` varchar(50)  NULL COMMENT '市',
  `city_id` bigint(20)  NULL COMMENT '市ID',
  `region` varchar(50)  NULL COMMENT '区',
  `region_id` bigint(20)  NULL COMMENT '区ID',
  `street` varchar(50) DEFAULT NULL COMMENT '街道，可以为空',
  `street_id` bigint(20) DEFAULT NULL COMMENT '街道ID，可以为空',
  `detail` varchar(256) NOT NULL COMMENT '详细地址',
  `extra_json` varchar(1024)  NULL COMMENT '额外信息',
  `created_at` datetime NOT NULL COMMENT '创建时间',
  `updated_at` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
)COMMENT='地址定位信息表';
create index idx_parana_address_gpss_business_id on parana_address_gpss(business_id);
create index idx_parana_address_gpss_type on parana_address_gpss(business_type);

drop table if exists `pousheng_auto_compensation`;
CREATE TABLE `pousheng_auto_compensation` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `type` tinyint(4) NOT NULL COMMENT '任务类型 1:同步无法派单商品至mpos',
  `time` tinyint(2) DEFAULT NULL,
  `extra_json` varchar(2048) NOT NULL COMMENT '额外信息,json表示',
  `status` tinyint(4) NOT NULL COMMENT '0:待处理，1:已处理',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
)COMMENT='自动补偿失败任务表';


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


drop table if exists `pousheng_zone_contracts`;
CREATE TABLE `pousheng_zone_contracts` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `zone_id` VARCHAR(100) NOT NULL COMMENT '区部id',
  `zone_name` VARCHAR(100) NOT NULL COMMENT '区部名称',
  `name` VARCHAR(30) NOT NULL COMMENT '联系人姓名',
  `email` VARCHAR(30) NOT NULL COMMENT '联系人邮箱',
  `phone` VARCHAR(30)  COMMENT '联系人电话',
  `group` tinyint(2) NOT NULL COMMENT '分组',
  `status` tinyint(2) NOT NULL COMMENT '状态,1可用,-1已删除',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
)  COMMENT='区部联系人表';
create index `idx_pousheng_zone_contracts_zone_name` on pousheng_zone_contracts(zone_name);
create index `idx_pousheng_zone_contracts_zone_id` on pousheng_zone_contracts(zone_id);

drop table if exists `pousheng_compensate_biz`;
CREATE TABLE `pousheng_compensate_biz` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `biz_id` VARCHAR(128) COMMENT '业务id',
  `biz_type` VARCHAR(128) NOT NULL COMMENT '业务类型',
  `context` LONGTEXT  COMMENT '明细内容',
  `status` VARCHAR(32) COMMENT '状态',
  `cnt` tinyint(4) DEFAULT 0 COMMENT '失败次数',
  `last_failed_reason` TEXT  COMMENT '上次失败原因',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
)  COMMENT='业务处理表';

drop table if exists `pousheng_stock_record_log`;
CREATE TABLE `pousheng_stock_record_log` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `shipment_id` bigint(20) COMMENT '发货单id',
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库id',
  `shop_id` bigint(20) COMMENT '店铺id',
  `sku_code` varchar(40) NOT NULL DEFAULT '' COMMENT '商品skuId',
  `context` LONGTEXT  COMMENT '明细内容',
  `type` VARCHAR(20)  COMMENT '日志类型',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
)  COMMENT='库存查询日志';

drop table if exists `parana_shipment_items`;
CREATE TABLE `parana_shipment_items` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `shipment_id` bigint(20) NOT NULL COMMENT '发货单id',
  `warehouse_id` bigint(20) NOT NULL COMMENT '仓库id',
  `shop_id` bigint(20) NOT NULL COMMENT '门店id',
  `status` smallint(6) NOT NULL COMMENT '冗余发货单状态',
  `sku_order_id` bigint(20) NOT NULL COMMENT '子订单id',
  `sku_code` varchar(40) DEFAULT NULL COMMENT 'SKU 编码 (标准库存单位编码)',
  `out_sku_code` varchar(64) DEFAULT NULL COMMENT 'sku外部id',
  `sku_out_id` varchar(64) DEFAULT NULL COMMENT '外部自订单id',
  `sku_name` varchar(512) NOT NULL COMMENT '商品名称',
  `item_id` varchar(40) DEFAULT NULL COMMENT '商品id',
  `clean_price` bigint(20) NOT NULL COMMENT '商品净价',
  `clean_fee` bigint(20) NOT NULL COMMENT '商品总净价',
  `refund_quantity` int(11) NOT NULL COMMENT '退货数量',
  `quantity` int(11) NOT NULL COMMENT '预计发货数量',
  `ship_quantity` int(11) NOT NULL COMMENT '实际发货数量',
  `occupy_quantity` int(11) NOT NULL COMMENT '发货占用数量',
  `integral` int(11) NOT NULL COMMENT '积分减免',
  `sku_price` bigint(20) DEFAULT NULL COMMENT '价格',
  `sku_discount` bigint(20) DEFAULT '0',
  `is_gift` smallint(6) DEFAULT '1' COMMENT '是否是赠品 0:不是 1:是',
  `extra_json` text,
  `share_platform_discount` int(11) DEFAULT '0' COMMENT '分摊优惠',
  `created_at` datetime DEFAULT NULL COMMENT '发货单明细创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '发货单明细更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_items_shipments_id` (`shipment_id`)
) COMMENT='发货单明细表';