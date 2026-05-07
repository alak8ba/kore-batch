-- ============================================================
-- Reset metadonnees Spring Batch uniquement
-- Garde les donnees metier intactes
-- Utile pour relancer un job avec les memes parametres
-- ============================================================

SET session_replication_role = replica;

TRUNCATE TABLE BATCH_STEP_EXECUTION_CONTEXT  RESTART IDENTITY CASCADE;
TRUNCATE TABLE BATCH_JOB_EXECUTION_CONTEXT   RESTART IDENTITY CASCADE;
TRUNCATE TABLE BATCH_STEP_EXECUTION          RESTART IDENTITY CASCADE;
TRUNCATE TABLE BATCH_JOB_EXECUTION_PARAMS    RESTART IDENTITY CASCADE;
TRUNCATE TABLE BATCH_JOB_EXECUTION           RESTART IDENTITY CASCADE;
TRUNCATE TABLE BATCH_JOB_INSTANCE            RESTART IDENTITY CASCADE;

ALTER SEQUENCE BATCH_STEP_EXECUTION_SEQ RESTART WITH 1;
ALTER SEQUENCE BATCH_JOB_EXECUTION_SEQ  RESTART WITH 1;
ALTER SEQUENCE BATCH_JOB_SEQ            RESTART WITH 1;

SET session_replication_role = DEFAULT;

SELECT 'Reset Spring Batch OK - ' || NOW() AS statut;
