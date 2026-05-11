# ADR-004 - @StepScope vs synchronized dans le reader

## Source

Echange technique avec des experts Spring Batch le 6 mai 2026. Cet echange a permis d'identifier des axes d'amelioration sur le socle original developpe en production (2017-2021) et d'apporter les correctifs documentés dans cet ADR.

## Contexte

Le socle original (2017-2021) implémentait le reader avec un mécanisme
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

Adopter `SynchronizedItemStreamReader` sur le reader, `@StepScope` sur le processor.

`@StepScope` est un scope Spring spécifique à Spring Batch.
Un bean `@StepScope` crée une nouvelle instance pour chaque
`StepExecution`. Avec le partitioning, chaque partition (worker)
obtient sa propre instance — pas de partage, pas besoin de verrou.

### Reader : SynchronizedItemStreamReader

`SynchronizedItemStreamReader` est le wrapper officiel Spring Batch pour
rendre un reader thread-safe. Un seul reader est partagé entre toutes les
partitions. Les appels `read()` sont synchronisés nativement par le framework.
Chaque item est lu UNE seule fois et dispatché à UN seul worker.

```java
// Nouvelle implémentation - Spring Batch 5 / Java 21
@Component // singleton - pas de @StepScope
public class AssureItemReader implements ItemReader<AssureDto> {

    private SynchronizedItemStreamReader<AssureDto> synchronizedReader;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) throws Exception {
        String inputFile = stepExecution.getJobParameters().getString("inputFile");

        FlatFileItemReader<AssureDto> delegate = new FlatFileItemReaderBuilder<AssureDto>()
                .name("assureItemReader")
                .resource(new FileSystemResource(inputFile))
                .lineMapper(new AssureLineMapper())
                .encoding("ISO-8859-1")
                .build();

        synchronizedReader = new SynchronizedItemStreamReader<>();
        synchronizedReader.setDelegate(delegate);
        synchronizedReader.open(new ExecutionContext());
    }

    @Override
    public AssureDto read() throws Exception {
        return synchronizedReader.read(); // thread-safe via wrapper Spring Batch
    }
}
```

### Processor : @StepScope

Le processor garde `@StepScope` car il maintient un état propre par partition :
la `IndividuSyntheseDto` qui comptabilise les résultats de chaque worker.

```java
@Component
@StepScope // Une synthese par partition
public class AssureItemProcessor implements ItemProcessor<AssureDto, AssureResultDto> {
    private IndividuSyntheseDto synthese; // état propre à chaque partition
    ...
}
```

## Comparaison des trois approches

| Approche | Thread-safety | Duplication | Parallelisme | Recommandation |
|---|---|---|---|---|
| `synchronized` (ancien) | Manuel | Non | Partiel (contention) | Abandonner |
| `@StepScope` seul | Isolation | Oui (x gridSize) | Oui mais données dupliquées | Pour reader AVEC état propre |
| `SynchronizedItemStreamReader` | Framework | Non | Oui, vrai parallelisme | Recommande SB5 |

## Conséquences

**Positives :**
- Vrai parallelisme : gridSize=4 + 200 lignes = 200 items traites (pas 800)
- Thread-safety delegue au framework Spring Batch
- Suppression du `synchronized` manuel
- Conforme aux recommandations Spring Batch 5

**A noter :**
- Le processor garde `@StepScope` car sa synthese est un état par partition
- `SynchronizedItemStreamReader` serialise les `read()` mais le traitement
  (processor + writer) reste parallele → gain de performance reel
