ALTER TABLE tbl_user
    ADD COLUMN auth_provider VARCHAR(50) DEFAULT 'local' NOT NULL;

UPDATE tbl_user SET auth_provider = 'local' WHERE auth_provider IS NULL;