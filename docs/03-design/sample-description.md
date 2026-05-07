# Description du Sample

## Objectif

`kore-batch-sample` est un exemple d'utilisation complet du socle `kore-batch`.
Il traite un fichier d'individus au format largeur fixe, valide les donnees,
persiste les resultats en base PostgreSQL et produit une synthese d'execution.

Il est concu pour etre remplace par votre propre domaine : la structure reste, le metier change.

## Cas metier simule

Traitement d'un fichier d'individus au format largeur fixe (ISO-8859-1) :
- Lecture ligne par ligne via `FlatFileItemReader` + `AssureLineMapper`
- Validation : reference presente, identifiant 10 chars commencant par 5 ou 6, nom et pays obligatoires
- Persistance : upsert dans `T_INDIVIDU` via `JdbcBatchItemWriter` (OK et KO)
- Synthese : comptage OK/KO + liste des references en erreur

## Donnees de test

Fichier `src/test/resources/data/individus_test.txt` - 200 individus fictifs :

| Cas | Nombre | Detail |
|---|---|---|
| Valides (identifiant correct) | 192 | Statut `OK` en base |
| Sans identifiant (1 sur 25) | 8 | Statut `KO` + message erreur en base |

Resultat attendu : `nbOK=192, nbKO=8, nbErreursFonctionnelles=8`

## Flux d'execution

```
java -jar kore-batch-sample.jar --inputFile=/chemin/fichier
    |
    v
IndividuBatchApplication.addJobParameters()
    | - valide que inputFile existe (InputFileValidator)
    | - ajoute timestamp pour nouvelle instance
    v
BatchHealthAggregator.checkAll()
    | - DatabaseHealthIndicator : BDD accessible ?
    | - FichierSourceHealthIndicator : repertoire source accessible ?
    | -> Si KO : arret immediat, code retour -1
    v
traitementIndividusJob
    |
    v
traitementIndividus-partition (SimplePartitioner, N threads)
    |
    +-- traitementIndividus-worker [thread 1..N]
    |       AssureItemReader.read()        (@StepScope - FlatFileItemReader)
    |       AssureItemProcessor.process()  (@StepScope)
    |           --> FunctionalException -> AssureResultDto.ko()
    |       AssureItemWriter.write()       (JdbcBatchItemWriter - upsert T_INDIVIDU)
    |       [IndividuSyntheseDto dans ExecutionContext]
    |
    v
IndividuAggregator.merge()
    | fusionne les IndividuSyntheseDto de chaque partition
    v
IndividuSyntheseDto (dans JobExecutionContext)
    | total, nbOK, nbKO, erreursFonctionnelles, referencesEnErreur
    v
BatchJobExecutionListener.afterJob()
    | log : total, OK, KO, erreurs, duree
    v
BatchLauncher.resolveExitCode()
    | System.exit(0) si OK, System.exit(-1) si erreur technique
```

## Modele de donnees

```sql
-- Table metier cree par Liquibase (v1.0.0/02-create-individu-table.xml)
CREATE TABLE T_INDIVIDU (
    ID              BIGINT PRIMARY KEY AUTO_INCREMENT,
    NUM_REFERENCE   VARCHAR(20)  NOT NULL UNIQUE,
    TYPE_REFERENCE  VARCHAR(10),
    IDENTIFIANT     VARCHAR(10),
    CIVILITE        VARCHAR(10),
    NOM_PRENOM      VARCHAR(80),
    ADRESSE_LIGNE1  VARCHAR(38),
    ADRESSE_LIGNE2  VARCHAR(38),
    CODE_PAYS       VARCHAR(5),
    PAYS            VARCHAR(27),
    DATE_REFERENCE  VARCHAR(20),
    STATUT          VARCHAR(10)  NOT NULL,   -- OK ou KO
    DATE_TRAITEMENT TIMESTAMP,
    MESSAGE_ERREUR  VARCHAR(500)
);

-- Tables Spring Batch (metadonnees - crees par 01-spring-batch-schema.xml)
BATCH_JOB_INSTANCE
BATCH_JOB_EXECUTION
BATCH_JOB_EXECUTION_PARAMS
BATCH_STEP_EXECUTION
BATCH_STEP_EXECUTION_CONTEXT
BATCH_JOB_EXECUTION_CONTEXT
```
