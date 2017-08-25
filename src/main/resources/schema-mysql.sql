-- Create syntax for TABLE 'article'
CREATE TABLE `article` (
  `article_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `content_id` bigint(20) unsigned NOT NULL,
  `headline` varchar(255) DEFAULT '',
  `text` longtext,
  PRIMARY KEY (`article_id`),
  UNIQUE KEY `content_id` (`content_id`),
  CONSTRAINT `article_ibfk_1` FOREIGN KEY (`content_id`) REFERENCES `content` (`content_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create syntax for TABLE 'content'
CREATE TABLE `content` (
  `content_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `content_type` varchar(100) DEFAULT '',
  `url` varchar(255) NOT NULL DEFAULT '',
  `content_hash` varchar(100) NOT NULL,
  `content` longtext,
  PRIMARY KEY (`content_id`),
  UNIQUE KEY `content_hash` (`content_hash`),
  UNIQUE KEY `url` (`url`),
  KEY `content_type` (`content_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create syntax for TABLE 'content_category'
CREATE TABLE `content_category` (
  `category_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `content_id` bigint(20) unsigned NOT NULL,
  `top_category` varchar(100) NOT NULL DEFAULT '',
  `sub_category` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`category_id`),
  KEY `content_id` (`content_id`),
  KEY `category_idx1` (`top_category`,`sub_category`),
  CONSTRAINT `content_category_ibfk_1` FOREIGN KEY (`content_id`) REFERENCES `content` (`content_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create syntax for TABLE 'content_relation'
CREATE TABLE `content_relation` (
  `content_relation_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `source_content_id` bigint(20) unsigned NOT NULL,
  `target_content_id` bigint(20) unsigned NOT NULL,
  `link_text` varchar(300) DEFAULT NULL,
  PRIMARY KEY (`content_relation_id`),
  UNIQUE KEY `content_relation_uk1` (`source_content_id`,`target_content_id`,`link_text`),
  KEY `target_content_id` (`target_content_id`),
  CONSTRAINT `content_relation_ibfk_1` FOREIGN KEY (`source_content_id`) REFERENCES `content` (`content_id`),
  CONSTRAINT `content_relation_ibfk_2` FOREIGN KEY (`target_content_id`) REFERENCES `content` (`content_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create syntax for TABLE 'paragraph'
CREATE TABLE `paragraph` (
  `paragraph_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `article_id` bigint(20) unsigned NOT NULL,
  `paragraph_seq` int(10) unsigned NOT NULL,
  `text` text NOT NULL,
  PRIMARY KEY (`paragraph_id`),
  UNIQUE KEY `article_id` (`article_id`,`paragraph_seq`),
  CONSTRAINT `paragraph_ibfk_1` FOREIGN KEY (`article_id`) REFERENCES `article` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create syntax for TABLE 'vocabulary'
CREATE TABLE `vocabulary` (
  `vocabulary_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `word` varchar(100) NOT NULL DEFAULT '',
  `pos` varchar(100) NOT NULL DEFAULT '',
  PRIMARY KEY (`vocabulary_id`),
  UNIQUE KEY `vocabulary_uk1` (`word`,`pos`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create syntax for TABLE 'vocabulary_relation'
CREATE TABLE `vocabulary_relation` (
  `vocabulary_relation_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vocabulary_id` bigint(20) unsigned NOT NULL,
  `paragraph_id` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`vocabulary_relation_id`),
  UNIQUE KEY `vocabulary_id` (`vocabulary_id`,`paragraph_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Create syntax for TABLE 'sentence'
CREATE TABLE `sentence` (
  `sentence_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `article_id` bigint(20) unsigned NOT NULL,
  `paragraph_seq` int(10) unsigned NOT NULL,
  `sentence_seq` int(10) unsigned NOT NULL,
  `text` text,
  PRIMARY KEY (`sentence_id`),
  KEY `article_id` (`article_id`),
  CONSTRAINT `sentence_ibfk_1` FOREIGN KEY (`article_id`) REFERENCES `article` (`article_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;