# ADR-003 - Liquibase à la place de Flyway

## Contexte

Les tables Spring Batch et les tables métier doivent être créées et versionnées de façon reproductible sur tous les environnements.

## Décision

Utiliser **Liquibase**.

## Raisons

- Standard dans les grandes organisations (banque, transport, assurance)
- Rollback natif (gratuit, contrairement à Flyway Community)
- Format XML structuré, validable par schéma XSD
- Gestion fine des changesets par auteur et par version
- Intégration Spring Boot native

## Structure des changelogs

```
db/changelog/
├── db.changelog-master.xml           <- Point d'entrée
└── versions/
    └── v1.0.0/
        ├── 01-spring-batch-schema.xml    <- Tables Spring Batch
        ├── 02-create-commande-table.xml  <- Tables métier
        └── sql/
            └── spring-batch-schema-postgresql.sql
```

## Convention de nommage

`{NN}-{description}.xml` - numéroté pour garantir l'ordre d'exécution.

Chaque `changeSet` porte l'attribut `author="alak8ba"` pour la traçabilité.

## Conséquences

- `spring.batch.jdbc.initialize-schema=never` : Liquibase gère tout
- Les scripts SQL PostgreSQL sont dans le dossier `sql/` pour être lisibles directement
