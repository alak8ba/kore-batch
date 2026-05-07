# Deploiement en production — Debian Server

Guide pas a pas pour deployer kore-batch sur un serveur Debian accessible via SSH.

---

## Deploiement en trois commandes

Apres le setup initial (une seule fois), deployer une nouvelle version se fait en 3 commandes :

```bash
# 1. Sur le poste dev - creer et pusher le tag
git tag v1.0.1
git push origin v1.0.1
# La CI publie automatiquement sur GitHub Packages

# 2. Sur le serveur - deployer
GITHUB_TOKEN=ghp_xxx ./deploy/deploy.sh 1.0.1

# 3. Verifier
journalctl -u kore-batch-sample -f
```

---

## Etape 1 — Setup initial du serveur (une seule fois)

### Connexion SSH

```bash
ssh user@IP_DU_SERVEUR
```

### Copier les scripts sur le serveur

Depuis le poste dev :
```bash
scp deploy/setup-debian.sh deploy/kore-batch-sample.service user@IP_DU_SERVEUR:~/
```

### Executer le setup

```bash
ssh user@IP_DU_SERVEUR
sudo DB_PASSWORD=mon_mot_de_passe_secret ./setup-debian.sh
```

Le script installe automatiquement :
1. **Java 21** (Temurin via adoptium.net)
2. **PostgreSQL** + creation de la base `batchdb` et de l'utilisateur `batch_user`
3. **Utilisateur systeme** `kore-batch` (sans shell, securise)
4. **Fichier secrets** `/etc/kore-batch/env` (chmod 600, inaccessible aux autres)
5. **Service systemd** active au demarrage

---

## Etape 2 — Creer un token GitHub

Le serveur doit pouvoir telecharger le JAR depuis GitHub Packages.

1. GitHub > Settings > Developer settings > Personal access tokens
2. **Generate new token (classic)**
3. Note : `kore-batch-deploy`
4. Cocher : `read:packages`
5. Copier le token (ne sera plus visible apres)

---

## Etape 3 — Configurer les secrets prod

Le fichier `/etc/kore-batch/env` cree par `setup-debian.sh` contient :

```properties
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://localhost:5432/batchdb
DB_USER=batch_user
DB_PASSWORD=mon_mot_de_passe_secret
INPUT_FILE=/data/input/fichier.txt
```

Pour modifier :
```bash
sudo nano /etc/kore-batch/env
sudo systemctl restart kore-batch-sample
```

Ce fichier est :
- Propriete de `kore-batch:kore-batch`
- `chmod 600` : lecture uniquement par le proprietaire
- Jamais commite en Git

---

## Etape 4 — Deployer une version

```bash
GITHUB_TOKEN=ghp_votre_token ./deploy/deploy.sh 1.0.0
```

Le script :
1. Telecharge le JAR `kore-batch-sample-1.0.0.jar` depuis GitHub Packages
2. Le place dans `/opt/kore-batch/`
3. Cree le lien symbolique `kore-batch-sample.jar`
4. Redémarre le service systemd
5. Affiche le statut

---

## Creer une release (depuis le poste dev)

```bash
# S'assurer que develop est merge dans main
git checkout main
git merge develop
git push origin main

# Creer et pusher le tag
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions (`release.yml`) se declenche et :
- Met a jour la version Maven a `1.0.0`
- Publie `kore-batch:1.0.0` sur GitHub Packages
- Cree une GitHub Release avec les notes d'installation

---

## Option : PostgreSQL via Docker sur le serveur

Si PostgreSQL est prefere en container sur le Debian :

```bash
# Creer le fichier d'environnement Docker
sudo cat > /etc/kore-batch/.env.docker <<EOF
DB_NAME=batchdb
DB_USER=batch_user
DB_PASSWORD=mon_mot_de_passe_secret
EOF
sudo chmod 600 /etc/kore-batch/.env.docker

# Demarrer PostgreSQL
docker compose -f docker-compose.prod.yml \
  --env-file /etc/kore-batch/.env.docker up -d

# Activer au demarrage
docker compose -f docker-compose.prod.yml \
  --env-file /etc/kore-batch/.env.docker up -d --restart always
```

Le `docker-compose.prod.yml` expose PostgreSQL uniquement sur `127.0.0.1:5432`
(pas accessible depuis le reseau externe).

---

## Architecture sur le serveur

```
/opt/kore-batch/
├── kore-batch-sample.jar          -> kore-batch-sample-1.0.0.jar (lien symbolique)
└── kore-batch-sample-1.0.0.jar   (JAR telecharge depuis GitHub Packages)

/etc/kore-batch/
└── env                            (secrets - chmod 600)

/var/log/kore-batch/
└── kore-batch-sample.log          (rotation automatique 100MB / 30 jours)

/etc/systemd/system/
└── kore-batch-sample.service      (lance le batch, charge /etc/kore-batch/env)
```

---

## Commandes utiles sur le serveur

```bash
# Statut du service
sudo systemctl status kore-batch-sample

# Logs en temps reel
journalctl -u kore-batch-sample -f

# Derniers 100 lignes
journalctl -u kore-batch-sample -n 100

# Logs depuis hier
journalctl -u kore-batch-sample --since yesterday

# Redemarrer le service
sudo systemctl restart kore-batch-sample

# Lancer le batch manuellement (hors service)
sudo -u kore-batch java -jar /opt/kore-batch/kore-batch-sample.jar \
  --spring.profiles.active=prod \
  --inputFile=/data/input/fichier.txt

# Voir les donnees en base
sudo -u postgres psql -d batchdb -c "SELECT STATUT, COUNT(*) FROM T_INDIVIDU GROUP BY STATUT;"
```

---

## Rollback vers une version precedente

```bash
# Lister les versions disponibles
ls -la /opt/kore-batch/*.jar

# Pointer sur une version anterieure
sudo ln -sf /opt/kore-batch/kore-batch-sample-1.0.0.jar \
            /opt/kore-batch/kore-batch-sample.jar

# Redemarrer
sudo systemctl restart kore-batch-sample
```

---

## Sauvegarde de la base de donnees

```bash
# Sauvegarde
sudo -u postgres pg_dump batchdb > backup_$(date +%Y%m%d_%H%M%S).sql

# Restauration
sudo -u postgres psql batchdb < backup_YYYYMMDD_HHMMSS.sql
```

---

## Resolution des problemes courants

### Le batch ne demarre pas

```bash
# Voir les logs de demarrage
journalctl -u kore-batch-sample -n 50

# Verifier les secrets
sudo cat /etc/kore-batch/env

# Verifier PostgreSQL
sudo systemctl status postgresql
```

### Health check KO au demarrage

Le `BatchHealthAggregator` verifie la BDD avant de lancer le job.
Si PostgreSQL n'est pas pret : attendre ou verifier la connexion.

```bash
sudo -u postgres psql -d batchdb -c "SELECT 1;"
```

### Espace disque insuffisant

```bash
# Verifier l'espace
df -h

# Nettoyer les anciens JARs
sudo find /opt/kore-batch -name "*.jar" ! -name "kore-batch-sample.jar" -delete

# Nettoyer les anciens logs
sudo journalctl --vacuum-time=7d
```
