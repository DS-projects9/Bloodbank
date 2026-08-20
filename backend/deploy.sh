#!/usr/bin/env bash
# Deploy the MedVault backend to Cloud Run.
#
# Usage:
#   ./deploy.sh [REGION] [PROJECT]
#
# Requires:
#   - gcloud CLI authenticated (gcloud auth login)
#   - Artifact Registry repo "medvault-backend" in the chosen region
#   - Secret Manager secrets: medvault-openai-key, medvault-cron-secret
#   - Service account medvault-backend@<project>.iam.gserviceaccount.com
#     with roles: roles/datastore.user, roles/storage.objectAdmin
#
# This script builds the image locally with Docker, pushes to Artifact
# Registry, and deploys a new Cloud Run revision.

set -euo pipefail

REGION="${1:-asia-south1}"
PROJECT="${2:-medvault-11c68}"
REPO="medvault-backend"
SERVICE="medvault-backend"
IMAGE="${REGION}-docker.pkg.dev/${PROJECT}/${REPO}/${SERVICE}:latest"

echo ">> Building image ${IMAGE}"
docker build -t "${IMAGE}" .

echo ">> Pushing image"
docker push "${IMAGE}"

echo ">> Deploying to Cloud Run (${REGION})"
gcloud run deploy "${SERVICE}" \
  --region="${REGION}" \
  --image="${IMAGE}" \
  --platform=managed \
  --allow-unauthenticated \
  --port=8080 \
  --set-env-vars="GOOGLE_CLOUD_PROJECT=${PROJECT}" \
  --set-secrets="OPENAI_API_KEY=medvault-openai-key:latest,CRON_SECRET=medvault-cron-secret:latest" \
  --service-account="medvault-backend@${PROJECT}.iam.gserviceaccount.com" \
  --cpu=1 \
  --memory=512Mi \
  --min-instances=0 \
  --max-instances=10 \
  --concurrency=80 \
  --timeout=300

echo ">> Done. Service URL:"
gcloud run services describe "${SERVICE}" --region="${REGION}" --format='value(status.url)'
