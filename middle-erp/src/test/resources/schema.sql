DROP TABLE IF EXISTS `pousheng_sku_group_rules`;

CREATE TABLE `pousheng_sku_group_rules` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键' ,
  `card_id` varchar(100) NOT NULL COMMENT '品牌id',
  `kind_id` varchar(20) DEFAULT NULL COMMENT '对应的类目id',
  `rule_type` tinyint(4) NOT NULL COMMENT '规则类型, 1为按照分割符来区分色号, 2为按照末尾xx为来区分色号, 3为优先分隔符, 次为index',
  `split_char` char(1) DEFAULT NULL COMMENT '分割符',
  `last_start` tinyint(4) DEFAULT NULL COMMENT '色号起始位(从末尾开始数)',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '修改时间',
  PRIMARY KEY (`id`)
)COMMENT='' ENGINE=InnoDB DEFAULT CHARSET=utf8;

create index idx_sgr_card_id on pousheng_sku_group_rules(`card_id`);


drop table if exists `pousheng_spu_materials`;

CREATE TABLE `pousheng_spu_materials` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '自增主键' ,
  `spu_id` bigint(20) NOT NULL COMMENT 'spu id',
  `material_id` varchar(32) NOT NULL COMMENT '货品id',
  `material_code` varchar(32) NOT NULL COMMENT '货品编码',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_psm_spu_id` (spu_id),
  UNIQUE KEY `idx_psm_material_id` (material_id)
) COMMENT='' ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- 后台类目表: parana_back_categories
drop table if exists `parana_back_categories`;
CREATE TABLE `parana_back_categories` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pid` bigint(20) NOT NULL COMMENT '父级id',
  `name` varchar(50) NOT NULL COMMENT '名称',
  `level` tinyint(1) NOT NULL COMMENT '级别 1.一级类目，2.二级目录,3.三级目录,4.四级目录',
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
--create unique index idx_pbrands_outer_id on parana_brands(outer_id)

-- 类目属性表: parana_category_attributes
drop table if exists `parana_category_attributes`;

CREATE TABLE `parana_category_attributes` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `category_id` int(11) NOT NULL COMMENT '类目id',
  `attr_key` VARCHAR(20) NOT NULL COMMENT '属性名',
  `group` VARCHAR(20) NULL COMMENT '所属组名',
  `index` SMALLINT(3) NULL COMMENT '顺序编号',
  `status` tinyint(1) NOT NULL COMMENT '状态,1启用,-1删除',
  `attr_metas_json` varchar(255) NULL COMMENT 'json 格式存储的属性元信息',
  `attr_vals_json` VARCHAR(4096) NULL  COMMENT 'json 格式存储的属性值信息',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='品牌表';
CREATE INDEX idx_pca_category_id ON parana_category_attributes (category_id);

-- 前台类目表:
drop table if exists `parana_front_categories`;

CREATE TABLE `parana_front_categories` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pid` bigint(20) NOT NULL COMMENT '父级id',
  `name` varchar(50) NOT NULL COMMENT '名称',
  `level` tinyint(1)  NULL COMMENT '级别',
  `has_children` tinyint(1)  NULL COMMENT '是否有孩子',
  `logo` VARCHAR(256) NULL COMMENT 'logo',
  `outer_id` VARCHAR(256) NULL COMMENT '外部 id',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='前台类目表';
CREATE INDEX idx_front_categories_pid ON parana_front_categories (pid);

-- 前后台叶子类目映射表: parana_category_bindings
drop table if exists `parana_category_bindings`;

CREATE TABLE `parana_category_bindings` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `front_category_id` bigint(20) NOT NULL COMMENT '前台叶子类目id',
  `back_category_id` bigint(20) NOT NULL COMMENT '后台叶子类目id',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='前后台叶子类目映射表';

-- 店铺内类目表: parana_shop_categories
drop table if exists `parana_shop_categories`;

CREATE TABLE `parana_shop_categories` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `name` VARCHAR(20) NOT NULL COMMENT '类目名称',
  `logo` varchar(128) NULL COMMENT '类目logo',
  `pid` bigint(20) NOT NULL COMMENT '父级id',
  `level` tinyint(1) NOT NULL COMMENT '级别',
  `has_children` tinyint(1)  NULL COMMENT '是否有孩子',
  `has_item` tinyint(1)  NULL COMMENT '是否有商品关联',
  `index` int NULL COMMENT '排序',
  `disclosed` tinyint(1) NULL COMMENT '是否默认展开',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
)COMMENT = '店铺内类目表';
CREATE INDEX idx_shopcats_shop_id ON `parana_shop_categories` (shop_id);

-- 店铺内类目和商品关联表: parana_shop_category_items
drop table if exists `parana_shop_category_items`;

CREATE TABLE `parana_shop_category_items` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `shop_id` bigint(20) NOT NULL COMMENT '店铺id',
  `item_id` bigint(20) NOT NULL COMMENT '商品id',
  `shop_category_id` bigint(20) NOT NULL COMMENT '店铺内类目id',
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
)COMMENT = '店铺内类目表';
CREATE INDEX idx_shopcis_shop_id ON `parana_shop_category_items` (shop_id);


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
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`)
) COMMENT='SPU表';
CREATE INDEX idx_spus_spu_code ON `parana_spus` (`spu_code`);
CREATE INDEX idx_spus_cid ON `parana_spus` (`category_id`);

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
