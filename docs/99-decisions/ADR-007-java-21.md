# ADR-007 - Choix de Java 21 comme version cible

## Source

Modernisation du socle kore-batch (mai 2026) - migration depuis Java 8

## Contexte

Le socle original était développé sous Java 8 (2017-2021).
Java 21 est la dernière version LTS (Long Term Support) disponible en 2024-2026.

La migration directe Java 8 -> Java 21 est risquée. Le chemin recommandé
passe par les versions LTS intermédiaires, chacune introduisant des
breaking changes à absorber progressivement.

## Chemin de migration

```
Java 8 (LTS) → Java 11 (LTS) → Java 17 (LTS) → Java 21 (LTS)
```

### Breaking changes identifiés sur ce socle

**Java 8 → Java 11**
- API internes `sun.*` bloquées par le système de modules (Jigsaw)
- Certaines librairies (Lombok, MapStruct) nécessitent une mise à jour

**Java 11 → Java 17**
- `clazz.newInstance()` déprécié
- Sealed classes disponibles (non utilisées ici)

**Java 17 → Java 21**
- `clazz.newInstance()` **supprimé** - impact direct sur l'agrégateur générique
- Virtual Threads disponibles (Project Loom)

### Impact principal sur kore-batch

`clazz.newInstance()` utilisé dans `AbstractBatchAggregator` pour instancier
la synthèse générique. Supprimé en Java 17+, rendu impossible en Java 21.

```java
// AVANT (Java 8) - reflexion, déprécié Java 9, supprimé Java 17
T synthese = clazz.newInstance();

// APRES (Java 21) - lambda, type-safe, zero reflexion
private final Supplier<T> syntheseFactory;
T synthese = syntheseFactory.get();
// Usage : super(MonSynthese::new)
```

## Décision

Cible directe : **Java 21 LTS**.

## Justification

- Java 21 est la version LTS actuelle avec support garanti jusqu'en 2031
- Les breaking changes identifiés sont tous corrigibles (Supplier<T>, imports)
- Java 21 apporte des fonctionnalités modernes utilisées dans ce socle :
  - **Records** : `AssureResultDto` déclaré comme `record`
  - **Text blocks** : commentaires et logs multi-lignes
  - **Pattern matching** `instanceof` : simplification des casts
  - **var** : inférence de type locale
- Lombok 1.18.36 supporte Java 21 nativement

## Fonctionnalités Java 21 utilisées dans kore-batch

| Fonctionnalité | Utilisation |
|---|---|
| `record` | `AssureResultDto`, `CommandeResultDto` - DTOs immuables |
| `Supplier<T>` | `AbstractBatchAggregator` - instanciation sans réflexion |
| `var` | Inférence de type dans les méthodes locales |
| `String.isBlank()` | Validations dans le processor |
| `String.formatted()` | Messages d'erreur dans les exceptions |

## Conséquences

- Spring Boot 3.x minimum (require Java 17+, recommande Java 21)
- Lombok 1.18.32+ requis pour le support Java 21
- `lombok-mapstruct-binding` requis pour l'ordre des annotation processors
- Compatible avec les Virtual Threads (Project Loom) pour évolution future
