create database mybatis;
use mybatis;
create table `mybatis`.`blog` (
    `id` int(11) AUTO_INCREMENT NOT NULL,
    `title` varchar(30),
    `content` varchar(255),
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;