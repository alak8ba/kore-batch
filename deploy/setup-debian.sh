#!/bin/bash
# Prérequis à exécuter une seule fois sur le serveur Debian
# Usage : sudo ./setup-debian.sh

set -e

echo "=== Installation des prérequis sur Debian ==="

# Java 21
echo "[1/4] Installation Java 21..."
apt-get update
apt-get install -y wget apt-transport-https
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /etc/apt/trusted.gpg.d/adoptium.gpg
echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list
apt-get update
apt-get install -y temurin-21-jdk

# PostgreSQL
echo "[2/4] Installation PostgreSQL..."
apt-get install -y postgresql postgresql-contrib

# Création de la base et de l'utilisateur
echo "[3/4] Création base de données..."
sudo -u postgres psql <<SQL
CREATE USER batch_user WITH PASSWORD 'batch_pass';
CREATE DATABASE batchdb OWNER batch_user;
GRANT ALL PRIVILEGES ON DATABASE batchdb TO batch_user;
SQL

# Création de l'utilisateur système et des répertoires
echo "[4/4] Création utilisateur système kore-batch..."
useradd -r -s /bin/false kore-batch || true
mkdir -p /opt/kore-batch /var/log/kore-batch
chown kore-batch:kore-batch /opt/kore-batch /var/log/kore-batch

# Installation du service systemd
cp kore-batch-sample.service /etc/systemd/system/
systemctl daemon-reload
systemctl enable kore-batch-sample

echo "=== Setup terminé - Lance ./deploy.sh pour déployer l'application ==="
