CREATE TABLE `content` (
  `content_id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `url` varchar(255) NOT NULL DEFAULT '',
  `content_hash` varchar(100) NOT NULL,
  `content` longtext,
  PRIMARY KEY (`content_id`),
  UNIQUE KEY `content_hash` (`content_hash`),
  UNIQUE KEY `url` (`url`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;