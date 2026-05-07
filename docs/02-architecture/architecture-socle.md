# Architecture du socle

## Vue d'ensemble

```
┌─────────────────────────────────────────────────────────────┐
│                      kore-batch (socle)                     │
│                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌───────────────┐  │
│  │ BatchLauncher│───►│  JobLauncher │───►│      Job      │  │
│  │  (abstract)  │    │   (Spring)   │    │   (Spring)    │  │
│  └──────────────┘    └──────────────┘    └──────┬────────┘  │
│                                                 │           │
│                                         ┌───────▼────────┐  │
│                                         │  PartitionStep │  │
│                                         └───────┬────────┘  │
│                                    ┌────────────┴──────────┐│
│                               Worker 1   ...   Worker N    ││
│                               (Reader)         (Reader)    ││
│                               (Processor)      (Processor) ││
│                               (Writer)         (Writer)    ││
│                                    └────────────┬──────────┘│
│                                    ┌────────────▼──────────┐│
│                                    │  AbstractBatchAggregator││
│                                    │   merge(global, part.) ││
│                                    └────────────┬──────────┘│
│                                    ┌────────────▼──────────┐│
│                                    │    ISynthese           ││
│                                    │  (résultat global)     ││
│                                    └───────────────────────┘│
└─────────────────────────────────────────────────────────────┘
```

## Composants du socle

### BatchLauncher

Point d'entrée abstrait. Implémente `CommandLineRunner` Spring Boot.

Responsabilités :
- Construire les `JobParameters` (timestamp + paramètres métier)
- Lancer le job via `JobLauncher`
- Résoudre le code retour depuis la synthèse d'exécution
- Appeler `System.exit()` avec le bon code

Codes retour :
| Code | Signification |
|---|---|
| `0` | Succès |
| `-1` | Erreur technique |
| `1` | Erreurs fonctionnelles bloquantes (désactivé par défaut) |

Le projet métier surcharge `addJobParameters()` pour injecter ses propres paramètres.

### AbstractBatchAggregator\<T\>

Agrège les synthèses de chaque partition en une synthèse globale.

Utilise un `Supplier<T>` (lambda) pour instancier la synthèse — remplace l'ancien `clazz.newInstance()` supprimé en Java 17+.

Le projet métier étend cette classe et implémente `merge(global, partition)`.

### ISynthese / SyntheseDto

Contrat de la synthèse d'exécution. Stockée dans le `JobExecutionContext` sous la clé `"synthese"`.

Compteurs : `nbOK`, `nbKO`, `nbDoublons`, `nbErreursTechniques`, `nbErreursFonctionnelles`.

Le projet métier étend `SyntheseDto` pour ajouter ses données métier.

### FunctionalException / TechnicalException

- `FunctionalException` (checked) : erreur métier. Le processor la catch, incrémente les compteurs, continue.
- `TechnicalException` (unchecked) : erreur infrastructure. Remonte jusqu'au `BatchLauncher`, code retour `-1`.

### BatchJobExecutionListener

Log structuré du début et de la fin du job avec durée et synthèse complète.

### CoreBatchConfiguration

Fournit le `ThreadPoolTaskExecutor` pour le partitionnement. Taille du pool configurable via `batch.partitioning.pool-size`.

## Changements Spring Batch 5 vs 4

| Spring Batch 4 | Spring Batch 5 |
|---|---|
| `JobBuilderFactory` | `new JobBuilder("nom", jobRepository)` |
| `StepBuilderFactory` | `new StepBuilder("nom", jobRepository)` |
| `@EnableBatchProcessing` obligatoire | Auto-configuré par Spring Boot 3 |
| `.chunk(10)` | `.chunk(10, transactionManager)` |
| `clazz.newInstance()` | `Supplier<T>` |
