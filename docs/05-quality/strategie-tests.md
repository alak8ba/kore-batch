# Stratégie de tests

## Niveaux de test

### Tests unitaires (`*Test.java`)

Tests sans Spring context. Vérifient la logique métier isolée.

```bash
mvn test -pl kore-batch-sample
```

Exemples :
- Validation des règles métier dans le processor
- Comportement de la synthèse (merge, compteurs)

### Tests d'intégration (`*IT.java`)

Tests avec Spring context complet et vrai PostgreSQL via **Testcontainers**.

```bash
mvn failsafe:integration-test failsafe:verify -pl kore-batch-sample
```

Héritent de `AbstractIntegrationTest` qui démarre un conteneur PostgreSQL partagé entre tous les tests.

```java
@SpringBatchTest
class TraitementCommandesJobIT extends AbstractIntegrationTest {

    @Test
    void syntheseDoitComptabiliserLesErreursFonctionnelles() throws Exception {
        // Lance le job complet sur PostgreSQL réel
        // Vérifie la synthèse agrégée
    }
}
```

## Pourquoi Testcontainers et pas H2

H2 ne se comporte pas comme PostgreSQL sur les types de données, les contraintes, et les séquences. Les migrations Liquibase écrites pour PostgreSQL peuvent passer sur H2 et échouer en production.

Testcontainers garantit que ce qui passe en test passe en prod.

## Gestion des erreurs

### Erreur fonctionnelle

Catchée dans le processor. Le batch continue.

```java
try {
    valider(item);
} catch (FunctionalException e) {
    synthese.incrementErreurFonctionnelle();
    return ResultDto.ko(item, e.getMessage());
}
```

Code retour final : `0` (succès).

### Erreur technique

Non catchée dans le processor. Remonte au `BatchLauncher`.

Code retour final : `-1`.

### Distinction dans la synthèse

```
SYNTHESE : total=5, OK=3, KO=2, errTech=0, errFonc=2, doublons=0
```

L'orchestrateur (Control-M, cron) interprète le code retour. Les erreurs fonctionnelles sont visibles dans les logs et la synthèse sans bloquer le traitement.
