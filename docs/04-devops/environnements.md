# Environnements

## Local (dev)

Stack Docker avec PostgreSQL uniquement. L'application tourne directement sur le poste du développeur.

```bash
# Démarrer PostgreSQL
docker compose -f docker-compose.dev.yml up -d

# Lancer le batch
mvn spring-boot:run -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.arguments="--inputFile=/data/commandes.csv"
```

**Profil** : `application-dev.yml`
- `spring.datasource.url` : `jdbc:postgresql://localhost:5432/batchdb`
- Logs en DEBUG
- Liquibase activé

## Production (Debian)

Serveur Debian sur réseau local. Le batch tourne comme service systemd.

### Premier déploiement

```bash
# Sur le serveur (une seule fois)
sudo ./deploy/setup-debian.sh
```

Ce script :
1. Installe Java 21 (Temurin)
2. Installe et configure PostgreSQL
3. Crée l'utilisateur système `kore-batch`
4. Installe le service systemd

### Déploiements suivants

```bash
# Depuis le poste dev, après mvn package
./deploy/deploy.sh 1.0.0
```

**Profil** : `application-prod.yml`
- Credentials via variables d'environnement (`DB_URL`, `DB_USER`, `DB_PASSWORD`)
- Logs en INFO, rotation automatique (100MB, 30 jours)
- Pool de connexions HikariCP configuré

### Variables d'environnement requises en prod

| Variable | Description |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/batchdb` |
| `DB_USER` | Utilisateur PostgreSQL |
| `DB_PASSWORD` | Mot de passe PostgreSQL |

Définies dans `/etc/systemd/system/kore-batch-sample.service`.

## CI/CD (GitHub Actions)

Déclenché à chaque push sur `main` :

```
push main
    │
    ▼
publish-core
    │  mvn deploy -pl kore-batch
    │  → publie kore-batch sur GitHub Packages
    │
    ▼
build-and-test
    │  mvn test -pl kore-batch-sample
    │  mvn failsafe:integration-test
    │  → Testcontainers (PostgreSQL réel)
```

Les credentials GitHub Packages sont injectés automatiquement via `${{ secrets.GITHUB_TOKEN }}`.
