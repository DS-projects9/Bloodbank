#!/usr/bin/env bash
# Deploys the MedKeen backend to Google Cloud Run.
#
# Prerequisites (run once):
#   gcloud auth login
#   gcloud config set project $GCP_PROJECT
#   # Cloud SQL (Postgres) instance + database + user
#   # MinIO (or GCS gateway) reachable at a public URL
#   # Secret Manager secret for the JWT signing key:
#   printf '%s' "$(openssl rand -base64 48)" | \
#     gcloud secrets create medkeen-jwt --data-file=- --project $GCP_PROJECT
#
# Then:  GCP_PROJECT=... DB_CONNECTION_NAME=... MINIO_ENDPOINT=... \
#          MINIO_ACCESS_KEY=... MINIO_SECRET_KEY=... ./scripts/deploy-cloudrun.sh
set -euo pipefail

PROJECT_ID="${GCP_PROJECT:?Set GCP_PROJECT}"
REGION="${REGION:-asia-south1}"
SERVICE="${SERVICE:-medkeen-backend}"
DB_NAME="${DB_NAME:-medkeen}"
DB_CONNECTION_NAME="${DB_CONNECTION_NAME:?Set DB_CONNECTION_NAME (project:region:instance)}"
JWT_SECRET_REF="projects/${PROJECT_ID}/secrets/medkeen-jwt/versions/latest"

# 1. Build & push the image.
gcloud builds submit --config cloudbuild.yaml --project "$PROJECT_ID" .

# 2. Deploy to Cloud Run, wiring Cloud SQL + env + secrets.
gcloud run deploy "$SERVICE" \
  --project "$PROJECT_ID" \
  --region "$REGION" \
  --image "gcr.io/${PROJECT_ID}/medkeen-backend" \
  --platform managed \
  --allow-unauthenticated \
  --add-cloudsql-instances "$DB_CONNECTION_NAME" \
  --set-env-vars "JDBC_URL=jdbc:postgresql:///${DB_NAME}?host=/cloudsql/${DB_CONNECTION_NAME}" \
  --set-env-vars "JWT_ISSUER=medkeen,JWT_AUDIENCE=medkeen-app,JWT_REALM=medkeen,SEED_DEMO_DATA=false" \
  --set-env-vars "MINIO_ENDPOINT=${MINIO_ENDPOINT},MINIO_ACCESS_KEY=${MINIO_ACCESS_KEY},MINIO_SECRET_KEY=${MINIO_SECRET_KEY},MINIO_BUCKET=${MINIO_BUCKET:-medkeen}" \
  --set-secrets "JWT_SECRET=${JWT_SECRET_REF}"

echo
echo "Deployed. Service URL:"
gcloud run services describe "$SERVICE" --project "$PROJECT_ID" --region "$REGION" \
  --format 'value(status.url)'