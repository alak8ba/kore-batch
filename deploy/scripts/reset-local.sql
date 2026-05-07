-- ============================================================
-- Script de reinitialisation de la base locale (dev uniquement)
-- Remet a zero les tables metadonnees Spring Batch
-- et les tables metier
-- ============================================================

-- Desactiver les contraintes FK temporairement
SET session_replication_role = replica;

-- ------------------------------------------------------------
-- Tables metier
-- ------------------------------------------------------------
TRUNCATE TABLE T_INDIVIDU RESTART IDENTITY CASCADE;

-- ------------------------------------------------------------
-- Tables metadonnees Spring Batch
-- (ordre important : respecter les FK)
-- ------------------------------------------------------------
TRUNCATE TABLE BATCH_STEP_EXECUTION_CONTEXT  RESTART IDENTITY CASCADE;
TRUNCATE TABLE BATCH_JOB_EXECUTION_CONTEXT   RESTART IDENTITY CASCADE;
TRUNCATE TABLE BATCH_STEP_EXECUTION          RESTART IDENTITY CASCADE;
TRUNCATE TABLE BATCH_JOB_EXECUTION_PARAMS    RESTART IDENTITY CASCADE;
TRUNCATE TABLE BATCH_JOB_EXECUTION           RESTART IDENTITY CASCADE;
TRUNCATE TABLE BATCH_JOB_INSTANCE            RESTART IDENTITY CASCADE;

-- Reinitialiser les sequences Spring Batch
ALTER SEQUENCE BATCH_STEP_EXECUTION_SEQ RESTART WITH 1;
ALTER SEQUENCE BATCH_JOB_EXECUTION_SEQ  RESTART WITH 1;
ALTER SEQUENCE BATCH_JOB_SEQ            RESTART WITH 1;

-- ------------------------------------------------------------
-- Tables Liquibase (tracking des changesets)
-- A inclure si les changesets ont ete renommes entre deux runs
-- ------------------------------------------------------------
TRUNCATE TABLE DATABASECHANGELOGLOCK;
DELETE FROM DATABASECHANGELOG;

-- Reactiver les contraintes FK
SET session_replication_role = DEFAULT;

-- Verification
SELECT 'BATCH_JOB_INSTANCE'         AS table_name, COUNT(*) AS nb FROM BATCH_JOB_INSTANCE
UNION ALL
SELECT 'BATCH_JOB_EXECUTION',                       COUNT(*) FROM BATCH_JOB_EXECUTION
UNION ALL
SELECT 'BATCH_STEP_EXECUTION',                      COUNT(*) FROM BATCH_STEP_EXECUTION
UNION ALL
SELECT 'T_INDIVIDU',                                COUNT(*) FROM T_INDIVIDU
ORDER BY table_name;
