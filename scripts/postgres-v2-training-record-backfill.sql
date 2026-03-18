-- InterviewOS V2 optional backfill script
-- Purpose:
--   Backfill legacy `training_record` rows to V2 `training_session` + `training_question`.
-- Usage:
--   export PGPASSWORD='your-db-password'
--   psql -h 127.0.0.1 -U interviewos -d interviewos -f scripts/postgres-v2-training-record-backfill.sql
--
-- Notes:
-- 1. Run `scripts/postgres-v2-migration.sql` first.
-- 2. This script is idempotent. Re-running will skip already migrated rows.
-- 3. Legacy rows without `knowledge.user_id` are skipped automatically.

BEGIN;

CREATE TABLE IF NOT EXISTS training_record_migration_map (
    training_record_id BIGINT PRIMARY KEY,
    session_id UUID NOT NULL UNIQUE,
    question_id UUID NOT NULL UNIQUE,
    migrated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

WITH source_records AS (
    SELECT
        tr.id AS training_record_id,
        (
            substr(md5('training-session:' || tr.id::text), 1, 8) || '-' ||
            substr(md5('training-session:' || tr.id::text), 9, 4) || '-' ||
            substr(md5('training-session:' || tr.id::text), 13, 4) || '-' ||
            substr(md5('training-session:' || tr.id::text), 17, 4) || '-' ||
            substr(md5('training-session:' || tr.id::text), 21, 12)
        )::uuid AS session_id,
        (
            substr(md5('training-question:' || tr.id::text), 1, 8) || '-' ||
            substr(md5('training-question:' || tr.id::text), 9, 4) || '-' ||
            substr(md5('training-question:' || tr.id::text), 13, 4) || '-' ||
            substr(md5('training-question:' || tr.id::text), 17, 4) || '-' ||
            substr(md5('training-question:' || tr.id::text), 21, 12)
        )::uuid AS question_id
    FROM training_record tr
)
INSERT INTO training_record_migration_map (training_record_id, session_id, question_id)
SELECT
    sr.training_record_id,
    sr.session_id,
    sr.question_id
FROM source_records sr
ON CONFLICT (training_record_id) DO NOTHING;

INSERT INTO training_session (
    id,
    user_id,
    knowledge_id,
    question_type,
    difficulty,
    hint_enabled,
    status,
    total_questions,
    answered_questions,
    current_question_no,
    summary_score,
    summary_band,
    summary_major_issue,
    started_at,
    completed_at,
    created_at
)
SELECT
    map.session_id,
    k.user_id,
    tr.knowledge_id,
    'FUNDAMENTAL',
    'MEDIUM',
    TRUE,
    'COMPLETED',
    1,
    1,
    1,
    GREATEST(0, LEAST(100, tr.overall)),
    CASE
        WHEN tr.overall < 40 THEN 'UNCLEAR'
        WHEN tr.overall < 55 THEN 'INCOMPLETE'
        WHEN tr.overall < 70 THEN 'BASIC'
        WHEN tr.overall < 85 THEN 'GOOD'
        ELSE 'STRONG'
    END,
    LEFT(COALESCE(NULLIF(BTRIM(tr.weaknesses), ''), '由 V1 training_record 迁移'), 255),
    tr.created_at,
    tr.created_at,
    tr.created_at
FROM training_record_migration_map map
JOIN training_record tr ON tr.id = map.training_record_id
JOIN knowledge k ON k.id = tr.knowledge_id
WHERE k.user_id IS NOT NULL
ON CONFLICT (id) DO NOTHING;

INSERT INTO training_question (
    id,
    session_id,
    knowledge_id,
    order_no,
    parent_question_id,
    question_type,
    difficulty,
    question_text,
    hint_text,
    hint_used,
    answer_text,
    score,
    feedback_band,
    major_issue,
    missing_points,
    better_answer_approach,
    natural_example_answer,
    weak_tags,
    mastery_before,
    mastery_after,
    answered_at,
    created_at
)
SELECT
    map.question_id,
    map.session_id,
    tr.knowledge_id,
    1,
    NULL,
    'FUNDAMENTAL',
    'MEDIUM',
    tr.question,
    NULL,
    FALSE,
    tr.answer,
    GREATEST(0, LEAST(100, tr.overall)),
    CASE
        WHEN tr.overall < 40 THEN 'UNCLEAR'
        WHEN tr.overall < 55 THEN 'INCOMPLETE'
        WHEN tr.overall < 70 THEN 'BASIC'
        WHEN tr.overall < 85 THEN 'GOOD'
        ELSE 'STRONG'
    END,
    LEFT(COALESCE(NULLIF(BTRIM(tr.weaknesses), ''), '由 V1 training_record 迁移'), 255),
    tr.weaknesses,
    tr.suggestions,
    tr.example_answer,
    NULL,
    NULL,
    tr.created_at,
    tr.created_at
FROM training_record_migration_map map
JOIN training_record tr ON tr.id = map.training_record_id
JOIN knowledge k ON k.id = tr.knowledge_id
WHERE k.user_id IS NOT NULL
ON CONFLICT (id) DO NOTHING;

COMMIT;

-- Validation queries (run manually after script):
-- SELECT COUNT(*) AS legacy_count FROM training_record;
-- SELECT COUNT(*) AS migrated_count FROM training_record_migration_map;
-- SELECT COUNT(*) AS migrated_question_count
-- FROM training_question tq
-- JOIN training_record_migration_map map ON map.question_id = tq.id;
