# Traitement d'un fichier à largeur fixe

## Contexte

Le sample `kore-batch-sample` traite un fichier au format **largeur fixe**.
Ce format est courant dans les échanges inter-systèmes (mainframes, legacy),
notamment dans les secteurs banque, assurance et retraite.

Chaque ligne du fichier représente un individu. Les champs ne sont pas
séparés par des délimiteurs (pas de `,` ou `;`) - ils occupent des positions
fixes dans la ligne.

## Format des lignes

```
KORE001     TYPE-A   *IND0000001*MME MARTIN CLAIRE               12 RUE DES ALPES  ...2650112345678
|___________|________|____________|__________________________|...  |_______________|
 Code (12)   Type(9)  *Ref*(12)    Civilite + Nom (32)            Identifiant (10 derniers)
```

**Champs extraits par le `AssureLineMapper` :**

| Champ | Extraction | Validation |
|---|---|---|
| `reference` | Entre les deux `*` | Obligatoire |
| `typeFlux` | Position 13-21 | Information |
| `civilite` | Apres le 2eme `*` (MME, M, MLE) | Information |
| `nomPrenom` | Apres civilite, ~32 chars | Obligatoire |
| `adresseLigne1` | Position variable | Information |
| `codePays` | Pattern `99999` | Information |
| `pays` | 27 chars apres `99999` | Obligatoire |
| `identifiant` | 10 derniers caracteres | Obligatoire, commence par 5 ou 6 |

## Pourquoi les 10 derniers caracteres pour l'identifiant ?

L'identifiant d'un individu fait exactement 10 caracteres
et est positionne en fin de ligne dans ce format. Sa position en fin de ligne
permet de l'extraire de facon fiable sans connaitre exactement la position
de tous les champs intermediaires (adresse de longueur variable).

**Format de l'identifiant (10 caracteres) :**
```
2 65 01 001 199 78
|  |  |  |   |   └ Numero d'ordre (2 chiffres)
|  |  |  |   └──── Sequence (3 chiffres)
|  |  |  └──────── Zone geographique (3 chiffres)
|  |  └─────────── Mois (2 chiffres)
|  └────────────── Annee (2 chiffres)
└───────────────── Categorie : 5 ou 6
```

## Pipeline de traitement

```
Fichier largeur fixe (ISO-8859-1)
        |
        v
AssureItemReader (@StepScope)
  └── FlatFileItemReader
      └── AssureLineMapper.mapLine()
              |  - extrait reference entre *
              |  - extrait identifiant (10 derniers chars)
              |  - extrait pays (apres code 99999)
              v
         AssureDto
              |
              v
AssureItemProcessor (@StepScope)
  └── valider()
      ├── reference presente ?          -> FunctionalException si absent
      ├── identifiant present ?         -> FunctionalException si absent
      ├── identifiant = 10 chars ?      -> FunctionalException si invalide
      ├── identifiant commence par 5/6? -> FunctionalException si invalide
      ├── nom present ?                 -> FunctionalException si absent
      └── pays present ?                -> FunctionalException si absent
              |
      +-------+--------+
      | OK             | KO
      v                v
AssureResultDto.ok()  AssureResultDto.ko()
synthese.incrementOK  synthese.incrementKO
                      synthese.addReferenceEnErreur()
              |
              v
AssureItemWriter (JdbcBatchItemWriter)
  └── UPSERT dans T_INDIVIDU (PostgreSQL)
      ├── STATUT = 'OK' si traitement reussi
      └── STATUT = 'KO' + MESSAGE_ERREUR si erreur fonctionnelle
      ON CONFLICT (NUM_REFERENCE) DO UPDATE
              |
              v
IndividuAggregator (apres toutes les partitions)
  └── merge() des syntheses de chaque partition
              |
              v
IndividuSyntheseDto (stockee dans JobExecutionContext)
  ├── total, nbOK, nbKO, erreursFonctionnelles
  └── liste des references en erreur
```

## Table de persistance : T_INDIVIDU

```sql
CREATE TABLE T_INDIVIDU (
    ID              BIGINT PRIMARY KEY AUTO_INCREMENT,
    NUM_REFERENCE   VARCHAR(20)  NOT NULL UNIQUE,  -- reference entre *
    TYPE_REFERENCE  VARCHAR(10),                   -- type de flux
    IDENTIFIANT     VARCHAR(10),                   -- identifiant national
    CIVILITE        VARCHAR(10),
    NOM_PRENOM      VARCHAR(80),
    ADRESSE_LIGNE1  VARCHAR(38),
    ADRESSE_LIGNE2  VARCHAR(38),
    CODE_PAYS       VARCHAR(5),
    PAYS            VARCHAR(27),
    DATE_REFERENCE  VARCHAR(20),
    STATUT          VARCHAR(10)  NOT NULL,          -- OK ou KO
    DATE_TRAITEMENT TIMESTAMP,
    MESSAGE_ERREUR  VARCHAR(500)                   -- rempli si KO
);
```

Le writer utilise un **upsert PostgreSQL** (`ON CONFLICT DO UPDATE`) :
si un individu est rejoue (meme `NUM_REFERENCE`), son statut est mis a jour
sans creer de doublon.

## Cas d'erreur typiques

| Erreur | Cause | Type |
|---|---|---|
| Identifiant absent | Ligne incomplete | Fonctionnelle |
| Identifiant invalide (pas 10 chars) | Donnees corrompues | Fonctionnelle |
| Identifiant commence par autre que 1/2 | Donnees incoherentes | Fonctionnelle |
| Nom absent | Ligne mal formee | Fonctionnelle |
| Fichier introuvable | Probleme infrastructure | Technique |
| Violation contrainte BDD | Erreur SQL non geree | Technique |

## Encoding ISO-8859-1

Les fichiers legacy sont souvent encodes en **ISO-8859-1** (Latin-1)
et non en UTF-8. Le `FlatFileItemReader` est configure explicitement :

```java
delegate = new FlatFileItemReaderBuilder<AssureDto>()
    .encoding("ISO-8859-1")
    .build();
```

Sans cette configuration, les caracteres accentues seraient corrompus a la lecture.

## Fichier de test

Le fichier `src/test/resources/data/individus_test.txt` contient 200 individus
generes avec des donnees fictives :
- 192 individus valides (identifiant correct, commence par 5 ou 6)
- 8 individus sans identifiant (1 ligne sur 25) -> erreurs fonctionnelles

Verifie par les tests d'integration `TraitementIndividusJobIT` :
- Statut du job : `COMPLETED`
- Synthese : `nbOK=192`, `nbKO=8`, `nbErreursFonctionnelles=8`
- Base de donnees : 200 lignes dans `T_INDIVIDU`, 192 OK et 8 KO
