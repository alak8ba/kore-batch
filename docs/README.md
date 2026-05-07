# Documentation KORE BATCH

Ce dossier contient la conception technique complète du socle KORE BATCH.

## Structure

| Dossier | Contenu |
|---|---|
| [01-context](01-context/) | Vision, acteurs, glossaire |
| [02-architecture](02-architecture/) | Architecture du socle, patterns batch |
| [03-design](03-design/) | Conception fonctionnelle, guide d'utilisation |
| [04-devops](04-devops/) | CI/CD, environnements, déploiement |
| [05-quality](05-quality/) | Stratégie de test, gestion des erreurs, audit expert |
| [06-dev](06-dev/) | Guides pratiques : setup dev, tests Windows, deploiement prod |
| [99-decisions](99-decisions/) | Décisions d'architecture (ADR) |

## Décisions d'architecture (ADR)

| ADR | Sujet |
|---|---|
| [ADR-001](99-decisions/ADR-001-spring-batch-5.md) | Migration Spring Batch 4 vers Spring Batch 5 |
| [ADR-002](99-decisions/ADR-002-github-packages.md) | GitHub Packages à la place de Nexus |
| [ADR-003](99-decisions/ADR-003-liquibase-vs-flyway.md) | Liquibase à la place de Flyway |
| [ADR-004](99-decisions/ADR-004-stepscope-vs-synchronized.md) | @StepScope vs synchronized dans le reader |
| [ADR-005](99-decisions/ADR-005-flatfileitemreader-vs-inmemory.md) | FlatFileItemReader vs chargement en mémoire |
| [ADR-006](99-decisions/ADR-006-skip-policy.md) | Pattern fonctionnel vs Skip natif Spring Batch |
| [ADR-007](99-decisions/ADR-007-java-21.md) | Choix de Java 21 comme version cible |

## Public cible

Cette documentation s'adresse aux développeurs qui souhaitent comprendre,
contribuer ou adapter le socle KORE BATCH à leur propre domaine métier.
