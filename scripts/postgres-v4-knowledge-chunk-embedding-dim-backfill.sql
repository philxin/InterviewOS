BEGIN;

UPDATE knowledge_chunk
SET embedding_dim = vector_dims(CAST(embedding AS vector))
WHERE embedding IS NOT NULL
  AND embedding_dim IS NULL;

COMMIT;
