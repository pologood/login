-- CREATE DATABASE IF NOT EXISTS account DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci;

CREATE TABLE IF NOT EXISTS `account`  (
  `id`       BIGINT AUTO_INCREMENT PRIMARY KEY,
  `phone`    VARCHAR(19) UNIQUE,
  `email`    VARCHAR(68) UNIQUE,
  `password` CHAR(64),
  `name`     VARCHAR(32),
  `headImg`  VARCHAR(256),
  `status`   TINYINT(4),
  `incId`    INT(11),
  `perm`     BIGINT,
  `createAt` TIMESTAMP
) ENGINE=InnoDB AUTO_INCREMENT=31415926 DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `sysPerm`  (
  `id`       BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name`     VARCHAR(64),
  `desc`     TEXT,
  `status`   int,
  `createAt` TIMESTAMP
) ENGINE=InnoDB AUTO_INCREMENT=10000 DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `incPerm`  (
  `id`       BIGINT AUTO_INCREMENT PRIMARY KEY,
  `incId`    INT(11),
  `name`     VARCHAR(64),
  `desc`     TEXT,
  `createAt` TIMESTAMP,
  INDEX(`incId`)
) ENGINE=InnoDB AUTO_INCREMENT=10000000 DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `permission`  (
  `uid`      BIGINT,
  `incId`    INT(11),
  `entity`   VARCHAR(128),
  `permId`   BIGINT,
  `grant`    TINYINT(4),
  UNIQUE(`uid`, `incId`, `permId`, `entity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `address` (
  `uid`      VARCHAR(64) UNIQUE,
  `def`      INT,
  `address1` TEXT,
  `address2` TEXT,
  `address3` TEXT,
  `address4` TEXT,
  `address5` TEXT,
  `address6` TEXT,
  `address7` TEXT,
  `address8` TEXT,
  `address9` TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `openAccount` (
  `openId`    VARCHAR(64) PRIMARY KEY,
  `uid`       BIGINT,
  `nickname`  VARCHAR(32),
  `headImg`   VARCHAR(256),
  `status`    TINYINT,
  INDEX(`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
