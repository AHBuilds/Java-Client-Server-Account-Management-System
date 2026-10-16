/* The following sql script is to create the required tables for the Database. Before
   creating the tables, make sure to create a new schema named: csc335
*/

CREATE SCHEMA IF NOT EXISTS csc335;

use csc335;

CREATE TABLE `accounts` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(45) NOT NULL,
  `password` varchar(45) NOT NULL,
  `email` varchar(45) NOT NULL,
  `active` int unsigned NOT NULL,
  `lockedout` int unsigned NOT NULL,
  `attempts` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`) /*!80000 INVISIBLE */
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `connections` (
  `user` int(10) unsigned zerofill NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`user`),
  UNIQUE KEY `user_UNIQUE` (`user`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
