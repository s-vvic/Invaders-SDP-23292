CREATE DATABASE IF NOT EXISTS invaders_db;

USE invaders_db;

CREATE TABLE IF NOT EXISTS users(
    id int AUTO_INCREMENT PRIMARY KEY NOT NULL,
    username varchar(100) NOT NULL,
    password varchar(100) NOT NULL,
    max_score int DEFALUT 0
);

--test
INSERT INTO users (username, password)
VALUES ('test', 1234)
ON DUPLICATE KEY UPDATE password='1234';