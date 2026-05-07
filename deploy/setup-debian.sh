#!/bin/bash
# Prerequis a executer une seule fois sur le serveur Debian
# Usage : sudo ./setup-debian.sh
#
# Variables d environnement requises :
#   DB_PASSWORD  : mot de passe PostgreSQL (obligatoire)
#   DB_USER      : utilisateur PostgreSQL (defaut: batch_user)
#   DB_NAME      : nom de la base (defaut: batchdb)
#
# Exemple :
#   sudo DB_PASSWORD=mon_mdp_secret ./setup-debian.sh

set -e

DB_USER=${DB_USER:-"batch_user"}
DB_NAME=${DB_NAME:-"batchdb"}
DB_PASSWORD=${DB_PASSWORD:-""}

if [ -z "$DB_PASSWORD" ]; then
    echo "ERREUR : DB_PASSWORD est obligatoire"
    echo "Usage : sudo DB_PASSWORD=mon_mdp_secret ./setup-debian.sh"
    exit 1
fi

echo "=== Installation des prerequis sur Debian ==="

# Java 21
echo "[1/5] Installation Java 21..."
apt-get update -q
apt-get install -y wget apt-transport-https gnupg
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /etc/apt/trusted.gpg.d/adoptium.gpg
echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list
apt-get update -q
apt-get install -y temurin-21-jdk

# PostgreSQL
echo "[2/5] Installation PostgreSQL..."
apt-get install -y postgresql postgresql-contrib

# Creation de la base et de l utilisateur
echo "[3/5] Creation base de donnees..."
sudo -u postgres psql <<SQL
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = '${DB_USER}') THEN
        CREATE USER ${DB_USER} WITH PASSWORD '${DB_PASSWORD}';
    END IF;
END
\$\$;
CREATE DATABASE IF NOT EXISTS ${DB_NAME} OWNER ${DB_USER};
GRANT ALL PRIVILEGES ON DATABASE ${DB_NAME} TO ${DB_USER};
SQL

# Utilisateur systeme et repertoires
echo "[4/5] Creation utilisateur systeme kore-batch..."
useradd -r -s /bin/false kore-batch || true
mkdir -p /opt/kore-batch /var/log/kore-batch /etc/kore-batch
chown kore-batch:kore-batch /opt/kore-batch /var/log/kore-batch
chmod 750 /etc/kore-batch

# Fichier de configuration secrets
echo "[5/5] Configuration des secrets..."
cat > /etc/kore-batch/env <<EOF
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://localhost:5432/${DB_NAME}
DB_USER=${DB_USER}
DB_PASSWORD=${DB_PASSWORD}
INPUT_FILE=/data/input/fichier.txt
EOF
chmod 600 /etc/kore-batch/env
chown kore-batch:kore-batch /etc/kore-batch/env

# Service systemd
cp kore-batch-sample.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable kore-batch-sample

echo ""
echo "=== Setup termine ==="
echo "Prochaine etape : ./deploy.sh 1.0.0"
