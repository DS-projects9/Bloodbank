#!/usr/bin/env bash
# Creates a Cloud Scheduler job that periodically triggers the backend's
# /api/v1/cron/run endpoint (auto-expiry of slots, blood requests, vault docs).
#
# Usage:
#   ./setup-scheduler.sh [REGION] [PROJECT]
#
# Requires:
#   - The Cloud Run service deployed (see deploy.sh)
#   - Secret Manager secret: medkeen-cron-secret (the CRON_SECRET value)
#   - A small Pub/Sub topic or the Cloud Run invoker permission for the
#     scheduler service account.

set -euo pipefail

REGION="${1:-asia-south1}"
PROJECT="${2:-medkeen-11c68}"
SERVICE="medkeen-backend"
TOPIC="medkeen-cron"
CRON_SECRET_NAME="medkeen-cron-secret"

SERVICE_URL=$(gcloud run services describe "${SERVICE}" --region="${REGION}" --format='value(status.url)')
CRON_SECRET=$(gcloud secrets versions access latest --secret="${CRON_SECRET_NAME}" --project="${PROJECT}")

echo ">> Service URL: ${SERVICE_URL}"

# Create a Pub/Sub topic for the scheduler to publish to (optional bridge).
gcloud pubsub topics create "${TOPIC}" --project="${PROJECT}" 2>/dev/null || true

# Create (or update) the scheduled job: every minute.
gcloud scheduler jobs create http medkeen-cron-job \
  --location="${REGION}" \
  --schedule="* * * * *" \
  --uri="${SERVICE_URL}/api/v1/cron/run?secret=${CRON_SECRET}" \
  --http-method=POST \
  --oidc-service-account-email="medkeen-backend@${PROJECT}.iam.gserviceaccount.com" \
  --project="${PROJECT}" 2>/dev/null \
  || gcloud scheduler jobs update http medkeen-cron-job \
       --location="${REGION}" \
       --schedule="* * * * *" \
       --uri="${SERVICE_URL}/api/v1/cron/run?secret=${CRON_SECRET}" \
       --http-method=POST \
       --oidc-service-account-email="medkeen-backend@${PROJECT}.iam.gserviceaccount.com" \
       --project="${PROJECT}"

echo ">> Scheduler job 'medkeen-cron-job' configured (every minute)."