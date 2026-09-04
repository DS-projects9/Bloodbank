#!/usr/bin/env bash
# Deploy the MedKeen backend to Cloud Run.
#
# Usage:
#   ./deploy.sh [REGION] [PROJECT]
#
# Requires:
#   - gcloud CLI authenticated (gcloud auth login)
#   - Artifact Registry repo "medkeen-backend" in the chosen region
#   - Secret Manager secrets: medkeen-openai-key, medkeen-cron-secret
#   - Service account medkeen-backend@<project>.iam.gserviceaccount.com
#     with roles: roles/datastore.user, roles/storage.objectAdmin
#
# This script builds the image locally with Docker, pushes to Artifact
# Registry, and deploys a new Cloud Run revision.

set -euo pipefail

REGION="${1:-asia-south1}"
PROJECT="${2:-medkeen-11c68}"
REPO="medkeen-backend"
SERVICE="medkeen-backend"
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
  --set-secrets="OPENAI_API_KEY=medkeen-openai-key:latest,CRON_SECRET=medkeen-cron-secret:latest" \
  --service-account="medkeen-backend@${PROJECT}.iam.gserviceaccount.com" \
  --cpu=1 \
  --memory=512Mi \
  --min-instances=0 \
  --max-instances=10 \
  --concurrency=80 \
  --timeout=300

echo ">> Done. Service URL:"
gcloud run services describe "${SERVICE}" --region="${REGION}" --format='value(status.url)'