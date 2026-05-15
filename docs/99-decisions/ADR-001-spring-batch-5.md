# ADR-001 - Migration Spring Batch 4 vers Spring Batch 5

## Source

Modernisation du socle kore-batch - migration de production (2018-2021) vers Java 21 / Spring Boot 3.3

## Contexte

Le socle original (2018-2021) était basé sur Spring Batch 4 / Spring Boot 2.1. Spring Batch 5, sorti avec Spring Boot 3, introduit des changements d'API non rétrocompatibles.

## Décision

Migrer vers Spring Batch 5 / Spring Boot 3.3 / Java 21.

## Changements principaux

### API de construction des jobs et steps

**Avant (SB4) :**
```java
@Autowired JobBuilderFactory jobBuilderFactory;
@Autowired StepBuilderFactory stepBuilderFactory;

jobBuilderFactory.get("monJob").start(step).build();
stepBuilderFactory.get("monStep").chunk(10).reader(r).processor(p).writer(w).build();
```

**Après (SB5) :**
```java
new JobBuilder("monJob", jobRepository).start(step).build();
new StepBuilder("monStep", jobRepository).chunk(10, transactionManager).reader(r).processor(p).writer(w).build();
```

`JobBuilderFactory` et `StepBuilderFactory` sont supprimés. Le `JobRepository` et le `TransactionManager` sont maintenant injectés explicitement.

### Auto-configuration

`@EnableBatchProcessing` n'est plus nécessaire avec Spring Boot 3 : tout est auto-configuré.

### Instanciation générique

**Avant :** `clazz.newInstance()` (déprécié Java 9+, supprimé Java 17+)

**Après :** `Supplier<T>` passé au constructeur de l'agrégateur.

## Conséquences

- API plus explicite, moins de magie
- Compatibilité Java 17+ et Java 21
- `TransactionManager` requis explicitement dans `.chunk()`
