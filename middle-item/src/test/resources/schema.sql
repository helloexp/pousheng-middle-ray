-- 后台类目表: parana_back_categories
drop table if exists `parana_back_categories`;
CREATE TABLE `parana_back_categories` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pid` bigint(20) NOT NULL COMMENT '父级id',
  `name` varchar(50) NOT NULL COMMENT '名称',
  `level` tinyint(1) NOT NULL COMMENT '级别',
  `status` tinyint(1) NOT NULL COMMENT '状态,1启用,-1禁用',
  `has_children` tinyint(1) NOT NULL COMMENT '是否有孩子',
  `has_spu` tinyint(1) NOT NULL COMMENT '是否有spu关联',
  `outer_id` VARCHAR(256) NULL COMMENT '外部 id',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='后台类目表';
CREATE INDEX idx_back_categories_pid ON parana_back_categories (pid);


-- 品牌表: parana_brands
drop table if exists `parana_brands`;

CREATE TABLE `parana_brands` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL COMMENT '名称',
  `unique_name` varchar(100) NOT NULL COMMENT '名称',
  `en_name` VARCHAR(100) NULL COMMENT '英文名称',
  `en_cap` CHAR(1) NULL COMMENT '首字母',
  `logo` VARCHAR(128) NULL COMMENT '品牌logo',
  `description` varchar(200)  NULL COMMENT '描述',
  `status` tinyint(1)  NULL COMMENT '状态,1启用,-1禁用',
  `outer_id` VARCHAR(256) NULL COMMENT '外部 id',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='品牌表';
CREATE INDEX idx_brands_name ON parana_brands (name);
CREATE INDEX idx_brands_en_name ON `parana_brands` (`en_name`);
CREATE INDEX idx_brands_unique_name ON `parana_brands` (`unique_name`);






-- spu表: parana_spus
drop table if exists `parana_spus`;

CREATE TABLE `parana_spus` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `spu_code` VARCHAR(40) NULL COMMENT 'spu编码',
  `category_id` int(11) UNSIGNED NOT NULL COMMENT '后台类目 ID',
  `brand_id` bigint(20)  NULL COMMENT '品牌id',
  `brand_name` varchar(100) NULL COMMENT '品牌名称',
  `name` varchar(200) NOT NULL COMMENT 'spu名称',
  `main_image` varchar(128)  NULL COMMENT '主图',
  `low_price` int(11) NULL COMMENT '实际售卖价格(所有sku的最低实际售卖价格)',
  `high_price` int(11) NULL COMMENT '实际售卖价格(所有sku的最高实际售卖价格)',
  `stock_type` TINYINT NULL COMMENT '库存类型, 0: 不分仓存储, 1: 分仓存储',
  `stock_quantity` int(11)  NULL COMMENT '库存',
  `status` tinyint(1) NOT NULL COMMENT '状态',
  `advertise` varchar(255) COMMENT '广告语',
  `specification` varchar(128) COMMENT '规格型号',
  `type` SMALLINT  NULL COMMENT 'spu类型 1为普通spu, 2为组合spu',
  `reduce_stock_type` SMALLINT DEFAULT 1 COMMENT '减库存方式, 1为拍下减库存, 2为付款减库存',
  `extra_json` VARCHAR(1024) COMMENT 'spu额外信息,建议json字符串',
  `spu_info_md5` CHAR(32) NULL COMMENT 'spu信息的m5值, 交易快照可能需要和这个摘要进行对比',
  `out_id` varchar(128) DEFAULT NULL COMMENT '外部id',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='SPU表';
CREATE UNIQUE INDEX idx_spus_spu_code ON `parana_spus` (`spu_code`);
CREATE INDEX idx_spus_cid ON `parana_spus` (`category_id`);
CREATE INDEX idx_spus_outid ON `parana_spus` (`out_id`);

-- SPU详情: parana_spu_details
drop table if exists `parana_spu_details`;

CREATE TABLE `parana_spu_details` (
  `spu_id` bigint(20) NOT NULL COMMENT 'spu id',
  `images_json` varchar(1024) DEFAULT NULL COMMENT '图片列表, json表示',
  `detail` text  NULL COMMENT '富文本详情',
  `packing_json` varchar(1024) COMMENT '包装清单,kv对, json表示',
  `service` text NULL COMMENT '售后服务',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`spu_id`)
) COMMENT='SPU详情';

-- spu属性: parana_spu_attributes
drop table if exists `parana_spu_attributes`;

CREATE TABLE `parana_spu_attributes` (
  `spu_id` bigint(20) NOT NULL COMMENT 'spu id',
  `sku_attributes` varchar(4096)  NULL COMMENT 'spu的sku属性, json存储',
  `other_attributes` varchar(8192)  NULL COMMENT 'spu的其他属性, json存储',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`spu_id`)
) COMMENT='spu属性';



-- SKU模板表: parana_skus
drop table if EXISTS `parana_sku_templates`;

CREATE TABLE `parana_sku_templates` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `sku_code` VARCHAR(40) NULL COMMENT 'SKU 编码 (标准库存单位编码)',
  `spu_id` bigint(20) NOT NULL COMMENT '商品id',
  `specification` VARCHAR(50) NULL COMMENT '型号/款式',
  `status` TINYINT(1) NOT NULL COMMENT 'sku template 状态, 1: 上架, -1:下架,  -3:删除',
  `image` varchar(128)  NULL COMMENT '图片url',
  `thumbnail` VARCHAR(128) NULL COMMENT '样本图 (SKU 缩略图) URL',
  `name` VARCHAR(100) NULL COMMENT '名称',
  `extra_price_json` VARCHAR(255)  NULL COMMENT '其他各种价格的json表示形式',
  `price` int(11) NULL COMMENT '实际售卖价格',
  `attrs_json` varchar(1024)  NULL COMMENT 'json存储的sku属性键值对',
  `stock_type` TINYINT NOT NULL COMMENT '库存类型, 0: 不分仓存储, 1: 分仓存储, (冗余自SPU表)',
  `stock_quantity` int(11) DEFAULT NULL COMMENT '库存',
  `extra`     TEXT         DEFAULT NULL COMMENT 'sku额外信息',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='SKU模板表';
CREATE INDEX idx_skutmpls_spu_id ON `parana_sku_templates` (`spu_id`);
CREATE INDEX idx_skutmpls_sku_code ON `parana_sku_templates` (`sku_code`);