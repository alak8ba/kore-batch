# ADR-005 - FlatFileItemReader vs chargement en mémoire

## source

Echange technique avec des experts Spring Batch le 6 mai 2026. Cet echange a permis d'identifier des axes d'amelioration sur le socle original developpe en production (2016-2019) et d'apporter les correctifs documented dans cet ADR.

## Contexte

Le socle original chargeait l'intégralité du fichier d'entrée dans une
`List<T>` en mémoire lors du `@BeforeStep`, puis distribuait les items
aux workers via un `AtomicInteger` servant d'index.

```java
// Ancienne implémentation
@BeforeStep
public void beforeStep(StepExecution stepExecution) {
    String inputFile = stepExecution.getJobParameters().getString("inputFile");
    this.items = chargerToutLeFichier(inputFile); // Toutes les lignes en RAM
    this.index = new AtomicInteger(0);
}

@Override
public T read() {
    int i = index.getAndIncrement();
    return i < items.size() ? items.get(i) : null;
}
```

## Problèmes identifiés

**1. Risque d'OutOfMemoryError**
Un fichier de 500 000 lignes × 300 octets = ~150 Mo en mémoire.
En production, avec plusieurs batchs simultanés, cela peut saturer le heap.

**2. Latence au démarrage**
Le batch attend que tout le fichier soit chargé avant de commencer
le traitement. Sur un fichier volumineux, cela représente un délai inutile.

**3. Non conforme aux conventions Spring Batch**
Spring Batch fournit `FlatFileItemReader` précisément pour ce cas d'usage.
Réimplémenter ce mécanisme manuellement est contraire au principe
"ne pas réinventer la roue".

**4. `AtomicInteger` comme palliatif de mauvaise architecture**
L'`AtomicInteger` compensait l'absence de `@StepScope`. Avec `@StepScope`,
chaque partition a sa propre instance du reader avec son propre état —
l'`AtomicInteger` devient inutile.

## Décision

Utiliser `FlatFileItemReader` de Spring Batch avec un `LineMapper` custom
pour parser le format propriétaire (largeur fixe).

```java
@Component
@StepScope
public class AssureItemReader implements ItemReader<AssureDto> {

    private FlatFileItemReader<AssureDto> delegate;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) throws Exception {
        String inputFile = stepExecution.getJobParameters().getString("inputFile");

        // FlatFileItemReader lit ligne par ligne - pas de chargement global
        delegate = new FlatFileItemReaderBuilder<AssureDto>()
                .name("assureItemReader")
                .resource(new FileSystemResource(inputFile))
                .lineMapper(new AssureLineMapper()) // Format largeur fixe custom
                .encoding("ISO-8859-1")
                .build();

        delegate.open(new ExecutionContext());
    }

    @Override
    public AssureDto read() throws Exception {
        return delegate.read(); // Lit une ligne à la fois
    }
}
```

**LineMapper custom pour le format largeur fixe :**

```java
public class AssureLineMapper implements LineMapper<AssureDto> {

    @Override
    public AssureDto mapLine(String line, int lineNumber) {
        return AssureDto.builder()
                .numPension(extractNumPension(line))
                .nir(extractNir(line))      // 10 derniers caractères
                .nomPrenom(extractNom(line))
                .pays(extractPays(line))
                .build();
    }
}
```

## Conséquences

**Positives :**
- Empreinte mémoire constante quelle que soit la taille du fichier
- Traitement commence immédiatement (pas d'attente de chargement)
- Conforme aux conventions Spring Batch
- `FlatFileItemReader` gère nativement : encoding, skip des lignes vides,
  gestion des erreurs de parsing, restart capability

**Compromis :**
- Le format largeur fixe nécessite un `LineMapper` custom (vs CSV standard)
- `FlatFileItemReader` lit séquentiellement — le partitioning avec
  `SimplePartitioner` donne à chaque worker le fichier entier.
  Pour un vrai découpage parallèle du fichier, utiliser
  `MultiResourcePartitioner` ou `FlatFileItemReader` avec range de lignes.
