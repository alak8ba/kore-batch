# Description du Sample

## Objectif

`kore-batch-sample` est un exemple d'utilisation complet du socle `kore-batch`. Il traite des commandes clients en appliquant des règles métier, et produit une synthèse d'exécution.

Il est conçu pour être remplacé par votre propre domaine : la structure reste, le métier change.

## Cas métier simulé

Traitement d'un flux de commandes :
- Lecture d'une liste de commandes (simulée, à remplacer par `FlatFileItemReader` ou `JdbcPagingItemReader`)
- Validation : clientId non vide, montant strictement positif
- Enrichissement : mise à jour du statut
- Persistance : log (à remplacer par `JpaItemWriter` ou `JdbcBatchItemWriter`)
- Synthèse : comptage OK/KO + liste des références en erreur

## Données de test

Le reader charge 5 commandes dont 2 invalides :

| Référence | ClientId | Montant | Statut attendu |
|---|---|---|---|
| CMD-001 | CLIENT-A | 150.00 | OK |
| CMD-002 | CLIENT-B | 75.50 | OK |
| CMD-003 | (vide) | 200.00 | KO — ClientId manquant |
| CMD-004 | CLIENT-D | -10.00 | KO — Montant invalide |
| CMD-005 | CLIENT-E | 300.00 | OK |

Résultat attendu : `nbOK=3, nbKO=2, nbErreursFonctionnelles=2`

## Flux d'exécution

```
Ligne de commande
    │  --inputFile=/data/commandes.csv
    ▼
SampleBatchApplication.addJobParameters()
    │
    ▼
traitementCommandesJob
    │
    ▼
traitementCommandes-partition (SimplePartitioner, N threads)
    │
    ├── traitementCommandes-worker [thread 1]
    │       CommandeItemReader.read()
    │       CommandeItemProcessor.process()
    │           └── FunctionalException → CommandeResultDto.ko()
    │       CommandeItemWriter.write()
    │       [synthese stockée dans ExecutionContext]
    │
    ├── ... [thread 2..N]
    │
    ▼
CommandeAggregator.merge()
    │  fusionne toutes les CommandeSyntheseDto
    ▼
ISynthese stockée dans JobExecutionContext
    │
    ▼
BatchJobExecutionListener.afterJob()
    │  log : total, OK, KO, erreurs
    ▼
BatchLauncher.resolveExitCode()
    │  System.exit(0 ou -1)
```

## Modèle de données

```sql
-- Table métier créée par Liquibase
CREATE TABLE T_COMMANDE (
    ID              BIGINT PRIMARY KEY,
    REFERENCE       VARCHAR(50) NOT NULL UNIQUE,
    CLIENT_ID       VARCHAR(100) NOT NULL,
    MONTANT         DECIMAL(15,2) NOT NULL,
    DATE_COMMANDE   DATE NOT NULL,
    STATUT          VARCHAR(20) NOT NULL,
    DATE_TRAITEMENT TIMESTAMP
);

-- Tables Spring Batch (métadonnées)
-- Créées par : 01-spring-batch-schema.xml
BATCH_JOB_INSTANCE
BATCH_JOB_EXECUTION
BATCH_JOB_EXECUTION_PARAMS
BATCH_STEP_EXECUTION
BATCH_STEP_EXECUTION_CONTEXT
BATCH_JOB_EXECUTION_CONTEXT
```
