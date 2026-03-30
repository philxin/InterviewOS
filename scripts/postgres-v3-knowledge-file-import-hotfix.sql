-- InterviewOS V3 hotfix for knowledge_file_import
-- Purpose:
-- 1) Add missing V3 columns (embedded_chunks, total_chunks, etc.)
-- 2) Backfill null values to safe defaults
-- 3) Align status constraint with current enum
-- 4) Keep the script idempotent (safe to run multiple times)
--
-- Usage example:
--   set -a; source /opt/interviewos/backend/.env; set +a
--   PGPASSWORD="$POSTGRES_PASSWORD" psql -h 127.0.0.1 -p 15432 -U "$POSTGRES_USERNAME" -d interviewos \
--     -f scripts/postgres-v3-knowledge-file-import-hotfix.sql

BEGIN;

CREATE TABLE IF NOT EXISTS knowledge_file_import (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    document_id UUID NULL,
    status VARCHAR(20) NOT NULL,
    default_tags TEXT NULL,
    created_count INTEGER NOT NULL DEFAULT 0,
    content_hash VARCHAR(64) NULL,
    total_chunks INTEGER NOT NULL DEFAULT 0,
    embedded_chunks INTEGER NOT NULL DEFAULT 0,
    failed_chunks INTEGER NOT NULL DEFAULT 0,
    parser_version VARCHAR(50) NULL,
    embedding_model VARCHAR(100) NULL,
    embedding_dim INTEGER NULL,
    failure_reason TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP NULL
);

ALTER TABLE knowledge_file_import
    ADD COLUMN IF NOT EXISTS document_id UUID,
    ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS total_chunks INTEGER,
    ADD COLUMN IF NOT EXISTS embedded_chunks INTEGER,
    ADD COLUMN IF NOT EXISTS failed_chunks INTEGER,
    ADD COLUMN IF NOT EXISTS parser_version VARCHAR(50),
    ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100),
    ADD COLUMN IF NOT EXISTS embedding_dim INTEGER;

-- Backfill nulls for non-null business fields.
UPDATE knowledge_file_import SET created_count = 0 WHERE created_count IS NULL;
UPDATE knowledge_file_import SET total_chunks = 0 WHERE total_chunks IS NULL;
UPDATE knowledge_file_import SET embedded_chunks = 0 WHERE embedded_chunks IS NULL;
UPDATE knowledge_file_import SET failed_chunks = 0 WHERE failed_chunks IS NULL;
UPDATE knowledge_file_import SET updated_at = COALESCE(updated_at, created_at, NOW()) WHERE updated_at IS NULL;
UPDATE knowledge_file_import SET created_at = COALESCE(created_at, NOW()) WHERE created_at IS NULL;

-- Normalize status values before adding/updating check constraints.
UPDATE knowledge_file_import
SET status = 'FAILED'
WHERE status IS NULL
   OR status NOT IN ('PENDING', 'PROCESSING', 'CHUNKING', 'EMBEDDING', 'SUCCESS', 'PARTIAL', 'FAILED');

ALTER TABLE knowledge_file_import
    ALTER COLUMN status TYPE VARCHAR(20),
    ALTER COLUMN status SET DEFAULT 'PENDING',
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN created_count SET DEFAULT 0,
    ALTER COLUMN created_count SET NOT NULL,
    ALTER COLUMN total_chunks SET DEFAULT 0,
    ALTER COLUMN total_chunks SET NOT NULL,
    ALTER COLUMN embedded_chunks SET DEFAULT 0,
    ALTER COLUMN embedded_chunks SET NOT NULL,
    ALTER COLUMN failed_chunks SET DEFAULT 0,
    ALTER COLUMN failed_chunks SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_knowledge_file_import_status'
    ) THEN
        ALTER TABLE knowledge_file_import DROP CONSTRAINT chk_knowledge_file_import_status;
    END IF;
END $$;

ALTER TABLE knowledge_file_import
    ADD CONSTRAINT chk_knowledge_file_import_status
    CHECK (status IN ('PENDING', 'PROCESSING', 'CHUNKING', 'EMBEDDING', 'SUCCESS', 'PARTIAL', 'FAILED'));

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_knowledge_file_import_user_id'
    ) THEN
        ALTER TABLE knowledge_file_import
            ADD CONSTRAINT fk_knowledge_file_import_user_id
            FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_file_import_user_created
    ON knowledge_file_import(user_id, created_at);

CREATE INDEX IF NOT EXISTS idx_file_import_status_created
    ON knowledge_file_import(status, created_at);

COMMIT;

