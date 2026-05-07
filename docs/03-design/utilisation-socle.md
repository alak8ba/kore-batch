# Guide d'utilisation du socle

## Ajouter la dependance

Dans votre `pom.xml` :

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/alak8ba/kore-batch</url>
    </repository>
</repositories>

<dependency>
    <groupId>dev.kore.batch</groupId>
    <artifactId>kore-batch</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

Configurer l'acces dans `~/.m2/settings.xml` :

```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>VOTRE_USERNAME</username>
            <password>VOTRE_TOKEN_GITHUB</password>
        </server>
    </servers>
</settings>
```

## Etapes d'implementation

### 1. Point d'entree - etendre BatchLauncher

`BatchLauncher` prend 4 arguments : `JobLauncher`, `Job`, `ApplicationContext`
et `BatchHealthAggregator` (injecte automatiquement par Spring).

```java
@SpringBootApplication(scanBasePackages = {"dev.kore.batch"})
public class MonBatchApplication extends BatchLauncher {

    public MonBatchApplication(JobLauncher jobLauncher, Job job,
                                ApplicationContext ctx,
                                BatchHealthAggregator healthAggregator) {
        super(jobLauncher, job, ctx, healthAggregator);
    }

    public static void main(String[] args) {
        SpringApplication.run(MonBatchApplication.class, args);
    }

    @Override
    protected void addJobParameters(String[] args, JobParametersBuilder builder)
            throws TechnicalException {
        // Extraire les parametres CLI et les ajouter au builder
        for (String arg : args) {
            if (arg.startsWith("--inputFile=")) {
                builder.addString("inputFile", arg.substring("--inputFile=".length()));
                return;
            }
        }
        throw new TechnicalException("Parametre obligatoire : --inputFile=...");
    }
}
```

### 2. Synthese metier - etendre SyntheseDto

```java
@Getter
public class MonSyntheseDto extends SyntheseDto {

    private final List<String> referencesEnErreur = new ArrayList<>();

    public void addReferenceEnErreur(String ref) {
        referencesEnErreur.add(ref);
    }

    public void merge(MonSyntheseDto other) {
        super.merge(other);
        if (other != null) referencesEnErreur.addAll(other.referencesEnErreur);
    }
}
```

### 3. Agregateur - etendre AbstractBatchAggregator

```java
@Component
public class MonAggregator extends AbstractBatchAggregator<MonSyntheseDto> {

    public MonAggregator() {
        super(MonSyntheseDto::new);
    }

    @Override
    protected void merge(MonSyntheseDto global, MonSyntheseDto partition) {
        global.merge(partition);
    }
}
```

### 4. Processor - gestion des erreurs fonctionnelles

```java
@Slf4j
@Component
@StepScope  // obligatoire pour le partitioning
public class MonItemProcessor implements ItemProcessor<MonInputDto, MonResultDto> {

    private MonSyntheseDto synthese;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        synthese = new MonSyntheseDto();
        stepExecution.getExecutionContext().put(ISynthese.SYNTHESE_KEY, synthese);
    }

    @Override
    public MonResultDto process(MonInputDto item) {
        try {
            valider(item);
            synthese.incrementOK();
            return MonResultDto.ok(item);
        } catch (FunctionalException e) {
            log.warn("Erreur fonctionnelle [{}] : {}", item.getReference(), e.getMessage());
            synthese.incrementKO();
            synthese.incrementErreurFonctionnelle();
            synthese.addReferenceEnErreur(item.getReference());
            return MonResultDto.ko(item, e.getMessage());
        }
    }
}
```

### 5. Configuration du job - Spring Batch 5

```java
@Configuration
@RequiredArgsConstructor
public class BatchConfiguration {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final BatchJobExecutionListener jobExecutionListener;

    @Value("${batch.chunk-size:10}")
    private int chunkSize;

    @Value("${batch.partitioning.grid-size:4}")
    private int gridSize;

    @Bean
    public Job monJob(Step partitionStep) {
        return new JobBuilder("monJob", jobRepository)
                .listener(jobExecutionListener)
                .validator(new InputFileValidator())  // valide inputFile avant lancement
                .start(partitionStep)
                .build();
    }

    @Bean
    public Step partitionStep(Step workerStep, MonAggregator aggregator) {
        return new StepBuilder("monJob-partition", jobRepository)
                .partitioner("monJob-worker", new SimplePartitioner())
                .step(workerStep)
                .aggregator(aggregator)
                .taskExecutor(taskExecutor)
                .gridSize(gridSize)
                .build();
    }

    @Bean
    public Step workerStep(MonItemReader reader, MonItemProcessor processor,
                           MonItemWriter writer, MonStepListener stepListener) {
        return new StepBuilder("monJob-worker", jobRepository)
                .<MonInputDto, MonResultDto>chunk(chunkSize, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .listener(stepListener)
                // FunctionalException geree dans le processor (pattern fonctionnel - ADR-006)
                // TechnicalException non geree -> arret immediat
                .faultTolerant()
                .noSkip(TechnicalException.class)
                .build();
    }
}
```

### 6. Health check WS externe (optionnel)

Pour verifier une dependance externe avant lancement, etendre `AbstractWSHealthIndicator` :

```java
@Component
public class MonServiceHealthIndicator extends AbstractWSHealthIndicator {

    @Override
    protected String getName() { return "MON_SERVICE"; }

    @Override
    protected boolean ping() {
        // retourner true si le service est accessible
        return monServiceClient.isAlive();
    }
}
```

Le bean est detecte automatiquement par `BatchHealthAggregator`.

## Exemple complet

Le module `kore-batch-sample` illustre l'utilisation complete du socle
sur un cas de traitement d'individus depuis un fichier largeur fixe.

Il couvre :
- Reader : `FlatFileItemReader` + `LineMapper` custom (format largeur fixe, ISO-8859-1)
- Processor : `@StepScope`, validation metier, `FunctionalException`
- Writer : `JdbcBatchItemWriter` avec upsert PostgreSQL
- Aggregateur : merge des syntheses de partitions
- Listeners : job et step
- Health check : `DatabaseHealthIndicator` + `FichierSourceHealthIndicator`
- Validation : `InputFileValidator` sur les `JobParameters`
- Tests IT : Testcontainers (vrai PostgreSQL), verification en base

Voir [sample-description.md](sample-description.md) et
[traitement-fichier-largeur-fixe.md](traitement-fichier-largeur-fixe.md).
