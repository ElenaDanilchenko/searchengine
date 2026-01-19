--liquibase formatted sql

--changeset root:1
CREATE TABLE IF NOT EXISTS site (
    id INT NOT NULL AUTO_INCREMENT,
    status ENUM('INDEXING','INDEXED','FAILED') NOT NULL,
    status_time DATETIME NOT NULL,
    last_error TEXT,
    url VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--changeset root:2
CREATE TABLE IF NOT EXISTS page (
    id INT NOT NULL AUTO_INCREMENT,
    site_id INT NOT NULL,
    path TEXT NOT NULL,
    code INT NOT NULL,
    content MEDIUMTEXT NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY (site_id) REFERENCES site(id) ON DELETE CASCADE,
    UNIQUE (site_id, path(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--changeset root:3
CREATE TABLE IF NOT EXISTS lemma (
    id INT NOT NULL AUTO_INCREMENT,
    site_id INT NOT NULL,
    lemma VARCHAR(255) NOT NULL,
    frequency INT NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY (site_id) REFERENCES site(id) ON DELETE CASCADE,
    UNIQUE (site_id, lemma)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--changeset root:4
CREATE TABLE IF NOT EXISTS `index` (
    id INT NOT NULL AUTO_INCREMENT,
    page_id INT NOT NULL,
    lemma_id INT NOT NULL,
    `rank` FLOAT NOT NULL,
    PRIMARY KEY(id),
    FOREIGN KEY (page_id) REFERENCES page(id) ON DELETE CASCADE,
    FOREIGN KEY (lemma_id) REFERENCES lemma(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;