#!/bin/bash
# Script de deploiement sur serveur Debian
# Telecharge le JAR depuis GitHub Packages et demarre le service
#
# Usage : ./deploy.sh <version> [github_token]
# Exemple : ./deploy.sh 1.0.0 ghp_xxx
#
# Variables d environnement :
#   GITHUB_TOKEN : token GitHub avec read:packages (obligatoire si non passe en arg)
#   GITHUB_OWNER : proprietaire du repo (defaut: alak8ba)
#   GITHUB_REPO  : nom du repo (defaut: kore-batch)

set -e

VERSION=${1:-"1.0.0"}
GITHUB_TOKEN=${2:-$GITHUB_TOKEN}
GITHUB_OWNER=${GITHUB_OWNER:-"alak8ba"}
GITHUB_REPO=${GITHUB_REPO:-"kore-batch"}
APP_NAME="kore-batch-sample"
JAR_NAME="${APP_NAME}-${VERSION}.jar"
DEPLOY_DIR="/opt/kore-batch"
LOG_DIR="/var/log/kore-batch"
SERVICE_NAME="kore-batch-sample"

if [ -z "$GITHUB_TOKEN" ]; then
    echo "ERREUR : GITHUB_TOKEN requis pour telecharger depuis GitHub Packages"
    echo "Usage : ./deploy.sh 1.0.0 ghp_votre_token"
    echo "Ou    : export GITHUB_TOKEN=ghp_xxx && ./deploy.sh 1.0.0"
    exit 1
fi

echo "=== Deploiement ${APP_NAME} v${VERSION} ==="

# Telecharger le JAR depuis GitHub Packages
echo "[1/4] Telechargement du JAR v${VERSION} depuis GitHub Packages..."
sudo mkdir -p ${DEPLOY_DIR}

PACKAGE_URL="https://maven.pkg.github.com/${GITHUB_OWNER}/${GITHUB_REPO}/dev/kore/batch/${APP_NAME}/${VERSION}/${JAR_NAME}"

sudo curl -L \
    -H "Authorization: Bearer ${GITHUB_TOKEN}" \
    -H "Accept: application/vnd.github.v3+json" \
    -o "${DEPLOY_DIR}/${JAR_NAME}" \
    "${PACKAGE_URL}"

sudo ln -sf "${DEPLOY_DIR}/${JAR_NAME}" "${DEPLOY_DIR}/${APP_NAME}.jar"
echo "JAR telecharge : ${DEPLOY_DIR}/${JAR_NAME}"

# Repertoire de logs
echo "[2/4] Preparation des logs..."
sudo mkdir -p ${LOG_DIR}
sudo chown kore-batch:kore-batch ${LOG_DIR}

# Redemarrage du service
echo "[3/4] Redemarrage du service..."
sudo systemctl daemon-reload
sudo systemctl restart ${SERVICE_NAME}

# Verification
echo "[4/4] Verification..."
sleep 3
sudo systemctl status ${SERVICE_NAME} --no-pager

echo ""
echo "=== Deploiement ${VERSION} termine ==="
echo "Logs : journalctl -u ${SERVICE_NAME} -f"
