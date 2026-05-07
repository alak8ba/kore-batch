# Lancer les tests — Windows 11 + Docker Desktop

## Prerequis

- Java 21 installe (`java -version`)
- Docker Desktop lance et en etat **Running** (icone dans la barre des taches)
- Port 2375 expose : Docker Desktop > Settings > General > "Expose daemon on tcp://localhost:2375 without TLS"
- Projet clone et builde (`mvn clean install -DskipTests`)

---

## Structure des tests

```
src/test/java/dev/kore/batch/sample/
├── AbstractIntegrationTest.java    <- base Testcontainers partagee
├── TraitementIndividusJobIT.java   <- tests integration (Docker requis)
└── (tests unitaires a venir)      <- sans Docker
```

| Fichier | Type | Docker requis |
|---|---|---|
| `TraitementIndividusJobIT` | Integration | Oui (PostgreSQL via Testcontainers) |

---

## Lancer les tests d'integration

### Verifier que Docker est accessible

```cmd
docker ps
docker info | findstr "Server Version"
```
Resultat attendu : liste des containers + version du daemon.

### Via IntelliJ

1. Clic droit sur `TraitementIndividusJobIT` -> **Run**
2. La console affiche :

```
INFO  o.t.d.DockerClientProviderStrategy - Found Docker environment...
INFO  tc.postgres:16-alpine - Starting...
INFO  tc.postgres:16-alpine - Container is started
...
SYNTHESE : total=200, OK=192, KO=8
Tests run: 3, Failures: 0, Errors: 0
```

### Via terminal

```cmd
# Tests d'integration uniquement
mvn -pl kore-batch-sample failsafe:integration-test failsafe:verify

# Tous les tests
mvn -pl kore-batch-sample verify
```

---

## Ce que verifient les tests IT

| Test | Assertion |
|---|---|
| `jobDoitSeTerminerEnSucces` | `BatchStatus.COMPLETED` |
| `syntheseDoitComptabiliserLesErreursFonctionnelles` | `nbOK=192, nbKO=8, nbErreursFonctionnelles=8` |
| `tousLesIndividusDoiventEtrePersisteesEnBase` | 200 lignes en base, 192 OK + 8 KO avec message |

---

## Problemes courants sur Windows 11

### Erreur : `Could not find a valid Docker environment`

**Cause :** Docker Desktop n'est pas lance ou le daemon n'est pas accessible.

**Fix :**
1. Ouvrir Docker Desktop
2. Attendre l'etat "Running" (icone verte)
3. Verifier : `docker ps` ne retourne pas d'erreur
4. Relancer les tests

### Erreur : `ServiceConfigurationError TestcontainersHostPropertyClientProviderStrategy`

**Cause :** Fichier `~/.testcontainers.properties` contient `docker.host=` ce qui declenche une strategie incompatible.

**Fix :**
Supprimer ou vider le fichier `C:\Users\USER\.testcontainers.properties`.
La configuration est geree directement dans `AbstractIntegrationTest` via le bloc `static`.

### Erreur : `Illegal character in authority`

**Cause :** Les variables d'environnement dans IntelliJ sont concatenees incorrectement.

**Fix :**
Dans Run Configuration > Environment variables, mettre **uniquement** :
```
DOCKER_HOST=tcp://localhost:2375
```
Sans autre variable sur la meme ligne.

### Testcontainers ne trouve pas Docker malgre Docker actif

**Cause :** Docker Desktop utilise WSL2 avec un socket npipe non reconnu.

**Fix :** Le bloc `static` dans `AbstractIntegrationTest` force la configuration :

```java
static {
    System.setProperty("TESTCONTAINERS_RYUK_DISABLED", "true");
    System.setProperty("DOCKER_HOST", "tcp://localhost:2375");
}
```

S'assurer que le port 2375 est bien accessible :
```cmd
Test-NetConnection -ComputerName localhost -Port 2375
# TcpTestSucceeded : True
```

### Containers qui restent apres les tests

Ryuk est desactive (`TESTCONTAINERS_RYUK_DISABLED=true`) pour eviter les problemes Windows.
Nettoyer manuellement si besoin :
```cmd
docker container prune -f
```

---

## Lancer les tests en CI (GitHub Actions)

Les tests IT passent automatiquement en CI car GitHub Actions utilise Ubuntu
ou Docker est natif — pas de probleme de socket Windows.

```yaml
# Dans ci.yml - le service PostgreSQL est fourni directement
services:
  postgres:
    image: postgres:16-alpine
    env:
      POSTGRES_DB: batchdb_test
      POSTGRES_USER: batch_user
      POSTGRES_PASSWORD: batch_pass
```

Testcontainers detecte l'environnement CI automatiquement et utilise
le service PostgreSQL du runner plutot que de lancer un nouveau container.

---

## Resume des commandes

| Action | Commande |
|---|---|
| Tests integration uniquement | `mvn -pl kore-batch-sample failsafe:integration-test` |
| Tous les tests | `mvn -pl kore-batch-sample verify` |
| Builder + tous les tests | `mvn clean verify` |
