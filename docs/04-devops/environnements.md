# Environnements

## Vue d'ensemble

```
+------------------+          +--------------------+          +------------------+
|   DEV (local)    |          |   CI (GitHub)      |          |   PROD (Debian)  |
|                  |          |                    |          |                  |
| Docker Compose   |          | Ubuntu runner      |          | Systemd service  |
| PostgreSQL       |          | Testcontainers     |          | PostgreSQL       |
| Profile : dev    |          | Profile : test     |          | Profile : prod   |
+------------------+          +--------------------+          +------------------+
```

---

## Local (dev)

### Prerequis

- Java 21 (Temurin)
- Docker Desktop
- Maven 3.9+

### Demarrage

```bash
# 1. Cloner le projet
git clone https://github.com/alak8ba/kore-batch.git
cd kore-batch

# 2. Demarrer PostgreSQL via Docker
docker compose -f docker-compose.dev.yml up -d

# 3. Builder
mvn clean install -DskipTests

# 4. Lancer le batch
mvn spring-boot:run -pl kore-batch-sample \
  -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.arguments="--inputFile=/chemin/vers/fichier"
```

### Profil dev (application-dev.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/batchdb
    username: batch_user
    password: batch_pass

batch:
  partitioning:
    grid-size: 1  # SynchronizedItemStreamReader - 1 fichier partage

logging:
  level:
    dev.kore.batch: DEBUG
```

### Reset de la base locale

Depuis DBeaver ou psql, executer `deploy/scripts/reset-local.sql` :

```sql
-- Remet a zero les tables Spring Batch ET metier
-- A utiliser apres un reset Docker ou changement de schema
SET session_replication_role = replica;
TRUNCATE TABLE T_INDIVIDU RESTART IDENTITY CASCADE;
TRUNCATE TABLE BATCH_STEP_EXECUTION_CONTEXT  RESTART IDENTITY CASCADE;
-- ... (voir le fichier complet)
DELETE FROM DATABASECHANGELOG;
SET session_replication_role = DEFAULT;
```

### IntelliJ - Configuration runner

```
Main class       : dev.kore.batch.sample.IndividuBatchApplication
Module           : kore-batch-sample
Active profiles  : dev
Program arguments: --inputFile=C:/workspace/datas/X/V/IR/VIRMCEA/fic/VIRMCEAFD01
JDK              : temurin-21
```

---

## CI/CD (GitHub Actions)

Deux workflows independants :

### ci.yml - Tests (push main/develop + PR)

```
push main ou develop
        |
        v
build-and-test
  ├── mvn clean install -DskipTests
  ├── mvn test (tests unitaires)
  └── mvn failsafe:integration-test (Testcontainers - vrai PostgreSQL)
```

Le service PostgreSQL est fourni nativement par GitHub Actions (pas de Docker Desktop requis).

### release.yml - Publication (tag v*)

```
git tag v1.0.0 + push
        |
        v
publish
  ├── Extraction version depuis le tag (v1.0.0 -> 1.0.0)
  ├── mvn versions:set -DnewVersion=1.0.0
  ├── mvn deploy -pl kore-batch (-> GitHub Packages)
  └── GitHub Release automatique avec notes d installation
```

### Creer une release

```bash
# Depuis GitKraken :
# 1. Merger develop -> main
# 2. Create tag -> v1.0.0
# 3. Push tag

# Ou en ligne de commande :
git tag v1.0.0
git push origin v1.0.0
```

---

## Production (Debian sur reseau local)

### Architecture

```
Debian Server
├── /opt/kore-batch/
│   ├── kore-batch-sample.jar  -> kore-batch-sample-1.0.0.jar
│   └── kore-batch-sample-1.0.0.jar
├── /etc/kore-batch/
│   └── env                    (secrets - chmod 600)
├── /var/log/kore-batch/       (logs applicatifs)
└── /etc/systemd/system/
    └── kore-batch-sample.service
```

### Premier deploiement (setup-debian.sh)

A executer une seule fois sur le serveur :

```bash
# Copier les scripts sur le serveur
scp deploy/setup-debian.sh deploy/kore-batch-sample.service user@serveur:~/

# Executer sur le serveur
ssh user@serveur
sudo DB_PASSWORD=mon_mot_de_passe_secret ./setup-debian.sh
```

Le script installe :
1. Java 21 (Temurin via adoptium.net)
2. PostgreSQL + creation de la base et de l utilisateur
3. Utilisateur systeme `kore-batch` (sans shell)
4. Fichier de secrets `/etc/kore-batch/env` (chmod 600)
5. Service systemd active au demarrage

### Fichier de secrets (/etc/kore-batch/env)

Cree automatiquement par `setup-debian.sh`. Format :

```properties
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://localhost:5432/batchdb
DB_USER=batch_user
DB_PASSWORD=mon_mot_de_passe_secret
INPUT_FILE=/data/input/fichier.txt
```

Ce fichier est :
- Propriete de `kore-batch:kore-batch`
- chmod 600 (lecture uniquement par le proprietaire)
- Jamais commite en Git (protege par .gitignore)

### Deploiement (deploy.sh)

```bash
# Sur le poste dev ou directement sur le serveur
GITHUB_TOKEN=ghp_votre_token ./deploy/deploy.sh 1.0.0
```

Le script :
1. Telecharge le JAR depuis GitHub Packages
2. Cree le lien symbolique `kore-batch-sample.jar`
3. Redémarre le service systemd
4. Verifie le statut

### Profil prod (application-prod.yml)

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10

batch:
  partitioning:
    grid-size: 4
    pool-size: 8

logging:
  level:
    dev.kore.batch: INFO
  file:
    name: /var/log/kore-batch/kore-batch-sample.log
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30
```

### Commandes utiles sur le serveur

```bash
# Statut du service
sudo systemctl status kore-batch-sample

# Logs en temps reel
journalctl -u kore-batch-sample -f

# Logs des derniers 100 lignes
journalctl -u kore-batch-sample -n 100

# Redemarrer le service
sudo systemctl restart kore-batch-sample

# Arreter le service
sudo systemctl stop kore-batch-sample

# Lancer le batch manuellement (hors service)
sudo -u kore-batch java -jar /opt/kore-batch/kore-batch-sample.jar \
  --inputFile=/data/input/fichier.txt
```

### PostgreSQL prod via Docker (alternatif)

Si PostgreSQL est gere via Docker sur le serveur Debian :

```bash
# Creer le fichier .env sur le serveur
cat > /etc/kore-batch/.env.docker <<EOF
DB_NAME=batchdb
DB_USER=batch_user
DB_PASSWORD=mon_mot_de_passe_secret
EOF

# Demarrer PostgreSQL
docker compose -f docker-compose.prod.yml --env-file /etc/kore-batch/.env.docker up -d

# Verifier
docker ps | grep kore-batch-postgres-prod
```

Le `docker-compose.prod.yml` configure PostgreSQL avec :
- Port expose uniquement sur `127.0.0.1` (pas accessible depuis le reseau)
- Volume persistant nomme
- Healthcheck toutes les 30 secondes
- Rotation des logs Docker

---

## Separation des configurations

| Element | DEV | CI | PROD |
|---|---|---|---|
| Profil Spring | `dev` | `test` | `prod` |
| PostgreSQL | Docker local (5432) | Testcontainers (port aleatoire) | Systemd/Docker (127.0.0.1:5432) |
| Secrets | `application-dev.yml` | Testcontainers auto | `/etc/kore-batch/env` (chmod 600) |
| Logs | Console DEBUG | Console DEBUG | Fichier + rotation |
| grid-size | 1 | 1 | 4 |
| Liquibase | Auto au demarrage | Auto au demarrage | Auto au demarrage |
| JAR source | `mvn package` local | Build CI | GitHub Packages |
