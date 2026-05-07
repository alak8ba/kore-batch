# Environnement de developpement — Windows 11 + IntelliJ IDEA + Docker

Guide pas a pas pour faire tourner kore-batch en local sur Windows 11.

---

## Prerequis a installer

### 1. Java 21 (Temurin)

Telecharger Eclipse Temurin 21 :
https://adoptium.net/temurin/releases/?version=21

- Installer avec les options par defaut
- Cocher "Set JAVA_HOME" et "Add to PATH" pendant l'installation

Verifier :
```cmd
java -version
```
Resultat attendu : `openjdk version "21.x.x"`

---

### 2. Maven 3.9+

Telecharger : https://maven.apache.org/download.cgi

Ajouter `MAVEN_HOME` et `%MAVEN_HOME%\bin` au PATH Windows.

Verifier :
```cmd
mvn -version
```

---

### 3. Docker Desktop

Telecharger : https://www.docker.com/products/docker-desktop

- Installer avec WSL2 (recommande)
- Dans Settings > General : cocher **"Expose daemon on tcp://localhost:2375 without TLS"**
- Lancer Docker Desktop avant de travailler

Verifier :
```cmd
docker ps
docker compose version
```

---

### 4. IntelliJ IDEA

Telecharger Community ou Ultimate :
https://www.jetbrains.com/idea/download

---

## Etape 1 — Cloner le projet

```cmd
git clone https://github.com/alak8ba/kore-batch.git
cd kore-batch
```

---

## Etape 2 — Configurer IntelliJ IDEA

1. **File > Open** -> selectionner le dossier `kore-batch`
2. IntelliJ detecte le `pom.xml` -> cliquer **Trust Project**
3. Attendre le chargement des dependances Maven

### Configurer le SDK Java 21

1. **File > Project Structure > Project**
2. SDK -> selectionner Java 21 (ou **Add SDK > Download JDK > Temurin 21**)
3. **Apply > OK**

### Configurer Maven pour GitHub Packages

Creer ou modifier `C:\Users\USER\.m2\settings.xml` :

```xml
<settings>
    <servers>
        <server>
            <id>github</id>
            <username>VOTRE_USERNAME_GITHUB</username>
            <password>VOTRE_TOKEN_GITHUB</password>
        </server>
    </servers>
</settings>
```

Le token GitHub doit avoir les scopes : `read:packages` (et `write:packages` pour publier).

---

## Etape 3 — Demarrer PostgreSQL via Docker

```cmd
docker compose -f docker-compose.dev.yml up -d
```

Verifier que PostgreSQL est pret :
```cmd
docker compose -f docker-compose.dev.yml ps
```
Resultat attendu : `kore-batch-postgres` avec statut `healthy`

---

## Etape 4 — Builder le projet

```cmd
mvn clean install -DskipTests
```

Le socle `kore-batch` est installe dans le repository Maven local.
Le sample `kore-batch-sample` peut maintenant le resoudre sans GitHub Packages.

---

## Etape 5 — Configurer le runner IntelliJ

1. **Run > Edit Configurations > + > Spring Boot**

| Champ | Valeur |
|---|---|
| Name | `kore-batch - Flux Individus` |
| Module | `kore-batch-sample` |
| Main class | `dev.kore.batch.sample.IndividuBatchApplication` |
| Active profiles | `dev` |
| Program arguments | `--inputFile=C:/workspace/datas/X/V/IR/VIRMCEA/fic/VIRMCEAFD01` |
| JDK | `temurin-21` |

2. Cliquer **OK**

---

## Etape 6 — Lancer le batch

### Option A — Via IntelliJ

Cliquer le bouton **Run** (triangle vert).

La console doit afficher :
```
=== DEBUT DU BATCH [traitementIndividusJob] ===
Health [DATABASE] : UP
...
=== FIN DU BATCH - Statut=COMPLETED - Duree=Xs ===
SYNTHESE : total=200, OK=192, KO=8, errTech=0, errFonc=8
```

### Option B — Via terminal

```cmd
mvn spring-boot:run -pl kore-batch-sample ^
  -Dspring-boot.run.profiles=dev ^
  -Dspring-boot.run.arguments="--inputFile=C:/workspace/datas/fichier.txt"
```

---

## Etape 7 — Verifier les donnees en base

Connecter DBeaver a PostgreSQL :

| Parametre | Valeur |
|---|---|
| Host | `localhost` |
| Port | `5432` |
| Database | `batchdb` |
| User | `batch_user` |
| Password | `batch_pass` |

Verifier le resultat :
```sql
SELECT STATUT, COUNT(*) FROM T_INDIVIDU GROUP BY STATUT;
-- Attendu : OK=192, KO=8

SELECT * FROM T_INDIVIDU WHERE STATUT = 'KO';
-- Les 8 individus sans identifiant avec leur message d erreur
```

---

## Reset complet de la base

Depuis DBeaver, executer `deploy/scripts/reset-local.sql` :

```sql
SET session_replication_role = replica;
TRUNCATE TABLE T_INDIVIDU RESTART IDENTITY CASCADE;
TRUNCATE TABLE BATCH_STEP_EXECUTION_CONTEXT  RESTART IDENTITY CASCADE;
TRUNCATE TABLE BATCH_JOB_EXECUTION_CONTEXT   RESTART IDENTITY CASCADE;
TRUNCATE TABLE BATCH_STEP_EXECUTION          RESTART IDENTITY CASCADE;
TRUNCATE TABLE BATCH_JOB_EXECUTION_PARAMS    RESTART IDENTITY CASCADE;
TRUNCATE TABLE BATCH_JOB_EXECUTION           RESTART IDENTITY CASCADE;
TRUNCATE TABLE BATCH_JOB_INSTANCE            RESTART IDENTITY CASCADE;
ALTER SEQUENCE BATCH_STEP_EXECUTION_SEQ RESTART WITH 1;
ALTER SEQUENCE BATCH_JOB_EXECUTION_SEQ  RESTART WITH 1;
ALTER SEQUENCE BATCH_JOB_SEQ            RESTART WITH 1;
TRUNCATE TABLE DATABASECHANGELOGLOCK;
DELETE FROM DATABASECHANGELOG;
SET session_replication_role = DEFAULT;
```

Liquibase recrée les tables au prochain demarrage.

---

## Commandes utiles

```cmd
# Demarrer PostgreSQL
docker compose -f docker-compose.dev.yml up -d

# Arreter PostgreSQL
docker compose -f docker-compose.dev.yml down

# Reset complet avec suppression du volume (repart de zero)
docker compose -f docker-compose.dev.yml down -v

# Voir les logs PostgreSQL
docker compose -f docker-compose.dev.yml logs -f

# Acceder a psql directement
docker exec -it kore-batch-postgres psql -U batch_user -d batchdb
```

---

## Resolution des problemes courants

### Maven ne trouve pas kore-batch (401 Unauthorized)

Verifier `~/.m2/settings.xml` — le serveur `github` doit etre configure avec un token valide.
Ou builder depuis la racine : `mvn clean install` installe le socle localement.

### Port 5432 deja utilise

```cmd
netstat -ano | findstr :5432
```
Verifier qu'un autre PostgreSQL ne tourne pas (kore-hexagonal par exemple).
Changer le port dans `docker-compose.dev.yml` si necessaire.

### IntelliJ ne trouve pas Java 21

**File > Project Structure > SDKs > + > Download JDK**
Selectionner : Vendor `Eclipse Temurin`, Version `21`

### Batch demarre mais T_INDIVIDU est vide

Le writer loggue uniquement si le profil n'est pas `dev`.
Verifier que `SPRING_PROFILES_ACTIVE=dev` est bien actif et que Liquibase a cree la table.
