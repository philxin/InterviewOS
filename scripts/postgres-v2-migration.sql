-- InterviewOS V1 -> V2 PostgreSQL migration
-- Usage:
--   export PGPASSWORD='your-db-password'
--   psql -h 127.0.0.1 -U interviewos -d interviewos -f scripts/postgres-v2-migration.sql
-- Optional legacy backfill:
--   psql -h 127.0.0.1 -U interviewos -d interviewos -f scripts/postgres-v2-training-record-backfill.sql
--
-- Notes:
-- 1. This script is aligned with the current JPA entities in `backend/src/main/java/com/philxin/interviewos/entity`.
-- 2. `training_record` is preserved as read-only legacy source for compatibility.
-- 3. Existing V1 knowledge rows are assigned to a bootstrap user if no real user exists yet.

BEGIN;

CREATE TABLE IF NOT EXISTS app_user (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(50) NOT NULL,
    target_role VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_app_user_email ON app_user(email);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_app_user_target_role'
    ) THEN
        ALTER TABLE app_user
            ADD CONSTRAINT chk_app_user_target_role
            CHECK (
                target_role IS NULL OR target_role IN (
                    'JAVA_BACKEND', 'FRONTEND', 'FULLSTACK', 'DEVOPS', 'DATA_ENGINEER'
                )
            );
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS knowledge (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    mastery INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_created_at_kn
    ON knowledge(created_at);

ALTER TABLE knowledge
    ADD COLUMN IF NOT EXISTS user_id BIGINT,
    ADD COLUMN IF NOT EXISTS source_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP;

INSERT INTO app_user (email, password_hash, display_name, target_role, created_at, updated_at)
SELECT
    'bootstrap-migrated@example.local',
    '{invalid-password-hash-change-me}',
    'Migrated User',
    'JAVA_BACKEND',
    NOW(),
    NOW()
WHERE NOT EXISTS (
    SELECT 1
    FROM app_user
    WHERE email = 'bootstrap-migrated@example.local'
);

UPDATE knowledge
SET source_type = 'MANUAL'
WHERE source_type IS NULL;

UPDATE knowledge
SET status = 'ACTIVE'
WHERE status IS NULL;

UPDATE knowledge
SET updated_at = COALESCE(created_at, NOW())
WHERE updated_at IS NULL;

UPDATE knowledge
SET user_id = (
    SELECT id
    FROM app_user
    WHERE email = 'bootstrap-migrated@example.local'
)
WHERE user_id IS NULL;

ALTER TABLE knowledge
    ALTER COLUMN source_type SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL,
    ALTER COLUMN user_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_knowledge_user_created
    ON knowledge(user_id, created_at);

CREATE INDEX IF NOT EXISTS idx_knowledge_user_mastery
    ON knowledge(user_id, mastery, updated_at);

CREATE INDEX IF NOT EXISTS idx_knowledge_user_status_created
    ON knowledge(user_id, status, created_at);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_knowledge_user_id'
    ) THEN
        ALTER TABLE knowledge
            ADD CONSTRAINT fk_knowledge_user_id
            FOREIGN KEY (user_id)
            REFERENCES app_user(id)
            ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_knowledge_mastery_range'
    ) THEN
        ALTER TABLE knowledge
            ADD CONSTRAINT chk_knowledge_mastery_range
            CHECK (mastery >= 0 AND mastery <= 100);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_knowledge_source_type'
    ) THEN
        ALTER TABLE knowledge
            ADD CONSTRAINT chk_knowledge_source_type
            CHECK (
                source_type IN ('MANUAL', 'BATCH_IMPORT', 'FILE_IMPORT', 'ROLE_GENERATED')
            );
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_knowledge_status'
    ) THEN
        ALTER TABLE knowledge
            ADD CONSTRAINT chk_knowledge_status
            CHECK (status IN ('ACTIVE', 'ARCHIVED'));
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS knowledge_tag (
    id BIGSERIAL PRIMARY KEY,
    knowledge_id BIGINT NOT NULL,
    tag VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_knowledge_tag_knowledge_id
        FOREIGN KEY (knowledge_id) REFERENCES knowledge(id) ON DELETE CASCADE,
    CONSTRAINT uk_knowledge_tag_unique UNIQUE (knowledge_id, tag)
);

CREATE INDEX IF NOT EXISTS idx_knowledge_tag_tag
    ON knowledge_tag(tag);

CREATE TABLE IF NOT EXISTS training_session (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    knowledge_id BIGINT NOT NULL,
    question_type VARCHAR(20) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    hint_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    total_questions INTEGER NOT NULL DEFAULT 1,
    answered_questions INTEGER NOT NULL DEFAULT 0,
    current_question_no INTEGER NOT NULL DEFAULT 1,
    summary_score INTEGER NULL,
    summary_band VARCHAR(20) NULL,
    summary_major_issue VARCHAR(255) NULL,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_training_session_user_id
        FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_training_session_knowledge_id
        FOREIGN KEY (knowledge_id) REFERENCES knowledge(id) ON DELETE CASCADE,
    CONSTRAINT chk_training_session_total_questions CHECK (total_questions >= 1),
    CONSTRAINT chk_training_session_answered_questions CHECK (answered_questions >= 0),
    CONSTRAINT chk_training_session_current_question_no CHECK (current_question_no >= 1),
    CONSTRAINT chk_training_session_status CHECK (
        status IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')
    ),
    CONSTRAINT chk_training_session_question_type CHECK (
        question_type IN ('FUNDAMENTAL', 'PROJECT', 'SCENARIO')
    ),
    CONSTRAINT chk_training_session_difficulty CHECK (
        difficulty IN ('EASY', 'MEDIUM', 'HARD')
    ),
    CONSTRAINT chk_training_session_summary_score CHECK (
        summary_score IS NULL OR (summary_score >= 0 AND summary_score <= 100)
    ),
    CONSTRAINT chk_training_session_summary_band CHECK (
        summary_band IS NULL OR summary_band IN ('UNCLEAR', 'INCOMPLETE', 'BASIC', 'GOOD', 'STRONG')
    )
);

CREATE INDEX IF NOT EXISTS idx_training_session_user_created
    ON training_session(user_id, created_at);

CREATE INDEX IF NOT EXISTS idx_training_session_user_status
    ON training_session(user_id, status, created_at);

CREATE INDEX IF NOT EXISTS idx_training_session_knowledge_created
    ON training_session(knowledge_id, created_at);

CREATE TABLE IF NOT EXISTS training_question (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL,
    knowledge_id BIGINT NOT NULL,
    order_no INTEGER NOT NULL,
    parent_question_id UUID NULL,
    question_type VARCHAR(20) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    question_text TEXT NOT NULL,
    hint_text TEXT NULL,
    hint_used BOOLEAN NOT NULL DEFAULT FALSE,
    answer_text TEXT NULL,
    score INTEGER NULL,
    feedback_band VARCHAR(20) NULL,
    major_issue VARCHAR(255) NULL,
    missing_points TEXT NULL,
    better_answer_approach TEXT NULL,
    natural_example_answer TEXT NULL,
    weak_tags TEXT NULL,
    mastery_before INTEGER NULL,
    mastery_after INTEGER NULL,
    answered_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_training_question_session_id
        FOREIGN KEY (session_id) REFERENCES training_session(id) ON DELETE CASCADE,
    CONSTRAINT fk_training_question_knowledge_id
        FOREIGN KEY (knowledge_id) REFERENCES knowledge(id) ON DELETE CASCADE,
    CONSTRAINT fk_training_question_parent_id
        FOREIGN KEY (parent_question_id) REFERENCES training_question(id) ON DELETE SET NULL,
    CONSTRAINT uk_training_question_session_order UNIQUE (session_id, order_no),
    CONSTRAINT chk_training_question_order_no CHECK (order_no >= 1),
    CONSTRAINT chk_training_question_question_type CHECK (
        question_type IN ('FUNDAMENTAL', 'PROJECT', 'SCENARIO')
    ),
    CONSTRAINT chk_training_question_difficulty CHECK (
        difficulty IN ('EASY', 'MEDIUM', 'HARD')
    ),
    CONSTRAINT chk_training_question_score_range CHECK (
        score IS NULL OR (score >= 0 AND score <= 100)
    ),
    CONSTRAINT chk_training_question_feedback_band CHECK (
        feedback_band IS NULL OR feedback_band IN ('UNCLEAR', 'INCOMPLETE', 'BASIC', 'GOOD', 'STRONG')
    ),
    CONSTRAINT chk_training_question_mastery_before_range CHECK (
        mastery_before IS NULL OR (mastery_before >= 0 AND mastery_before <= 100)
    ),
    CONSTRAINT chk_training_question_mastery_after_range CHECK (
        mastery_after IS NULL OR (mastery_after >= 0 AND mastery_after <= 100)
    )
);

CREATE INDEX IF NOT EXISTS idx_training_question_knowledge_created
    ON training_question(knowledge_id, created_at);

CREATE INDEX IF NOT EXISTS idx_training_question_band_created
    ON training_question(feedback_band, created_at);

CREATE TABLE IF NOT EXISTS knowledge_file_import (
    id UUID PRIMARY KEY,
    user_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    default_tags TEXT NULL,
    created_count INTEGER NOT NULL DEFAULT 0,
    failure_reason TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP NULL
);

CREATE INDEX IF NOT EXISTS idx_file_import_user_created
    ON knowledge_file_import(user_id, created_at);

CREATE INDEX IF NOT EXISTS idx_file_import_status_created
    ON knowledge_file_import(status, created_at);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_knowledge_file_import_user_id'
    ) THEN
        ALTER TABLE knowledge_file_import
            ADD CONSTRAINT fk_knowledge_file_import_user_id
            FOREIGN KEY (user_id)
            REFERENCES app_user(id)
            ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_knowledge_file_import_status'
    ) THEN
        ALTER TABLE knowledge_file_import
            ADD CONSTRAINT chk_knowledge_file_import_status
            CHECK (status IN ('PENDING', 'PROCESSING', 'SUCCESS', 'FAILED'));
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS training_record (
    id BIGSERIAL PRIMARY KEY,
    knowledge_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    accuracy INTEGER NOT NULL,
    depth INTEGER NOT NULL,
    clarity INTEGER NOT NULL,
    overall INTEGER NOT NULL,
    strengths TEXT NULL,
    weaknesses TEXT NULL,
    suggestions TEXT NULL,
    example_answer TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_knowledge_created
    ON training_record(knowledge_id, created_at);

CREATE INDEX IF NOT EXISTS idx_created_at_tr
    ON training_record(created_at);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_knowledge_id'
    ) THEN
        ALTER TABLE training_record
            ADD CONSTRAINT fk_knowledge_id
            FOREIGN KEY (knowledge_id)
            REFERENCES knowledge(id)
            ON DELETE CASCADE;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_training_record_scores_range'
    ) THEN
        ALTER TABLE training_record
            ADD CONSTRAINT chk_training_record_scores_range
            CHECK (
                accuracy >= 0 AND accuracy <= 100
                AND depth >= 0 AND depth <= 100
                AND clarity >= 0 AND clarity <= 100
                AND overall >= 0 AND overall <= 100
            );
    END IF;
END $$;

COMMIT;
