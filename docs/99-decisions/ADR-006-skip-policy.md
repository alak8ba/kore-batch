# ADR-006 - Pattern fonctionnel vs Skip natif Spring Batch

## Source

Echange technique avec des experts Spring Batch le 6 mai 2026. Cet echange a permis d'identifier des axes d'amelioration sur le socle original developpe en production (2016-2019) et d'apporter les correctifs documented dans cet ADR.

## Contexte

Spring Batch propose un mécanisme natif de gestion des erreurs item par item :
le **Skip**. Lorsqu'un item échoue (exception levée dans le processor ou writer),
Spring Batch peut l'ignorer et continuer avec l'item suivant, jusqu'à une limite.

Le socle kore-batch implémente une approche différente : le **pattern fonctionnel**.
Les erreurs métier ne sont jamais propagées comme exceptions — elles sont catchées
dans le processor et stockées dans une synthèse d'exécution.

## Les deux approches

### Approche 1 - Skip natif Spring Batch

```java
new StepBuilder("monStep", jobRepository)
    .<Input, Output>chunk(10, transactionManager)
    .reader(reader())
    .processor(processor())
    .writer(writer())
    .faultTolerant()
    .skip(FunctionalException.class)    // Ignorer ces exceptions
    .skipLimit(100)                      // Stopper si > 100 skips
    .noSkip(TechnicalException.class)   // Fatale : arrêter le batch
    .listener(new SkipListener<>() {
        public void onSkipInProcess(Input item, Throwable t) {
            // Logguer l'item sauté
        }
    })
    .build();
```

**Avantages :**
- Mécanisme standard Spring Batch, reconnu immédiatement
- Intégré au `JobRepository` : les skips sont tracés dans `BATCH_STEP_EXECUTION`
- `SkipListener` pour réagir à chaque skip
- `skipLimit` : sécurité automatique si trop d'erreurs

**Inconvénients :**
- Rapport d'erreurs limité (compteur global, pas de détail par item)
- Interaction avec le mécanisme de transaction/rollback
- Moins de contrôle sur ce qui est tracé dans la synthèse métier

---

### Approche 2 - Pattern fonctionnel (kore-batch)

```java
// Dans le processor
@Override
public AssureResultDto process(AssureDto assure) {
    try {
        valider(assure);
        synthese.incrementOK();
        return AssureResultDto.ok(assure);          // Résultat positif

    } catch (FunctionalException e) {
        log.warn("Erreur fonctionnelle : {}", e.getMessage());
        synthese.incrementKO();
        synthese.incrementErreurFonctionnelle();
        synthese.addNumeroEnErreur(assure.getNumPension());
        return AssureResultDto.ko(assure, e.getMessage()); // Résultat négatif
        // Pas de throw -> pas de skip -> pas de rollback
    }
}
```

**Avantages :**
- Synthèse détaillée : liste précise des items en erreur avec leur message
- Zéro interaction avec le mécanisme de transaction Spring Batch
- Contrôle total sur ce qui est loggué et tracé
- Le writer reçoit tous les items (OK et KO) et peut générer un rapport

**Inconvénients :**
- Non standard Spring Batch — nécessite une explication à un lecteur nouveau
- Les erreurs fonctionnelles n'apparaissent pas dans `BATCH_STEP_EXECUTION`
- Le `skipLimit` natif n'est pas utilisé — la limite doit être gérée manuellement

---

## Décision

Conserver le **pattern fonctionnel** comme approche principale dans kore-batch,
avec une documentation explicite du choix.

## Justification

Le socle est conçu pour des batchs de reporting et d'alimentation de données
qui nécessitent un **rapport fonctionnel détaillé** : liste des individus en erreur,
nature de l'erreur, compteurs précis par type d'erreur.

Ce niveau de détail n'est pas fourni par le Skip natif Spring Batch sans
développement supplémentaire (SkipListener + stockage custom).

## Positionnement par rapport au Skip natif

| Critère | Pattern fonctionnel | Skip natif SB |
|---|---|---|
| Rapport d'erreurs | Détaillé (par item) | Compteur global |
| Contrôle | Total | Délégué au framework |
| Convention Spring Batch | Non standard | Standard |
| Interaction transactionnelle | Aucune | Rollback par chunk |
| Limite d'erreurs | Manuelle | `skipLimit` natif |
| Cas d'usage | Reporting détaillé | Cas simples, erreurs rares |

## Quand utiliser le Skip natif

Le Skip natif est préférable quand :
- Les erreurs sont rares et exceptionnelles (pas de rapport détaillé nécessaire)
- On veut bénéficier du rollback natif Spring Batch par item
- Le projet suit strictement les conventions Spring Batch
- L'équipe est familière avec le framework et attend ce pattern

## Evolution possible

Les deux patterns sont combinables. On peut utiliser le pattern fonctionnel
pour les erreurs métier attendues, et le Skip natif pour les erreurs
techniques inattendues :

```java
new StepBuilder("monStep", jobRepository)
    .<Input, Output>chunk(10, transactionManager)
    .reader(reader())
    .processor(processor()) // Catch FunctionalException -> résultat KO
    .writer(writer())
    .faultTolerant()
    .noSkip(FunctionalException.class)  // Gérée dans le processor
    .skip(RuntimeException.class)       // Erreurs techniques imprévues
    .skipLimit(10)
    .build();
```
