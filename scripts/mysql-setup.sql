-- One-time MySQL setup for PlaceWise. Run as a MySQL administrator, for example:
--     mysql -u root -p < scripts/mysql-setup.sql
-- Then copy db.properties.example to db.properties and put the same user/password in it.
-- The tables themselves are created by the program on first start (src/main/resources/schema.sql).

CREATE DATABASE IF NOT EXISTS placewise CHARACTER SET utf8mb4;

-- Change the password before using this anywhere but your own laptop.
CREATE USER IF NOT EXISTS 'placewise'@'localhost' IDENTIFIED BY 'change-me';
GRANT ALL PRIVILEGES ON placewise.* TO 'placewise'@'localhost';
FLUSH PRIVILEGES;
