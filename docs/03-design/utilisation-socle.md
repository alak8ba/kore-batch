# Guide d'utilisation du socle

## Ajouter la dépendance

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
    <version>1.0.0</version>
</dependency>
```

Configurer l'accès dans `~/.m2/settings.xml` :

```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>VOTRE_USERNAME</username>
            <password>VOTRE_TOKEN</password>
        </server>
    </servers>
</settings>
```

## Étapes d'implémentation

### 1. Point d'entrée - étendre BatchLauncher

```java
@SpringBootApplication(scanBasePackages = {"dev.kore.batch"})
public class MonBatchApplication extends BatchLauncher {

    public MonBatchApplication(JobLauncher jobLauncher, Job job, ApplicationContext ctx) {
        super(jobLauncher, job, ctx);
    }

    public static void main(String[] args) {
        SpringApplication.run(MonBatchApplication.class, args);
    }

    @Override
    protected void addJobParameters(String[] args, JobParametersBuilder builder) {
        builder.addString("inputFile", extractArg(args, "--inputFile"));
    }
}
```

### 2. Synthèse métier - étendre SyntheseDto

```java
@Getter
public class MonSyntheseDto extends SyntheseDto {

    private final List<String> idsEnErreur = new ArrayList<>();

    public void addIdEnErreur(String id) {
        idsEnErreur.add(id);
    }

    public void merge(MonSyntheseDto other) {
        super.merge(other);
        if (other != null) idsEnErreur.addAll(other.idsEnErreur);
    }
}
```

### 3. Agrégateur - étendre AbstractBatchAggregator

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
            log.warn("Erreur fonctionnelle [{}] : {}", item.getId(), e.getMessage());
            synthese.incrementKO();
            synthese.incrementErreurFonctionnelle();
            synthese.addIdEnErreur(item.getId());
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

    @Bean
    public Job monJob(Step partitionStep) {
        return new JobBuilder("monJob", jobRepository)
                .listener(jobExecutionListener)
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
                .gridSize(4)
                .build();
    }

    @Bean
    public Step workerStep(MonItemReader reader, MonItemProcessor processor, MonItemWriter writer) {
        return new StepBuilder("monJob-worker", jobRepository)
                .<MonInputDto, MonResultDto>chunk(10, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }
}
```

## Exemple complet

Le module `kore-batch-sample` illustre l'utilisation complète du socle sur un cas métier de traitement de commandes. Il couvre :

- Lecture depuis une source (fichier / BDD)
- Validation métier avec `FunctionalException`
- Synthèse avec liste des références en erreur
- Configuration partitionnée
- Tests d'intégration avec Testcontainers
