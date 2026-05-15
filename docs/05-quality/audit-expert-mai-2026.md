# Audit technique - Echange avec des experts Spring Batch

**Date :** 6 mai 2026
**Contexte :** Echange technique avec des experts Spring Batch sur le socle kore-batch.
Cet échange a permis d'identifier plusieurs axes d'amélioration sur le socle original
développé en production entre 2018 et 2021.

---

## Points d'amélioration identifiés

### 1. Thread-safety du reader : synchronized vs @StepScope

**Constat :** Le reader utilisait `synchronized` pour gérer la concurrence
entre partitions parallèles. Ce pattern crée de la contention entre threads
et annule partiellement le bénéfice du partitioning.

**Décision :** Adopter `@StepScope` pour isoler chaque instance par partition.

→ voir [ADR-004 - @StepScope vs synchronized](../99-decisions/ADR-004-stepscope-vs-synchronized.md)

---

### 2. Chargement du fichier en mémoire

**Constat :** Le reader chargeait l'intégralité du fichier d'entrée dans une
`List` en mémoire avant traitement. Ce pattern ne passe pas à l'échelle sur
des fichiers volumineux (risque d'OutOfMemoryError).

**Décision :** Utiliser `FlatFileItemReader` natif Spring Batch avec
streaming ligne par ligne.

→ voir [ADR-005 - FlatFileItemReader vs chargement en mémoire](../99-decisions/ADR-005-flatfileitemreader-vs-inmemory.md)

---

### 3. Gestion des erreurs : pattern fonctionnel vs Skip natif

**Constat :** Le pattern de gestion d'erreurs (zéro exception propagée,
tout stocké dans une synthèse) n'est pas immédiatement reconnu par
les développeurs habitués aux conventions Spring Batch.

**Décision :** Documenter explicitement le choix et le positionner
par rapport au mécanisme Skip natif de Spring Batch.

→ voir [ADR-006 - Pattern fonctionnel vs Skip natif Spring Batch](../99-decisions/ADR-006-skip-policy.md)

---

### 4. Instanciation générique : Supplier<T> vs clazz.newInstance()

**Constat :** L'agrégateur utilisait `clazz.newInstance()` (réflexion Java),
déprécié depuis Java 9 et supprimé en Java 17+. Incompatible avec Java 21.

**Décision :** Remplacer par `Supplier<T>` (lambda), type-safe et sans réflexion.

→ voir [ADR-001 - Migration Spring Batch 5](../99-decisions/ADR-001-spring-batch-5.md)

---

## Bilan des corrections appliquées

| Point | Avant | Après | ADR |
|---|---|---|---|
| Thread-safety reader | `synchronized` | `@StepScope` | ADR-004 |
| Lecture fichier | `List` en mémoire | `FlatFileItemReader` streamé | ADR-005 |
| Gestion erreurs | Non documenté | Pattern explicite + comparatif Skip | ADR-006 |
| Instanciation générique | `clazz.newInstance()` | `Supplier<T>` | ADR-001 |
| Construction Job/Step | `JobBuilderFactory` | `JobBuilder` + `StepBuilder` | ADR-001 |

Toutes ces corrections ont été intégrées dans la version `1.0.0-SNAPSHOT` du socle.
