#!/bin/bash
# Script de déploiement sur serveur Debian
# Usage : ./deploy.sh <version>
# Exemple : ./deploy.sh 1.0.0

set -e

VERSION=${1:-"1.0.0"}
APP_NAME="kore-batch-sample"
JAR_NAME="${APP_NAME}-${VERSION}.jar"
DEPLOY_DIR="/opt/kore-batch"
LOG_DIR="/var/log/kore-batch"
SERVICE_NAME="kore-batch-sample"

echo "=== Déploiement ${APP_NAME} v${VERSION} ==="

# Copier le JAR
echo "[1/4] Copie du JAR..."
sudo mkdir -p ${DEPLOY_DIR}
sudo cp "kore-batch-sample/target/${JAR_NAME}" "${DEPLOY_DIR}/${JAR_NAME}"
sudo ln -sf "${DEPLOY_DIR}/${JAR_NAME}" "${DEPLOY_DIR}/${APP_NAME}.jar"

# Créer le répertoire de logs
echo "[2/4] Préparation des logs..."
sudo mkdir -p ${LOG_DIR}
sudo chown kore-batch:kore-batch ${LOG_DIR}

# Recharger et redémarrer le service
echo "[3/4] Redémarrage du service..."
sudo systemctl daemon-reload
sudo systemctl restart ${SERVICE_NAME}

# Vérification
echo "[4/4] Vérification..."
sleep 3
sudo systemctl status ${SERVICE_NAME} --no-pager

echo "=== Déploiement terminé ==="
