CREATE DATABASE IF NOT EXISTS account DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

CREATE TABLE IF NOT EXISTS `account`  (
  `id`       BIGINT AUTO_INCREMENT PRIMARY KEY,
  `phone`    VARCHAR(15) UNIQUE,
  `email`    VARCHAR(64) UNIQUE,
  `password` CHAR(64),
  `name`     VARCHAR(32),
  `headImg`  VARCHAR(256),
  `status`   TINYINT(4),
  `perm`     INT(11)
) ENGINE=InnoDB AUTO_INCREMENT=31415926 DEFAULT CHARSET=utf8;                      
