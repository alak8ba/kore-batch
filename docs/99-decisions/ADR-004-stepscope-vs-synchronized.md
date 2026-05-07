# ADR-004 - @StepScope vs synchronized dans le reader

## Source

Echange technique avec des experts Spring Batch le 6 mai 2026. Cet echange a permis d'identifier des axes d'amelioration sur le socle original developpe en production (2016-2019) et d'apporter les correctifs documented dans cet ADR.

## Contexte

Le socle original (2016-2019) implémentait le reader avec un mécanisme
`synchronized` pour gérer la concurrence entre les partitions parallèles.

Le reader était un bean Spring singleton. Avec le partitioning (N threads),
plusieurs threads appelaient `read()` sur la même instance. Pour éviter
les race conditions sur l'index de lecture, les méthodes étaient synchronisées.

```java
// Ancienne implémentation - Spring Batch 4
@Component
public class MonItemReader implements ItemReader<T> {

    private List<T> items;
    private int index = 0;

    @BeforeStep
    public synchronized void beforeStep(StepExecution stepExecution) {
        items = chargerFichier();
        index = 0;
    }

    @Override
    public synchronized T read() {
        if (index < items.size()) {
            return items.get(index++);
        }
        return null;
    }
}
```

## Problèmes identifiés

**1. Contention entre threads**
`synchronized` sérialise les appels à `read()`. Avec 4 partitions,
les threads se bloquent mutuellement à chaque lecture. Le gain du
parallélisme est partiellement annulé.

**2. Mauvaise architecture**
`synchronized` est un patch sur un problème de conception.
Le vrai problème est qu'un singleton ne devrait pas être partagé
entre des workers parallèles qui ont chacun leur propre état.

**3. Race condition sur beforeStep**
Si deux partitions démarrent quasi-simultanément, le `beforeStep`
de la seconde partition réinitialise `index = 0` alors que la première
est déjà en cours de lecture. Résultat : données dupliquées ou perdues.

## Décision

Adopter `@StepScope` sur le reader et le processor.

`@StepScope` est un scope Spring spécifique à Spring Batch.
Un bean `@StepScope` crée une nouvelle instance pour chaque
`StepExecution`. Avec le partitioning, chaque partition (worker)
obtient sa propre instance — pas de partage, pas besoin de verrou.

```java
// Nouvelle implémentation - Spring Batch 5 / Java 21
@Component
@StepScope // Une instance par StepExecution / partition
public class AssureItemReader implements ItemReader<AssureDto> {

    private FlatFileItemReader<AssureDto> delegate;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) throws Exception {
        String inputFile = stepExecution.getJobParameters().getString("inputFile");

        delegate = new FlatFileItemReaderBuilder<AssureDto>()
                .name("assureItemReader")
                .resource(new FileSystemResource(inputFile))
                .lineMapper(new AssureLineMapper())
                .encoding("ISO-8859-1")
                .build();

        delegate.open(new ExecutionContext());
    }

    @Override
    public AssureDto read() throws Exception {
        return delegate.read(); // Thread-safe : instance isolée
    }
}
```

## Conséquences

**Positives :**
- Pas de contention entre threads → parallélisme pleinement efficace
- Pas de race condition sur `beforeStep`
- Code plus simple et lisible
- Conforme aux conventions Spring Batch

**A noter :**
- `@StepScope` ne peut pas être utilisé sur des beans injectés
  dans un contexte hors step (ex : `@Configuration`). Dans ce cas,
  utiliser un proxy via `@Bean @StepScope` dans une `@Configuration`.
- Chaque partition lit le fichier depuis le début. Avec `SimplePartitioner`,
  toutes les partitions traitent le même fichier — le découpage se fait
  via le `FlatFileItemReader` qui est thread-safe en lecture séquentielle
  car chaque instance a son propre état de lecture.
